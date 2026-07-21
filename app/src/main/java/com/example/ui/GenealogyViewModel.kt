package com.example.ui

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

class GenealogyViewModel(application: Application) : AndroidViewModel(application) {

    private val database = GenealogyDatabase.getDatabase(application)
    private val repository = GenealogyRepository(database.genealogyDao())

    val allPeople: StateFlow<List<Person>> = repository.allPeople
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRelationships: StateFlow<List<Relationship>> = repository.allRelationships
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

    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)

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
            if (person.id == 0L) {
                val newId = repository.insertPerson(person)
                onComplete(newId)
            } else {
                repository.updatePerson(person)
                onComplete(person.id)
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
            repository.insertRelationship(Relationship(personId1 = personId1, personId2 = personId2, type = type))
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

    // Export and Import Logic
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val jsonAdapter = moshi.adapter(GenealogyData::class.java)

    fun exportDatabase(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                val peopleList = allPeople.value.map { it.toData() }
                val relList = allRelationships.value.map { it.toData() }
                val data = GenealogyData(peopleList, relList)
                val jsonString = jsonAdapter.indent("  ").toJson(data)

                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(jsonString)
                    }
                }
                _statusMessage.value = "Данные успешно экспортированы!"
            } catch (e: Exception) {
                Log.e("GenealogyVM", "Export failed", e)
                _statusMessage.value = "Ошибка экспорта: ${e.localizedMessage}"
            }
        }
    }

    fun importDatabase(contentResolver: ContentResolver, uri: Uri, mode: ImportMode) {
        viewModelScope.launch {
            try {
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

                val data = jsonAdapter.fromJson(jsonString.toString())
                if (data == null) {
                    _statusMessage.value = "Не удалось распознать формат файла!"
                    return@launch
                }

                val peopleEntities = data.people.map { it.toEntity() }
                val relationshipEntities = data.relationships.map { it.toEntity() }

                when (mode) {
                    ImportMode.REPLACE -> {
                        repository.replaceDatabase(peopleEntities, relationshipEntities)
                        _statusMessage.value = "Данные полностью замещены! Импортировано: ${peopleEntities.size} чел."
                    }
                    ImportMode.MERGE -> {
                        repository.mergeDatabase(peopleEntities, relationshipEntities, updateOnConflict = false)
                        _statusMessage.value = "Данные объединены! Новые записи добавлены."
                    }
                    ImportMode.UPDATE -> {
                        repository.mergeDatabase(peopleEntities, relationshipEntities, updateOnConflict = true)
                        _statusMessage.value = "Данные обновлены! Существующие записи перезаписаны."
                    }
                }
            } catch (e: Exception) {
                Log.e("GenealogyVM", "Import failed", e)
                _statusMessage.value = "Ошибка импорта: ${e.localizedMessage}"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}

enum class ImportMode {
    MERGE,    // Слияние (добавляем только новые)
    UPDATE,   // Обновление (добавляем новые + перезаписываем существующие)
    REPLACE   // Полное замещение (очищаем БД и загружаем заново)
}

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
