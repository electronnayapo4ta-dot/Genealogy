package com.densappstudio.genealogy.ui

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.densappstudio.genealogy.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Calendar

enum class LivingFilter { ALL, LIVING, DECEASED }
enum class RelationFilter {
    ALL,
    PARENTS,
    CHILDREN,
    ADOPTED_CHILDREN,
    SPOUSES,
    FRIENDS,
    NANNY_WET_NURSE,
    CLOSE_CIRCLE // Friends, Nannies, Wet Nurses
}

enum class AppTheme { LIGHT, DARK, SYSTEM }

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GenealogyViewModel(application: Application) : AndroidViewModel(application) {

    private val database = GenealogyDatabase.getDatabase(application)
    private val repository = GenealogyRepository(database.genealogyDao())

    // Tree State
    private val _activeTreeId = MutableStateFlow<Long?>(null)
    val activeTreeId = _activeTreeId.asStateFlow()

    val allTrees: StateFlow<List<GenealogyTree>> = repository.allTrees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTree: StateFlow<GenealogyTree?> = combine(allTrees, _activeTreeId) { trees, activeId ->
        trees.find { it.id == activeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allPeople: StateFlow<List<Person>> = activeTree
        .flatMapLatest { tree -> 
            if (tree == null) flowOf(emptyList()) 
            else repository.getPeopleForTree(tree.id) 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRelationships: StateFlow<List<Relationship>> = activeTree
        .flatMapLatest { tree -> 
            if (tree == null) flowOf(emptyList()) 
            else repository.getRelationshipsForTree(tree.id) 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected person details state
    private val _selectedPersonId = MutableStateFlow<Long?>(null)
    val selectedPersonId: StateFlow<Long?> = _selectedPersonId.asStateFlow()

    val selectedPerson: StateFlow<Person?> = _selectedPersonId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getPersonById(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Search and filter inputs
    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _livingFilter = MutableStateFlow(LivingFilter.ALL)
    val livingFilter = _livingFilter.asStateFlow()

    private val _relationFilter = MutableStateFlow(RelationFilter.ALL)
    val relationFilter = _relationFilter.asStateFlow()

    private val _focusPersonId = MutableStateFlow<Long?>(null)
    val focusPersonId = _focusPersonId.asStateFlow()

    private val _minAge = MutableStateFlow<Int?>(null)
    val minAge = _minAge.asStateFlow()

    private val _birthYearStart = MutableStateFlow<Int?>(null)
    val birthYearStart = _birthYearStart.asStateFlow()

    private val _birthYearEnd = MutableStateFlow<Int?>(null)
    val birthYearEnd = _birthYearEnd.asStateFlow()

    // Status state for Import/Export
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    private val prefs = application.getSharedPreferences("genealogy_prefs", Context.MODE_PRIVATE)
    private val _appTheme = MutableStateFlow(
        AppTheme.valueOf(prefs.getString("app_theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name)
    )
    val appTheme = _appTheme.asStateFlow()

    private val _publicCollections = MutableStateFlow<List<PublicCollection>>(emptyList())
    val publicCollections = _publicCollections.asStateFlow()

    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    // Export and Import Logic
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val jsonAdapter = moshi.adapter(GenealogyData::class.java)
    private val collectionListAdapter = moshi.adapter<List<PublicCollection>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, PublicCollection::class.java)
    )

    init {
        viewModelScope.launch {
            // Auto-select first tree if available
            repository.allTrees.first().firstOrNull()?.let {
                _activeTreeId.value = it.id
            }
            // Fetch remote library index from GitHub
            fetchPublicCollections()
        }
    }

    private fun fetchPublicCollections() {
        viewModelScope.launch {
            try {
                Log.d("GenealogyVM", "Fetching public collections from GitHub...")
                val indexUrl = "https://raw.githubusercontent.com/electronnayapo4ta-dot/Genealogy/main/collections/index.json"
                val jsonString = withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(indexUrl).build()
                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            Log.e("GenealogyVM", "GitHub request failed: ${response.code}")
                            null
                        } else {
                            response.body?.string()
                        }
                    }
                }
                
                jsonString?.let {
                    Log.d("GenealogyVM", "Index fetched successfully: $it")
                    val collections = collectionListAdapter.fromJson(it)
                    if (collections != null) {
                        _publicCollections.value = collections
                        Log.d("GenealogyVM", "Public collections updated: ${collections.size}")
                    }
                }
            } catch (e: Exception) {
                Log.e("GenealogyVM", "Failed to fetch collections index", e)
            }
        }
    }

    // Filtered People List based on search inputs
    val filteredPeople: StateFlow<List<Person>> = combine(
        allPeople,
        allRelationships,
        _searchText,
        _livingFilter,
        _relationFilter,
        _focusPersonId,
        _minAge,
        _birthYearStart,
        _birthYearEnd
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val people = args[0] as List<Person>
        @Suppress("UNCHECKED_CAST")
        val relationships = args[1] as List<Relationship>
        val text = args[2] as String
        val living = args[3] as LivingFilter
        val relation = args[4] as RelationFilter
        val focusId = args[5] as Long?
        val age = args[6] as Int?
        val bStart = args[7] as Int?
        val bEnd = args[8] as Int?

        var result = people

        // 1. Text Search (Name, Residence, Education, Biography)
        if (text.isNotBlank()) {
            val query = text.trim().lowercase()
            result = result.filter { person ->
                person.fullName.lowercase().contains(query) ||
                        (person.residence?.lowercase()?.contains(query) == true) ||
                        (person.education?.lowercase()?.contains(query) == true) ||
                        (person.biography?.lowercase()?.contains(query) == true)
            }
        }

        // 2. Living / Deceased Filter
        result = when (living) {
            LivingFilter.ALL -> result
            LivingFilter.LIVING -> result.filter { !it.isDeceased }
            LivingFilter.DECEASED -> result.filter { it.isDeceased }
        }

        // 3. Min Age Filter (older than N years)
        if (age != null) {
            result = result.filter { person ->
                val calculatedAge = getAge(person)
                calculatedAge != null && calculatedAge >= age
            }
        }

        // 4. Birth Year Range Filter
        if (bStart != null) {
            result = result.filter { (it.birthYear ?: 0) >= bStart }
        }
        if (bEnd != null) {
            result = result.filter { (it.birthYear ?: Int.MAX_VALUE) <= bEnd }
        }

        // 5. Relationship focus filter
        if (focusId != null && relation != RelationFilter.ALL) {
            val relatedIds = getRelatedIds(focusId, relation, relationships)
            result = result.filter { it.id in relatedIds }
        }

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Helper to calculate age
    fun getAge(person: Person): Int? {
        val birth = person.birthYear ?: return null
        return if (person.isDeceased) {
            val death = person.deathYear ?: return null
            death - birth
        } else {
            currentYear - birth
        }
    }

    // Helper to fetch relationships for selected person directly
    fun getDirectRelationshipsForPerson(personId: Long, relationships: List<Relationship>): List<DirectRelation> {
        val results = mutableListOf<DirectRelation>()
        val peopleMap = allPeople.value.associateBy { it.id }

        for (rel in relationships) {
            if (rel.personId1 == personId) {
                val other = peopleMap[rel.personId2] ?: continue
                val directType = when (rel.type) {
                    "PARENT" -> DirectRelationType.CHILD_BIOLOGICAL
                    "PARENT_ADOPTED" -> DirectRelationType.CHILD_ADOPTED
                    "SPOUSE" -> DirectRelationType.SPOUSE
                    "EX_SPOUSE" -> DirectRelationType.EX_SPOUSE
                    "FRIEND" -> DirectRelationType.FRIEND
                    "NANNY" -> DirectRelationType.CARED_PERSON // personId1 is nanny, personId2 is child
                    "WET_NURSE" -> DirectRelationType.CARED_PERSON
                    else -> DirectRelationType.OTHER
                }
                results.add(DirectRelation(rel.id, other, directType))
            } else if (rel.personId2 == personId) {
                val other = peopleMap[rel.personId1] ?: continue
                val directType = when (rel.type) {
                    "PARENT" -> DirectRelationType.PARENT_BIOLOGICAL
                    "PARENT_ADOPTED" -> DirectRelationType.PARENT_ADOPTED
                    "SPOUSE" -> DirectRelationType.SPOUSE
                    "EX_SPOUSE" -> DirectRelationType.EX_SPOUSE
                    "FRIEND" -> DirectRelationType.FRIEND
                    "NANNY" -> DirectRelationType.NANNY // personId1 is nanny, personId2 is child
                    "WET_NURSE" -> DirectRelationType.WET_NURSE
                    else -> DirectRelationType.OTHER
                }
                results.add(DirectRelation(rel.id, other, directType))
            }
        }
        return results
    }

    private fun getRelatedIds(focusId: Long, filter: RelationFilter, relationships: List<Relationship>): Set<Long> {
        val ids = mutableSetOf<Long>()
        for (rel in relationships) {
            when (filter) {
                RelationFilter.PARENTS -> {
                    if (rel.personId2 == focusId && (rel.type == "PARENT" || rel.type == "PARENT_ADOPTED")) {
                        ids.add(rel.personId1)
                    }
                }
                RelationFilter.CHILDREN -> {
                    if (rel.personId1 == focusId && (rel.type == "PARENT" || rel.type == "PARENT_ADOPTED")) {
                        ids.add(rel.personId2)
                    }
                }
                RelationFilter.ADOPTED_CHILDREN -> {
                    if (rel.personId1 == focusId && rel.type == "PARENT_ADOPTED") {
                        ids.add(rel.personId2)
                    }
                }
                RelationFilter.SPOUSES -> {
                    if (rel.personId1 == focusId && (rel.type == "SPOUSE" || rel.type == "EX_SPOUSE")) ids.add(rel.personId2)
                    if (rel.personId2 == focusId && (rel.type == "SPOUSE" || rel.type == "EX_SPOUSE")) ids.add(rel.personId1)
                }
                RelationFilter.FRIENDS -> {
                    if (rel.personId1 == focusId && rel.type == "FRIEND") ids.add(rel.personId2)
                    if (rel.personId2 == focusId && rel.type == "FRIEND") ids.add(rel.personId1)
                }
                RelationFilter.NANNY_WET_NURSE -> {
                    // Fetch nannies/wet nurses of focusId (rel.personId1)
                    if (rel.personId2 == focusId && (rel.type == "NANNY" || rel.type == "WET_NURSE")) {
                        ids.add(rel.personId1)
                    }
                    // Fetch children where focusId was nanny/wet nurse (rel.personId2)
                    if (rel.personId1 == focusId && (rel.type == "NANNY" || rel.type == "WET_NURSE")) {
                        ids.add(rel.personId2)
                    }
                }
                RelationFilter.CLOSE_CIRCLE -> {
                    if (rel.personId1 == focusId && (rel.type == "FRIEND" || rel.type == "NANNY" || rel.type == "WET_NURSE")) ids.add(rel.personId2)
                    if (rel.personId2 == focusId && (rel.type == "FRIEND" || rel.type == "NANNY" || rel.type == "WET_NURSE")) ids.add(rel.personId1)
                }
                else -> {}
            }
        }
        return ids
    }

    // CRUD Methods
    fun setActiveTree(id: Long) {
        _activeTreeId.value = id
        selectPerson(null)
    }

    fun createNewTree(name: String, description: String = "") {
        viewModelScope.launch {
            val newId = repository.insertTree(GenealogyTree(name = name, description = description))
            setActiveTree(newId)
            _statusMessage.value = "Создана новая база: $name"
        }
    }

    fun deleteTree(tree: GenealogyTree) {
        viewModelScope.launch {
            repository.deleteTree(tree)
            val remaining = repository.allTrees.first()
            if (remaining.isNotEmpty()) {
                setActiveTree(remaining.first().id)
            } else {
                _activeTreeId.value = null
            }
        }
    }

    fun updateTreeInfo(tree: GenealogyTree) {
        viewModelScope.launch {
            repository.updateTree(tree)
            _statusMessage.value = "База обновлена"
        }
    }

    fun selectPerson(id: Long?) {
        _selectedPersonId.value = id
    }

    fun updateSearchText(text: String) {
        _searchText.value = text
    }

    fun updateLivingFilter(filter: LivingFilter) {
        _livingFilter.value = filter
    }

    fun updateRelationFilter(filter: RelationFilter, focusPersonId: Long?) {
        _relationFilter.value = filter
        _focusPersonId.value = focusPersonId
    }

    fun updateMinAge(age: Int?) {
        _minAge.value = age
    }

    fun updateBirthYearRange(start: Int?, end: Int?) {
        _birthYearStart.value = start
        _birthYearEnd.value = end
    }

    fun clearFilters() {
        _searchText.value = ""
        _livingFilter.value = LivingFilter.ALL
        _relationFilter.value = RelationFilter.ALL
        _focusPersonId.value = null
        _minAge.value = null
        _birthYearStart.value = null
        _birthYearEnd.value = null
    }

    fun savePerson(person: Person, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val currentTreeId = _activeTreeId.value
            if (currentTreeId == null) {
                _statusMessage.value = "Ошибка: база не выбрана!"
                return@launch
            }

            val personWithTree = if (person.treeId == 0L || person.id == 0L) {
                person.copy(treeId = currentTreeId)
            } else person

            if (personWithTree.id == 0L) {
                val newId = repository.insertPerson(personWithTree)
                onComplete(newId)
            } else {
                repository.updatePerson(personWithTree)
                onComplete(personWithTree.id)
            }
        }
    }

    fun deletePerson(person: Person) {
        viewModelScope.launch {
            if (_selectedPersonId.value == person.id) {
                _selectedPersonId.value = null
            }
            if (_focusPersonId.value == person.id) {
                _focusPersonId.value = null
                _relationFilter.value = RelationFilter.ALL
            }
            repository.deletePerson(person)
        }
    }

    fun addRelationship(personId1: Long, personId2: Long, type: String) {
        viewModelScope.launch {
            if (personId1 == personId2) return@launch
            val currentTreeId = _activeTreeId.value ?: return@launch
            repository.insertRelationship(Relationship(
                treeId = currentTreeId,
                personId1 = personId1,
                personId2 = personId2,
                type = type
            ))
        }
    }

    fun removeRelationshipById(id: Long) {
        viewModelScope.launch {
            repository.deleteRelationshipById(id)
        }
    }

    fun removeSpecificRelationship(personId1: Long, personId2: Long, type: String) {
        viewModelScope.launch {
            repository.deleteSpecificRelationship(personId1, personId2, type)
        }
    }

    fun exportDatabase(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                val currentTreeId = _activeTreeId.value
                if (currentTreeId == null) {
                    _statusMessage.value = "Ошибка: база не выбрана!"
                    return@launch
                }

                // Fetch fresh data from DB to ensure completeness
                val peopleEntities = repository.getPeopleForTreeSuspend(currentTreeId)
                val relationshipEntities = repository.allRelationships.first().filter { it.treeId == currentTreeId }

                // Normalize IDs to 1..N for a "clean" export file
                val idMap = mutableMapOf<Long, Long>()
                val normalizedPeople = peopleEntities.mapIndexed { index, person ->
                    val newId = (index + 1).toLong()
                    idMap[person.id] = newId
                    person.toData().copy(id = newId)
                }

                val normalizedRelationships = relationshipEntities.mapIndexedNotNull { index, rel ->
                    val newP1 = idMap[rel.personId1]
                    val newP2 = idMap[rel.personId2]
                    if (newP1 != null && newP2 != null) {
                        rel.toData().copy(
                            id = (index + 1).toLong(),
                            personId1 = newP1,
                            personId2 = newP2
                        )
                    } else null
                }

                val data = GenealogyData(normalizedPeople, normalizedRelationships)
                val jsonString = jsonAdapter.indent("  ").toJson(data)

                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(jsonString)
                    }
                }
                _statusMessage.value = "Данные успешно экспортированы (${normalizedPeople.size} чел.)"
            } catch (e: Exception) {
                Log.e("GenealogyVM", "Export failed", e)
                _statusMessage.value = "Ошибка экспорта: ${e.localizedMessage}"
            }
        }
    }

    private val okHttpClient = OkHttpClient()

    fun importFromUrl(url: String, mode: ImportMode, createNewTree: Boolean = false, treeName: String? = null) {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Загрузка данных..."
                val jsonString = withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(url).build()
                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("Unexpected code $response")
                        response.body?.string()
                    }
                }

                if (jsonString == null) {
                    _statusMessage.value = "Ошибка: Пустой ответ от сервера"
                    return@launch
                }

                if (createNewTree && treeName != null) {
                    val newTreeId = repository.insertTree(GenealogyTree(name = treeName))
                    setActiveTree(newTreeId)
                }

                processImportJson(jsonString, mode)
            } catch (e: Exception) {
                Log.e("GenealogyVM", "Import from URL failed", e)
                _statusMessage.value = "Ошибка загрузки: ${e.localizedMessage}"
            }
        }
    }

    private suspend fun processImportJson(jsonString: String, mode: ImportMode) {
        val data = jsonAdapter.fromJson(jsonString)
        if (data == null) {
            _statusMessage.value = "Не удалось распознать формат файла!"
            return
        }

        // Use _activeTreeId directly to avoid race conditions with the combined activeTree state
        var treeId = _activeTreeId.value

        if (treeId == null) {
            treeId = repository.insertTree(GenealogyTree(name = "Импортированная база"))
            _activeTreeId.value = treeId
        }

        // Remap IDs to avoid collisions between different trees/imports
        val oldToNewIdMap = mutableMapOf<Long, Long>()

        // 1. Clear tree if REPLACE mode
        if (mode == ImportMode.REPLACE) {
            repository.clearPeopleForTree(treeId)
            repository.clearRelationshipsForTree(treeId)
        }

        // 2. Insert people and build ID mapping
        data.people.forEach { personData ->
            val personEntity = personData.toEntity().copy(id = 0, treeId = treeId)
            
            // If MERGE mode, we could potentially check for duplicates by name/birth, 
            // but for now, let's just insert everyone as new records to ensure no data is lost
            // especially since they are in the context of the current tree.
            
            val newId = repository.insertPerson(personEntity)
            oldToNewIdMap[personData.id] = newId
        }

        // 3. Insert relationships using the new ID mapping
        data.relationships.forEach { relData ->
            val newPersonId1 = oldToNewIdMap[relData.personId1]
            val newPersonId2 = oldToNewIdMap[relData.personId2]

            if (newPersonId1 != null && newPersonId2 != null) {
                repository.insertRelationship(Relationship(
                    id = 0,
                    treeId = treeId,
                    personId1 = newPersonId1,
                    personId2 = newPersonId2,
                    type = relData.type
                ))
            }
        }

        val totalCount = repository.getPeopleForTreeSuspend(treeId).size
        val addedCount = data.people.size

        _statusMessage.value = when (mode) {
            ImportMode.REPLACE -> "База полностью заменена. Загружено: $addedCount чел."
            ImportMode.MERGE -> "Объединение завершено. Добавлено: $addedCount чел. Всего в базе: $totalCount чел."
            ImportMode.UPDATE -> "Обновление завершено. Добавлено/обновлено: $addedCount чел. Всего в базе: $totalCount чел."
        }
    }

    fun importDatabase(contentResolver: ContentResolver, uri: Uri, mode: ImportMode, createNewTree: Boolean = false, treeName: String? = null) {
        viewModelScope.launch {
            try {
                if (createNewTree && treeName != null) {
                    val newTreeId = repository.insertTree(GenealogyTree(name = treeName))
                    setActiveTree(newTreeId)
                }

                val jsonString = StringBuilder()
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            jsonString.append(line)
                            line = reader.readLine()
                        }
                    }
                }
                processImportJson(jsonString.toString(), mode)
            } catch (e: Exception) {
                Log.e("GenealogyVM", "Import failed", e)
                _statusMessage.value = "Ошибка импорта: ${e.localizedMessage}"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun updateTheme(theme: AppTheme) {
        _appTheme.value = theme
        prefs.edit().putString("app_theme", theme.name).apply()
    }
}

enum class ImportMode {
    MERGE,    // Слияние (добавляем только новые)
    UPDATE,   // Обновление (добавляем новые + перезаписываем существующие)
    REPLACE   // Полное замещение (очищаем БД и загружаем заново)
}

data class PublicCollection(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val imageUrl: String,
    val downloadUrl: String
)

// Data holder for display purposes
data class DirectRelation(
    val relationshipId: Long,
    val person: Person,
    val type: DirectRelationType
)

enum class DirectRelationType(val localizedName: String) {
    PARENT_BIOLOGICAL("Родитель (биол.)"),
    PARENT_ADOPTED("Родитель (усыновитель)"),
    CHILD_BIOLOGICAL("Ребенок (биол.)"),
    CHILD_ADOPTED("Ребенок (усыновленный)"),
    SPOUSE("Супруг(а)"),
    EX_SPOUSE("Бывший(ая) супруг(а)"),
    FRIEND("Близкий друг"),
    NANNY("Няня"),
    WET_NURSE("Кормилица"),
    CARED_PERSON("Воспитанник"),
    OTHER("Связь")
}
