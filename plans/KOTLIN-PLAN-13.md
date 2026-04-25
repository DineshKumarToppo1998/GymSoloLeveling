# KOTLIN-PLAN-13.md — WorkoutHistory, WorkoutDetail, SavedWorkouts & TemplateBuilder

## Goal
Implement WorkoutHistoryScreen (past workout list), WorkoutDetailScreen (full set breakdown), SavedWorkoutsScreen (templates list), and TemplateBuilderScreen.

## Phase
Features — Phase 5 of 6. Depends on KOTLIN-PLAN-09.

---

## Files to Create

### `feature/history/data/WorkoutHistoryRepository.kt`
```kotlin
@Singleton
class WorkoutHistoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val exerciseRepository: ExerciseRepository,
) {
    fun getWorkoutHistory(): Flow<List<WorkoutSummary>> = callbackFlow {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run { close(); return@callbackFlow }
        val listener = firestore.collection("users").document(uid)
            .collection("workouts")
            .orderBy("startedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val summaries = snap?.documents?.mapNotNull { doc ->
                    WorkoutSummary(
                        id = doc.id,
                        title = doc.getString("title") ?: "Workout",
                        workoutType = runCatching { WorkoutType.valueOf(doc.getString("workoutType") ?: "") }.getOrDefault(WorkoutType.CUSTOM),
                        startedAt = doc.getLong("startedAt") ?: 0L,
                        endedAt = doc.getLong("endedAt"),
                        totalXp = doc.getLong("totalXp")?.toInt() ?: 0,
                        exerciseCount = doc.getLong("exerciseCount")?.toInt() ?: 0,
                        totalSets = doc.getLong("totalSets")?.toInt() ?: 0,
                        totalVolumeKg = doc.getDouble("totalVolumeKg") ?: 0.0,
                        prCount = doc.getLong("prCount")?.toInt() ?: 0,
                    )
                } ?: emptyList()
                trySend(summaries)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getWorkoutDetail(workoutId: String): WorkoutDetailData? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val doc = firestore.collection("users").document(uid)
            .collection("workouts").document(workoutId).get().await()

        // Get exercises subcollection
        val exerciseDocs = firestore.collection("users").document(uid)
            .collection("workouts").document(workoutId)
            .collection("exercises").get().await()

        return WorkoutDetailData(
            id = doc.id,
            title = doc.getString("title") ?: "Workout",
            workoutType = runCatching { WorkoutType.valueOf(doc.getString("workoutType") ?: "") }.getOrDefault(WorkoutType.CUSTOM),
            startedAt = doc.getLong("startedAt") ?: 0L,
            endedAt = doc.getLong("endedAt"),
            totalXp = doc.getLong("totalXp")?.toInt() ?: 0,
            totalVolumeKg = doc.getDouble("totalVolumeKg") ?: 0.0,
            prCount = doc.getLong("prCount")?.toInt() ?: 0,
            exercises = emptyList(), // Populate from exerciseDocs if subcollection exists
        )
    }
}

data class WorkoutSummary(
    val id: String,
    val title: String,
    val workoutType: WorkoutType,
    val startedAt: Long,
    val endedAt: Long?,
    val totalXp: Int,
    val exerciseCount: Int,
    val totalSets: Int,
    val totalVolumeKg: Double,
    val prCount: Int,
)

data class WorkoutDetailData(
    val id: String,
    val title: String,
    val workoutType: WorkoutType,
    val startedAt: Long,
    val endedAt: Long?,
    val totalXp: Int,
    val totalVolumeKg: Double,
    val prCount: Int,
    val exercises: List<WorkoutExercise>,
)
```

### `feature/history/presentation/WorkoutHistoryViewModel.kt`
```kotlin
@HiltViewModel
class WorkoutHistoryViewModel @Inject constructor(
    private val historyRepository: WorkoutHistoryRepository,
) : ViewModel() {

    val workouts: StateFlow<List<WorkoutSummary>> = historyRepository.getWorkoutHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

### `feature/history/presentation/WorkoutHistoryScreen.kt`
```kotlin
@Composable
fun WorkoutHistoryScreen(
    onWorkoutSelected: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: WorkoutHistoryViewModel = hiltViewModel()
) {
    val workouts by viewModel.workouts.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout History") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (workouts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏋️", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(16.dp))
                    Text("No workouts yet", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                    Text("Complete your first workout to see history", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Group by month
                val grouped = workouts.groupBy { summary ->
                    val date = java.time.Instant.ofEpochMilli(summary.startedAt).atZone(java.time.ZoneId.systemDefault())
                    "${date.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())} ${date.year}"
                }

                grouped.forEach { (monthYear, monthWorkouts) ->
                    item {
                        Text(monthYear, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                    }
                    items(monthWorkouts, key = { it.id }) { summary ->
                        WorkoutSummaryCard(
                            summary = summary,
                            onClick = { onWorkoutSelected(summary.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutSummaryCard(summary: WorkoutSummary, onClick: () -> Unit) {
    val dateStr = remember(summary.startedAt) {
        val instant = java.time.Instant.ofEpochMilli(summary.startedAt)
        val local = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        "${local.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())}, ${local.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())} ${local.dayOfMonth}"
    }
    val durationStr = remember(summary.startedAt, summary.endedAt) {
        val secs = ((summary.endedAt ?: summary.startedAt) - summary.startedAt) / 1000
        "${secs / 60}m"
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(summary.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text("+${summary.totalXp} XP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallStat("${summary.exerciseCount}", "exercises")
                SmallStat("${summary.totalSets}", "sets")
                SmallStat(durationStr, "duration")
                if (summary.prCount > 0) SmallStat("${summary.prCount} 🏆", "PRs")
            }
        }
    }
}

@Composable
private fun SmallStat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
    }
}
```

### `feature/history/presentation/WorkoutDetailScreen.kt`
```kotlin
@Composable
fun WorkoutDetailScreen(
    workoutId: String,
    onBack: () -> Unit,
    viewModel: WorkoutDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(workoutId) { viewModel.loadDetail(workoutId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.workout?.title ?: "Workout Detail") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            uiState.workout?.let { detail ->
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary row
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatChipDetail("+${detail.totalXp}", "XP Earned", MaterialTheme.colorScheme.primary)
                            StatChipDetail("${(detail.totalVolumeKg / 1000).toInt()}t", "Volume", MaterialTheme.colorScheme.secondary)
                            StatChipDetail("${detail.prCount} 🏆", "PRs", GoldAccent)
                        }
                    }

                    if (detail.exercises.isEmpty()) {
                        item {
                            Text("Set details not available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    } else {
                        items(detail.exercises) { exercise ->
                            ExerciseHistoryCard(workoutExercise = exercise)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChipDetail(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

@Composable
fun ExerciseHistoryCard(workoutExercise: WorkoutExercise) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(workoutExercise.exercise.name, style = MaterialTheme.typography.titleSmall)
            workoutExercise.sets.forEachIndexed { i, set ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Set ${i + 1}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.width(48.dp))
                    Text(set.toDisplayString(), style = MaterialTheme.typography.bodyMedium)
                    if (set.isPersonalRecord) Text("🏆 PR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
```

### `feature/history/presentation/WorkoutDetailViewModel.kt`
```kotlin
@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    private val historyRepository: WorkoutHistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutDetailUiState())
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()

    fun loadDetail(workoutId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val detail = historyRepository.getWorkoutDetail(workoutId)
            _uiState.update { it.copy(isLoading = false, workout = detail) }
        }
    }
}

data class WorkoutDetailUiState(
    val isLoading: Boolean = false,
    val workout: WorkoutDetailData? = null,
)
```

### `feature/templates/presentation/SavedWorkoutsScreen.kt`
```kotlin
@Composable
fun SavedWorkoutsScreen(
    onCreateTemplate: () -> Unit,
    onTemplateSelected: (String) -> Unit,
    onStartFromTemplate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SavedWorkoutsViewModel = hiltViewModel()
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Workouts") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTemplate, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Create template")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (templates.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(16.dp))
                    Text("No saved workouts", style = MaterialTheme.typography.headlineSmall)
                    Text("Create a template to get started", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates, key = { it.id }) { template ->
                    TemplateCard(
                        template = template,
                        onEdit = { onTemplateSelected(template.id) },
                        onStart = { onStartFromTemplate(template.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun TemplateCard(template: WorkoutTemplate, onEdit: () -> Unit, onStart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(template.name, style = MaterialTheme.typography.titleSmall)
                Text("~${template.estimatedDurationMinutes}m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Text("${template.exercises.size} exercises • ${template.workoutType.displayName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onEdit) { Text("Edit") }
                Button(onClick = onStart) { Text("Start") }
            }
        }
    }
}
```

### `feature/templates/presentation/SavedWorkoutsViewModel.kt`
```kotlin
@HiltViewModel
class SavedWorkoutsViewModel @Inject constructor(
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val json: Json,
) : ViewModel() {

    val templates: StateFlow<List<WorkoutTemplate>> = flow {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@flow
        workoutTemplateDao.getAll(uid)
            .map { entities -> entities.map { it.toDomain(json) } }
            .collect { emit(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

### `feature/templates/presentation/TemplateBuilderScreen.kt`
```kotlin
@Composable
fun TemplateBuilderScreen(
    templateId: String? = null,
    onSaved: () -> Unit,
    onAddExercise: () -> Unit,
    onBack: () -> Unit,
    viewModel: TemplateBuilderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(templateId) { templateId?.let { viewModel.loadTemplate(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (templateId == null) "New Template" else "Edit Template") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Template Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            WorkoutTypeSelector(
                selected = uiState.workoutType,
                onSelected = viewModel::onWorkoutTypeChange,
            )

            // Exercise list
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.exercises, key = { it.id }) { exercise ->
                    ExerciseTemplateRow(exerciseName = exercise.exercise.name, onRemove = { viewModel.removeExercise(exercise.id) })
                }
                item {
                    TextButton(onClick = onAddExercise, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Exercise")
                    }
                }
            }

            Button(
                onClick = { viewModel.save(); onSaved() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = uiState.name.isNotBlank() && uiState.exercises.isNotEmpty()
            ) {
                Text("Save Template")
            }
        }
    }
}
```

---

## Verification
1. WorkoutHistoryScreen shows past workouts grouped by month
2. Tap workout → WorkoutDetailScreen opens with stats
3. SavedWorkoutsScreen shows empty state with create button
4. Create template → appears in list; tap Start → launches ActiveWorkoutScreen with exercises pre-loaded
