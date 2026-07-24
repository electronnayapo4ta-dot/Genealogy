package com.densappstudio.genealogy

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.densappstudio.genealogy.data.*
import com.densappstudio.genealogy.ui.*
import com.densappstudio.genealogy.ui.theme.MyApplicationTheme
import androidx.compose.animation.core.*
import androidx.compose.animation.Animatable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.util.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        // Убираем системную заставку как можно быстрее
        splashScreen.setKeepOnScreenCondition { false }
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var showBranding by remember { mutableStateOf(true) }

                if (showBranding) {
                    BrandingScreen(onFinished = { showBranding = false })
                } else {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun BrandingScreen(onFinished: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    // Анимация пульсации облака
    val cloudScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "cloudScale"
    )

    // Анимация мерцания частиц
    val particleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "particleAlpha"
    )

    // Анимация роста (проявления снизу вверх)
    val growthProgress = remember { Animatable(0f) }
    
    var flashCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        growthProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(2000, easing = EaseOutQuart)
        )
        repeat(6) {
            flashCount++
            delay(250)
        }
        delay(400)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1A3F), Color(0xFF0E3A5D), Color(0xFF052131))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // 1. Анимированные частицы данных
        Canvas(modifier = Modifier.fillMaxSize()) {
            val random = java.util.Random(123)
            repeat(40) {
                val x = random.nextFloat() * size.width
                val y = random.nextFloat() * size.height
                val speed = random.nextFloat()
                drawCircle(
                    color = Color(0xFF3FAF7D).copy(alpha = particleAlpha * speed),
                    radius = 3f,
                    center = Offset(x, y)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Золотое свечение
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD166).copy(alpha = 0.25f * growthProgress.value), 
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Дерево с эффектом роста (mask reveal)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .graphicsLayer(
                            compositingStrategy = CompositingStrategy.Offscreen,
                            alpha = growthProgress.value
                        )
                ) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.splash_genealogy),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    
                    // Маска для эффекта "роста" снизу вверх
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Transparent,
                                (1f - growthProgress.value) to Color.Transparent,
                                (1.1f - growthProgress.value) to Color.Black,
                                1f to Color.Black
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
                
                // Вспышки портретов
                if (growthProgress.value > 0.6f) {
                    Canvas(modifier = Modifier.fillMaxWidth(0.85f).aspectRatio(1f)) {
                        val positions = listOf(
                            Offset(0.5f, 0.35f), Offset(0.38f, 0.45f), Offset(0.62f, 0.48f),
                            Offset(0.3f, 0.6f), Offset(0.7f, 0.58f), Offset(0.52f, 0.55f)
                        )
                        val count = (flashCount % (positions.size + 1))
                        for (i in 0 until count) {
                            if (i < positions.size) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.9f),
                                    radius = 8f,
                                    center = Offset(size.width * positions[i].x, size.height * positions[i].y)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.3f),
                                    radius = 20f,
                                    center = Offset(size.width * positions[i].x, size.height * positions[i].y)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Облако синхронизации
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = null,
                tint = Color(0xFF5BA9E0),
                modifier = Modifier
                    .size(56.dp)
                    .scale(cloudScale)
                    .graphicsLayer(alpha = 0.8f)
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: GenealogyViewModel = viewModel()) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(2) } // Start with Library (index 2 in old, will be 0 in new)
    
    // Initial tab set to 0 (Library) in the new logic
    LaunchedEffect(Unit) {
        currentTab = 0
    }
    
    // Selected Screen State
    var isEditingPerson by remember { mutableStateOf(false) }
    var editPersonData by remember { mutableStateOf<Person?>(null) }
    
    val selectedPersonId by viewModel.selectedPersonId.collectAsStateWithLifecycle()
    val selectedPerson by viewModel.selectedPerson.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    // Clear Status messages with Toasts
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isEditingPerson && selectedPersonId == null) {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AutoStories, contentDescription = "Библиотека") },
                        label = { Text("Библиотека") },
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.People, contentDescription = "Древо") },
                        label = { Text("Древо") },
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
                        label = { Text("Поиск") },
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Backup, contentDescription = "Данные") },
                        label = { Text("Данные") },
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // Edit / Add Mode
                isEditingPerson -> {
                    EditPersonScreen(
                        person = editPersonData,
                        onCancel = {
                            isEditingPerson = false
                            editPersonData = null
                        },
                        onSave = { updatedPerson ->
                            viewModel.savePerson(updatedPerson) { savedId ->
                                if (editPersonData == null) {
                                    // If newly added, select them immediately
                                    viewModel.selectPerson(savedId)
                                }
                            }
                            isEditingPerson = false
                            editPersonData = null
                        }
                    )
                }
                
                // Detailed Member View
                selectedPersonId != null && selectedPerson != null -> {
                    PersonDetailsScreen(
                        person = selectedPerson!!,
                        viewModel = viewModel,
                        onBack = { viewModel.selectPerson(null) },
                        onEdit = {
                            editPersonData = selectedPerson
                            isEditingPerson = true
                        }
                    )
                }
                
                // Main Tab Contents
                else -> {
                    when (currentTab) {
                        0 -> LibraryTabScreen(viewModel = viewModel)
                        1 -> TreeTabScreen(
                            viewModel = viewModel,
                            onAddClick = {
                                editPersonData = null
                                isEditingPerson = true
                            }
                        )
                        2 -> SearchTabScreen(viewModel = viewModel)
                        3 -> BackupTabScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}



// ==========================================
// TAB 1: TREE / PEOPLE SCREEN
// ==========================================
@Composable
fun TreeTabScreen(
    viewModel: GenealogyViewModel,
    onAddClick: () -> Unit
) {
    val people by viewModel.allPeople.collectAsStateWithLifecycle()
    val activeTree by viewModel.activeTree.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.densappstudio.genealogy.R.drawable.genealogy_banner),
                    contentDescription = "Родословная",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = activeTree?.name ?: "Семейное Древо",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = activeTree?.description.takeIf { !it.isNullOrBlank() } ?: "Хранение истории рода и близких связей",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (people.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FamilyRestroom,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "База данных пуста",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Нажмите кнопку '+' внизу экрана, чтобы добавить первого человека в генеалогию рода.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(people) { person ->
                        PersonGridItem(
                            person = person,
                            viewModel = viewModel,
                            onClick = { viewModel.selectPerson(person.id) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_person_fab"),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить человека")
        }
    }
}



// ==========================================
// GRID/CARD COMPONENT with Mourning Frame
// ==========================================
@Composable
fun PersonGridItem(
    person: Person,
    viewModel: GenealogyViewModel,
    onClick: () -> Unit
) {
    val age = viewModel.getAge(person)
    
    // Distinct styles for deceased (mourning frame)
    val cardBorder = if (person.isDeceased) {
        BorderStroke(2.dp, Color.Black)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    
    val cardColors = if (person.isDeceased) {
        CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .testTag("person_card_${person.id}"),
        shape = RoundedCornerShape(12.dp),
        border = cardBorder,
        colors = cardColors
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Photo & Mourning Ribbon container
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        if (person.isDeceased) Color.Black else MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!person.photoUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = Uri.parse(person.photoUri),
                        contentDescription = "Фото ${person.firstName}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (person.gender == "FEMALE") Icons.Default.Female else Icons.Default.Male,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = if (person.isDeceased) Color.DarkGray else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                // Mourning ribbon across the bottom right of the photo
                if (person.isDeceased) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(w, h * 0.55f)
                            lineTo(w * 0.55f, h)
                            lineTo(w, h)
                            close()
                        }
                        drawPath(path = path, color = Color.Black)
                    }
                    
                    // Small gold/black outline circle representing tribute
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.5.dp, Color.Black, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Name
            Text(
                text = "${person.lastName}\n${person.firstName}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Life dates and Age label
            val dateLabel = buildString {
                if (person.birthYear != null) {
                    append(person.birthYear)
                    if (person.isDeceased) {
                        append(" — ")
                        append(person.deathYear ?: "†")
                    } else {
                        append(" (")
                        append(age ?: "?")
                        append(" л.)")
                    }
                } else if (person.isDeceased) {
                    append("† Усопший")
                } else {
                    append("Жив")
                }
            }

            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (person.isDeceased) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}



// ==========================================
// DETAILS SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailsScreen(
    person: Person,
    viewModel: GenealogyViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    var showAddRelationDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val relationships by viewModel.allRelationships.collectAsStateWithLifecycle()
    
    val directRelations = remember(person.id, relationships) {
        viewModel.getDirectRelationshipsForPerson(person.id, relationships)
    }

    Scaffold(
        containerColor = if (person.isDeceased) Color(0xFF121212) else MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(person.fullName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (person.isDeceased) Color.Black else MaterialTheme.colorScheme.surface,
                    titleContentColor = if (person.isDeceased) Color.White else MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = if (person.isDeceased) Color.White else MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = if (person.isDeceased) Color.White else MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (person.isDeceased) Color(0xFF161616) else MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Card Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (person.isDeceased) Color.Black else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Photo with mourning decorations
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(if (person.isDeceased) Color.Black else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!person.photoUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = Uri.parse(person.photoUri),
                                contentDescription = "Фото ${person.firstName}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = if (person.gender == "FEMALE") Icons.Default.Female else Icons.Default.Male,
                                contentDescription = null,
                                modifier = Modifier.size(84.dp),
                                tint = if (person.isDeceased) Color.DarkGray else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }

                        if (person.isDeceased) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val path = Path().apply {
                                    moveTo(w, h * 0.65f)
                                    lineTo(w * 0.65f, h)
                                    lineTo(w, h)
                                    close()
                                }
                                drawPath(path = path, color = Color.Black)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(2.5.dp, Color.Black, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = person.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (person.isDeceased) Color.White else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val lifeText = buildString {
                        if (person.birthDate != null) {
                            append("Род: ${person.birthDate}")
                            if (person.birthPlace != null) append(" (${person.birthPlace})")
                        }
                        if (person.isDeceased) {
                            append("\nУход: ${person.deathDate ?: "неизвестно"}")
                            if (person.deathPlace != null) append(" (${person.deathPlace})")
                        }
                    }

                    if (lifeText.isNotEmpty()) {
                        Text(
                            text = lifeText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (person.isDeceased) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    if (person.isDeceased) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Circle,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Обрел вечный покой",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Biography & Demographics Section
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Places & Info Cards
                if (!person.residence.isNullOrBlank()) {
                    InfoRow(label = "Места жительства", value = person.residence, isDeceased = person.isDeceased)
                }
                if (!person.education.isNullOrBlank()) {
                    InfoRow(label = "Образование", value = person.education, isDeceased = person.isDeceased)
                }
                if (!person.biography.isNullOrBlank()) {
                    Text(
                        text = "Жизненный путь",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (person.isDeceased) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = person.biography,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (person.isDeceased) Color.LightGray else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Divider(color = if (person.isDeceased) Color.DarkGray else MaterialTheme.colorScheme.outlineVariant)
                
                // CONNECTIONS SECTION
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Родственные и близкие связи",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (person.isDeceased) Color.White else MaterialTheme.colorScheme.primary
                    )
                    
                    Button(
                        onClick = { showAddRelationDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (person.isDeceased) Color.White else MaterialTheme.colorScheme.primary,
                            contentColor = if (person.isDeceased) Color.Black else MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Связать", fontSize = 12.sp)
                    }
                }

                if (directRelations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Нет зарегистрированных связей.\nНажмите кнопку 'Связать' выше, чтобы построить древо.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = if (person.isDeceased) Color.DarkGray else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        for (relation in directRelations) {
                            RelationItemCard(
                                relation = relation,
                                isDeceasedContext = person.isDeceased,
                                onClick = { viewModel.selectPerson(relation.person.id) },
                                onUnlink = {
                                    viewModel.removeRelationshipById(relation.relationshipId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Unlink or delete confirmation
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Удалить запись?") },
            text = { Text("Это действие полностью удалит ${person.fullName} из базы данных, включая все его родственные связи. Это действие необратимо.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePerson(person)
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Add Relation Dialog
    if (showAddRelationDialog) {
        AddRelationshipDialog(
            subject = person,
            viewModel = viewModel,
            onDismiss = { showAddRelationDialog = false }
        )
    }
}



@Composable
fun InfoRow(label: String, value: String, isDeceased: Boolean) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDeceased) Color.DarkGray else MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDeceased) Color.LightGray else MaterialTheme.colorScheme.onSurface
        )
    }
}



@Composable
fun RelationItemCard(
    relation: DirectRelation,
    isDeceasedContext: Boolean,
    onClick: () -> Unit,
    onUnlink: () -> Unit
) {
    val bColors = if (isDeceasedContext) {
        CardDefaults.cardColors(containerColor = Color(0xFF222222), contentColor = Color.White)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = bColors,
        border = if (relation.person.isDeceased) BorderStroke(1.dp, Color.Black) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Tiny avatar inside relationship block
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (relation.person.isDeceased) Color.Black else MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!relation.person.photoUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = Uri.parse(relation.person.photoUri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = if (relation.person.gender == "FEMALE") Icons.Default.Female else Icons.Default.Male,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (relation.person.isDeceased) Color.DarkGray else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    if (relation.person.isDeceased) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val path = Path().apply {
                                moveTo(w, h * 0.55f)
                                lineTo(w * 0.55f, h)
                                lineTo(w, h)
                                close()
                            }
                            drawPath(path = path, color = Color.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = relation.person.fullName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = relation.type.localizedName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (relation.person.isDeceased) Color.LightGray else MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onUnlink) {
                Icon(
                    imageVector = Icons.Default.LinkOff,
                    contentDescription = "Удалить связь",
                    tint = if (isDeceasedContext) Color.LightGray else MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}



// ==========================================
// ADD RELATIONSHIP DIALOG
// ==========================================
@Composable
fun AddRelationshipDialog(
    subject: Person,
    viewModel: GenealogyViewModel,
    onDismiss: () -> Unit
) {
    val allPeople by viewModel.allPeople.collectAsStateWithLifecycle()
    val availablePeople = remember(allPeople, subject.id) {
        allPeople.filter { it.id != subject.id }
    }

    var selectedTargetPerson by remember { mutableStateOf<Person?>(null) }
    var selectedTypeIndex by remember { mutableStateOf(0) }
    var showTargetSelector by remember { mutableStateOf(false) }

    val relationTypes = listOf(
        "PARENT" to "Родитель (биологический)",
        "PARENT_ADOPTED" to "Родитель (усыновитель/няня/друг)", // Managed by UI
        "SPOUSE" to "Супруг(а) / Брак",
        "EX_SPOUSE" to "Развод / Бывший супруг(а)",
        "FRIEND" to "Близкий друг",
        "NANNY" to "Няня",
        "WET_NURSE" to "Кормилица"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Управление связями") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Выберите тип связи между ${subject.fullName} и другим лицом.",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Selected Target Person field
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTargetSelector = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Связать с лицом:", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = selectedTargetPerson?.fullName ?: "Нажмите для выбора...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                // Type selector
                Text("Выберите роль этого лица относительно ${subject.firstName}:", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    relationTypes.forEachIndexed { index, (typeKey, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedTypeIndex == index,
                                    onClick = { selectedTypeIndex = index }
                                )
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedTypeIndex == index,
                                onClick = { selectedTypeIndex = index }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = selectedTargetPerson
                    if (target != null) {
                        val (typeKey, _) = relationTypes[selectedTypeIndex]
                        
                        // Decide order of insertion to keep schema clean:
                        // For PARENT: personId1 is parent, personId2 is child.
                        // If subject.id wants target to be parent, then personId1 = target.id, personId2 = subject.id.
                        // Let's implement correct logic based on selection:
                        when (typeKey) {
                            "PARENT", "PARENT_ADOPTED" -> {
                                // selected target person is the parent
                                viewModel.addRelationship(
                                    personId1 = target.id,
                                    personId2 = subject.id,
                                    type = typeKey
                                )
                            }
                            "NANNY", "WET_NURSE" -> {
                                // Target person is the nanny/wet nurse, subject is the child
                                viewModel.addRelationship(
                                    personId1 = target.id,
                                    personId2 = subject.id,
                                    type = typeKey
                                )
                            }
                            else -> {
                                // Symmetric spouses, friends, etc.
                                viewModel.addRelationship(
                                    personId1 = subject.id,
                                    personId2 = target.id,
                                    type = typeKey
                                )
                            }
                        }
                        onDismiss()
                    } else {
                        // Error toast
                    }
                },
                enabled = selectedTargetPerson != null
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )

    // Secondary Dialog to pick target person
    if (showTargetSelector) {
        Dialog(onDismissRequest = { showTargetSelector = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Выберите лицо из базы",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (availablePeople.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Сначала добавьте других людей в базу данных.")
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(availablePeople) { person ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedTargetPerson = person
                                            showTargetSelector = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (person.gender == "FEMALE") Icons.Default.Female else Icons.Default.Male,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    Text(person.fullName, style = MaterialTheme.typography.bodyMedium)
                                }
                                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = { showTargetSelector = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Закрыть")
                    }
                }
            }
        }
    }
}



// ==========================================
// FORM SCREEN: EDIT / ADD PERSON
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPersonScreen(
    person: Person?,
    onCancel: () -> Unit,
    onSave: (Person) -> Unit
) {
    val context = LocalContext.current
    
    // Form States
    var firstName by remember { mutableStateOf(person?.firstName ?: "") }
    var lastName by remember { mutableStateOf(person?.lastName ?: "") }
    var patronymic by remember { mutableStateOf(person?.patronymic ?: "") }
    var maidenName by remember { mutableStateOf(person?.maidenName ?: "") }
    var gender by remember { mutableStateOf(person?.gender ?: "MALE") }
    var birthDate by remember { mutableStateOf(person?.birthDate ?: "") }
    var birthPlace by remember { mutableStateOf(person?.birthPlace ?: "") }
    var deathDate by remember { mutableStateOf(person?.deathDate ?: "") }
    var deathPlace by remember { mutableStateOf(person?.deathPlace ?: "") }
    var isDeceased by remember { mutableStateOf(person?.isDeceased ?: false) }
    var education by remember { mutableStateOf(person?.education ?: "") }
    var residence by remember { mutableStateOf(person?.residence ?: "") }
    var biography by remember { mutableStateOf(person?.biography ?: "") }
    var photoUriString by remember { mutableStateOf(person?.photoUri ?: "") }

    // Try to auto-parse birth year from text
    val birthYear: Int? = remember(birthDate) {
        // Regex to find 4 digit years
        val match = Regex("\\b\\d{4}\\b").find(birthDate)
        match?.value?.toIntOrNull()
    }
    
    val deathYear: Int? = remember(deathDate) {
        val match = Regex("\\b\\d{4}\\b").find(deathDate)
        match?.value?.toIntOrNull()
    }

    // Gallery Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                context.contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                    val fileName = "person_photo_${System.currentTimeMillis()}.jpg"
                    val file = File(context.filesDir, fileName)
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    photoUriString = Uri.fromFile(file).toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Ошибка при сохранении фото", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (person == null) "Добавление в род" else "Редактирование") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (firstName.isBlank() || lastName.isBlank()) {
                                Toast.makeText(context, "Имя и Фамилия обязательны к заполнению!", Toast.LENGTH_LONG).show()
                            } else {
                                val savedPerson = Person(
                                    id = person?.id ?: 0L,
                                    firstName = firstName.trim(),
                                    lastName = lastName.trim(),
                                    patronymic = patronymic.trim(),
                                    maidenName = if (gender == "FEMALE" && maidenName.isNotBlank()) maidenName.trim() else null,
                                    gender = gender,
                                    birthDate = birthDate.trim().ifEmpty { null },
                                    birthYear = birthYear,
                                    birthPlace = birthPlace.trim().ifEmpty { null },
                                    deathDate = if (isDeceased) deathDate.trim().ifEmpty { null } else null,
                                    deathYear = if (isDeceased) deathYear else null,
                                    deathPlace = if (isDeceased) deathPlace.trim().ifEmpty { null } else null,
                                    isDeceased = isDeceased,
                                    education = education.trim().ifEmpty { null },
                                    residence = residence.trim().ifEmpty { null },
                                    biography = biography.trim().ifEmpty { null },
                                    photoUri = photoUriString.ifEmpty { null }
                                )
                                onSave(savedPerson)
                            }
                        },
                        modifier = Modifier.testTag("save_person_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Сохранить")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo Picker Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { photoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUriString.isNotEmpty()) {
                            AsyncImage(
                                model = Uri.parse(photoUriString),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Выбрать фото",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (photoUriString.isEmpty()) "Выбрать фотографию" else "Изменить фотографию",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { photoPickerLauncher.launch("image/*") }
                    )
                }
            }

            // Text fields
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Фамилия *") },
                modifier = Modifier.fillMaxWidth().testTag("last_name_input"),
                singleLine = true
            )

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("Имя *") },
                modifier = Modifier.fillMaxWidth().testTag("first_name_input"),
                singleLine = true
            )

            OutlinedTextField(
                value = patronymic,
                onValueChange = { patronymic = it },
                label = { Text("Отчество") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Gender Segmented Selection
            Column {
                Text("Пол", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val genders = listOf("MALE" to "Мужской", "FEMALE" to "Женский")
                    genders.forEach { (gKey, gLabel) ->
                        FilterChip(
                            selected = gender == gKey,
                            onClick = { gender = gKey },
                            label = { Text(gLabel) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Maiden Name for Females
            if (gender == "FEMALE") {
                OutlinedTextField(
                    value = maidenName,
                    onValueChange = { maidenName = it },
                    label = { Text("Девичья фамилия") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Divider()

            // Birth Info
            OutlinedTextField(
                value = birthDate,
                onValueChange = { birthDate = it },
                label = { Text("Дата рождения (например, 12.05.1950 или 1950)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = birthPlace,
                onValueChange = { birthPlace = it },
                label = { Text("Место рождения") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Status: Deceased (Mourning check)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isDeceased = !isDeceased }
                    .padding(vertical = 8.dp)
            ) {
                Checkbox(checked = isDeceased, onCheckedChange = { isDeceased = it })
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Обрел вечный покой (Усопший)", fontWeight = FontWeight.Bold)
                    Text("Будет отображаться в траурной рамке", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (isDeceased) {
                OutlinedTextField(
                    value = deathDate,
                    onValueChange = { deathDate = it },
                    label = { Text("Дата смерти / ухода") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = deathPlace,
                    onValueChange = { deathPlace = it },
                    label = { Text("Место захоронения / смерти") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Divider()

            // Biography & Resume details
            OutlinedTextField(
                value = residence,
                onValueChange = { residence = it },
                label = { Text("Места жительства (города, страны...)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = education,
                onValueChange = { education = it },
                label = { Text("Образование / Род деятельности") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = biography,
                onValueChange = { biography = it },
                label = { Text("Жизненный путь / Биография") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )
        }
    }
}



@Composable
fun SearchTabScreen(viewModel: GenealogyViewModel) {
    val searchText by viewModel.searchText.collectAsStateWithLifecycle()
    val livingFilter by viewModel.livingFilter.collectAsStateWithLifecycle()
    val relationFilter by viewModel.relationFilter.collectAsStateWithLifecycle()
    val focusPersonId by viewModel.focusPersonId.collectAsStateWithLifecycle()
    val minAge by viewModel.minAge.collectAsStateWithLifecycle()
    val birthYearStart by viewModel.birthYearStart.collectAsStateWithLifecycle()
    val birthYearEnd by viewModel.birthYearEnd.collectAsStateWithLifecycle()

    val people by viewModel.allPeople.collectAsStateWithLifecycle()
    val searchResults by viewModel.filteredPeople.collectAsStateWithLifecycle()
    val activeTree by viewModel.activeTree.collectAsStateWithLifecycle()

    var showFocusSelector by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(
                "Генеалогический Поиск",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "База: ${activeTree?.name ?: "не выбрана"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Search Bar
        OutlinedTextField(
            value = searchText,
            onValueChange = { viewModel.updateSearchText(it) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchText("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Очистить")
                    }
                }
            },
            placeholder = { Text("Поиск по имени, местам, биографии...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Expanded collapsible filter panel
        var filtersExpanded by remember { mutableStateOf(false) }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { filtersExpanded = !filtersExpanded }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Дополнительные фильтры поиска",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = if (filtersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }

        if (filtersExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 2. Living filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Статус:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    FilterChip(
                        selected = livingFilter == LivingFilter.ALL,
                        onClick = { viewModel.updateLivingFilter(LivingFilter.ALL) },
                        label = { Text("Все") }
                    )
                    FilterChip(
                        selected = livingFilter == LivingFilter.LIVING,
                        onClick = { viewModel.updateLivingFilter(LivingFilter.LIVING) },
                        label = { Text("Живые") }
                    )
                    FilterChip(
                        selected = livingFilter == LivingFilter.DECEASED,
                        onClick = { viewModel.updateLivingFilter(LivingFilter.DECEASED) },
                        label = { Text("Усопшие") }
                    )
                }

                // 3. Minimum Age Slider (Older than N years)
                Column {
                    val ageLabel = if (minAge == null) "Показать любой возраст" else "Показать возраст старше: $minAge лет"
                    Text(ageLabel, style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = minAge?.toFloat() ?: 0f,
                        onValueChange = { viewModel.updateMinAge(if (it == 0f) null else it.toInt()) },
                        valueRange = 0f..100f,
                        steps = 20
                    )
                }

                // 4. Birth Year Interval
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Год рождения от:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = birthYearStart?.toString() ?: "",
                        onValueChange = { viewModel.updateBirthYearRange(it.toIntOrNull(), birthYearEnd) },
                        modifier = Modifier.width(70.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )
                    Text("до:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = birthYearEnd?.toString() ?: "",
                        onValueChange = { viewModel.updateBirthYearRange(birthYearStart, it.toIntOrNull()) },
                        modifier = Modifier.width(70.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )
                    IconButton(
                        onClick = { viewModel.updateBirthYearRange(null, null) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Сброс")
                    }
                }

                // 5. Relations Explorer Relative to a focus person
                Divider()
                Text("Поиск по степени родства:", style = MaterialTheme.typography.labelMedium)
                
                Card(
                    onClick = { showFocusSelector = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val focusPerson = people.find { it.id == focusPersonId }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Связи относительно лица:", style = MaterialTheme.typography.labelSmall)
                            Text(
                                focusPerson?.fullName ?: "Нажмите для выбора центральной фигуры...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                if (focusPersonId != null) {
                    // Type of relations relative to focus
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val relationFilters = listOf(
                            RelationFilter.ALL to "Все родственники",
                            RelationFilter.PARENTS to "Родители",
                            RelationFilter.CHILDREN to "Дети (все)",
                            RelationFilter.ADOPTED_CHILDREN to "Только усыновленные",
                            RelationFilter.SPOUSES to "Супруги / Разводы",
                            RelationFilter.CLOSE_CIRCLE to "Близкие (Друзья/Няни)"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            relationFilters.forEach { (rfKey, label) ->
                                FilterChip(
                                    selected = relationFilter == rfKey,
                                    onClick = { viewModel.updateRelationFilter(rfKey, focusPersonId) },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { viewModel.clearFilters() },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.textButtonColors()
                ) {
                    Text("Сбросить все")
                }
            }
        }

        // Search Result list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Найдено записей: ${searchResults.size}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Ничего не найдено.\nПопробуйте изменить условия фильтрации.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(searchResults) { person ->
                    SearchCardItem(
                        person = person,
                        onClick = { viewModel.selectPerson(person.id) }
                    )
                }
            }
        }
    }

    // Focus Selector Dialog
    if (showFocusSelector) {
        Dialog(onDismissRequest = { showFocusSelector = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Выберите центр связей",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(people) { person ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateRelationFilter(relationFilter, person.id)
                                        showFocusSelector = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (person.gender == "FEMALE") Icons.Default.Female else Icons.Default.Male,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Text(person.fullName, style = MaterialTheme.typography.bodyMedium)
                            }
                            Divider()
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = { showFocusSelector = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Закрыть")
                    }
                }
            }
        }
    }
}

@Composable
fun SearchCardItem(person: Person, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (person.isDeceased) Color(0xFF1C1C1C) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            contentColor = if (person.isDeceased) Color.White else MaterialTheme.colorScheme.onSurface
        ),
        border = if (person.isDeceased) BorderStroke(1.5.dp, Color.Black) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (person.isDeceased) Color.Black else MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!person.photoUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = Uri.parse(person.photoUri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (person.gender == "FEMALE") Icons.Default.Female else Icons.Default.Male,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (person.isDeceased) Color.DarkGray else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                if (person.isDeceased) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(w, h * 0.55f)
                            lineTo(w * 0.55f, h)
                            lineTo(w, h)
                            close()
                        }
                        drawPath(path = path, color = Color.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = person.fullName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                
                val subText = buildString {
                    if (person.birthDate != null) {
                        append("Род: ${person.birthDate} г. ")
                    }
                    if (person.isDeceased) {
                        append(" † Усопший")
                    } else if (person.birthYear != null) {
                        append(" (живой, ${2026 - person.birthYear} лет)")
                    }
                }
                
                Text(
                    text = subText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (person.isDeceased) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}



// ==========================================
// TAB 3: LIBRARY / PUBLIC COLLECTIONS
// ==========================================
@Composable
fun LibraryTabScreen(viewModel: GenealogyViewModel) {
    val localTrees by viewModel.allTrees.collectAsStateWithLifecycle()
    val activeTreeId by viewModel.activeTreeId.collectAsStateWithLifecycle()
    val publicCollections by viewModel.publicCollections.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var showCreateTreeDialog by remember { mutableStateOf(false) }
    var selectedPublicCollection by remember { mutableStateOf<PublicCollection?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    
    var treeToEdit by remember { mutableStateOf<GenealogyTree?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val collectionsSourceUrl = "https://github.com/electronnayapo4ta-dot/Genealogy/tree/main/collections"

    var importUri by remember { mutableStateOf<Uri?>(null) }
    var showImportAsNewDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            importUri = it
            showImportAsNewDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "Моя Библиотека",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // NEW: Source Link Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Источник коллекций (GitHub):", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = "electronnayapo4ta-dot/Genealogy",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Genealogy Collections URL", collectionsSourceUrl)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Ссылка скопирована!", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Скопировать ссылку", modifier = Modifier.size(20.dp))
                }
            }
        }

        // SECTION 1: LOCAL DATABASES
        Text("Ваши сохранённые базы родов", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            localTrees.forEach { tree ->
                LocalTreeCard(
                    tree = tree,
                    isActive = tree.id == activeTreeId,
                    onClick = { viewModel.setActiveTree(tree.id) },
                    onDelete = { viewModel.deleteTree(tree) },
                    onRename = {
                        treeToEdit = tree
                        showRenameDialog = true
                    }
                )
            }
            
            // Invitation to create new
            Card(
                onClick = { showCreateTreeDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Создать новую базу рода", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Начните новую генеалогическую историю с чистого листа", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            OutlinedButton(
                onClick = { filePickerLauncher.launch("application/json") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Импортировать базу из файла как новую")
            }
        }

        Divider()

        // SECTION 2: PUBLIC COLLECTIONS
        Text("Общественные коллекции", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Генеалогические базы данных великих семей и исторических личностей.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        publicCollections.forEach { collection ->
            LibraryItemCard(
                collection = collection,
                onDownloadClick = {
                    selectedPublicCollection = collection
                    showImportDialog = true
                }
            )
        }
    }

    if (showCreateTreeDialog) {
        CreateTreeDialog(
            onDismiss = { showCreateTreeDialog = false },
            onCreate = { name, desc ->
                viewModel.createNewTree(name, desc)
                showCreateTreeDialog = false
            }
        )
    }

    if (showRenameDialog && treeToEdit != null) {
        RenameTreeDialog(
            currentName = treeToEdit!!.name,
            currentDesc = treeToEdit!!.description,
            onDismiss = { 
                showRenameDialog = false 
                treeToEdit = null
            },
            onConfirm = { newName, newDesc ->
                viewModel.updateTreeInfo(treeToEdit!!.copy(name = newName, description = newDesc))
                showRenameDialog = false
                treeToEdit = null
            }
        )
    }

    if (showImportAsNewDialog && importUri != null) {
        var importName by remember { mutableStateOf("Импортированный род") }
        
        AlertDialog(
            onDismissRequest = { showImportAsNewDialog = false },
            title = { Text("Импорт новой базы") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Введите название для новой базы:")
                    OutlinedTextField(
                        value = importName,
                        onValueChange = { importName = it },
                        label = { Text("Название базы") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.importDatabase(
                        context.contentResolver,
                        importUri!!,
                        ImportMode.REPLACE,
                        createNewTree = true,
                        treeName = importName
                    )
                    showImportAsNewDialog = false
                    importUri = null
                }) {
                    Text("Импортировать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportAsNewDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showImportDialog && selectedPublicCollection != null) {
        var importMode by remember { mutableStateOf(ImportMode.MERGE) }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Загрузить базу: ${selectedPublicCollection!!.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Выберите режим импорта в активную базу (${localTrees.find { it.id == activeTreeId }?.name}):")
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ImportModeOption(
                            title = "Слияние (Merge)",
                            description = "Добавить только новых людей",
                            selected = importMode == ImportMode.MERGE,
                            onClick = { importMode = ImportMode.MERGE }
                        )
                        ImportModeOption(
                            title = "Обновление (Update)",
                            description = "Обновить существующих и добавить новых",
                            selected = importMode == ImportMode.UPDATE,
                            onClick = { importMode = ImportMode.UPDATE }
                        )
                        ImportModeOption(
                            title = "Замещение (Replace)",
                            description = "ОЧИСТИТЬ ТЕКУЩУЮ базу и загрузить эту",
                            selected = importMode == ImportMode.REPLACE,
                            isWarning = true,
                            onClick = { importMode = ImportMode.REPLACE }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.importFromUrl(selectedPublicCollection!!.downloadUrl, importMode)
                    showImportDialog = false
                }) {
                    Text("Загрузить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun LocalTreeCard(
    tree: GenealogyTree,
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Folder,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(tree.name, fontWeight = FontWeight.Bold)
                    if (tree.description.isNotEmpty()) {
                        Text(tree.description, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            
            Row {
                IconButton(onClick = onRename) {
                    Icon(Icons.Default.Edit, contentDescription = "Переименовать", modifier = Modifier.size(20.dp))
                }
                if (!isActive) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun RenameTreeDialog(
    currentName: String,
    currentDesc: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var desc by remember { mutableStateOf(currentDesc) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактирование базы") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, desc) },
                enabled = name.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun CreateTreeDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая база рода") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название (например, Род Ивановых)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Описание (необязательно)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, desc) },
                enabled = name.isNotBlank()
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun LibraryItemCard(collection: PublicCollection, onDownloadClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AsyncImage(
                model = collection.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = collection.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Автор: ${collection.author}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = collection.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDownloadClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Добавить в моё древо")
                }
            }
        }
    }
}

@Composable
fun ImportModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    isWarning: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = if (isWarning && selected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isWarning && selected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==========================================
// TAB 4: BACKUP / IMPORT & EXPORT SCREEN
// ==========================================
@Composable
fun BackupTabScreen(viewModel: GenealogyViewModel) {
    val context = LocalContext.current
    var selectedImportMode by remember { mutableStateOf(ImportMode.MERGE) }
    val activeTree by viewModel.activeTree.collectAsStateWithLifecycle()

    // Backup Export File creator launcher
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportDatabase(context.contentResolver, it)
        }
    }

    // Backup Import File picker launcher
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importDatabase(context.contentResolver, it, selectedImportMode)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Экспорт и импорт базы данных",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Облачное и локальное архивирование", fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Вы можете экспортировать вашу текущую активную базу (${activeTree?.name ?: "без названия"}) в один структурированный JSON файл. При переносе на новое устройство вы сможете восстановить все родственные связи и биографии.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Divider()

        // EXPORT CARD
        Text("Выгрузка данных", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Экспорт всей базы генеалогии (люди и связи) в стандартный файл json.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = {
                        exportFileLauncher.launch("genealogy_backup.json")
                    },
                    modifier = Modifier.fillMaxWidth().testTag("export_button")
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Экспортировать БД в файл")
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // IMPORT CARD
        Text("Загрузка данных", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Импорт базы из ранее сохраненного файла. Пожалуйста, укажите режим интеграции записей:",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Segmented Import Mode Picker
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedImportMode == ImportMode.MERGE,
                                onClick = { selectedImportMode = ImportMode.MERGE }
                            )
                    ) {
                        RadioButton(
                            selected = selectedImportMode == ImportMode.MERGE,
                            onClick = { selectedImportMode = ImportMode.MERGE }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Режим слияния (Merge)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Добавляются только новые люди которых нет в текущей БД", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedImportMode == ImportMode.UPDATE,
                                onClick = { selectedImportMode = ImportMode.UPDATE }
                            )
                    ) {
                        RadioButton(
                            selected = selectedImportMode == ImportMode.UPDATE,
                            onClick = { selectedImportMode = ImportMode.UPDATE }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Режим обновления (Update)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Перезаписывает существующие карточки с теми же ID и добавляет новые", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedImportMode == ImportMode.REPLACE,
                                onClick = { selectedImportMode = ImportMode.REPLACE }
                            )
                    ) {
                        RadioButton(
                            selected = selectedImportMode == ImportMode.REPLACE,
                            onClick = { selectedImportMode = ImportMode.REPLACE }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Полное замещение (Full Replacement)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                            Text("Очищает ВСЮ базу данных перед загрузкой. Все ваши текущие данные пропадут!", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        importFileLauncher.launch("application/json")
                    },
                    modifier = Modifier.fillMaxWidth().testTag("import_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedImportMode == ImportMode.REPLACE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Импортировать БД из файла")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}


