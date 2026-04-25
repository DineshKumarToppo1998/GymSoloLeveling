# KOTLIN-PLAN-17.md — Custom Exercises with YouTube Picker

## Goal
Implement CustomExercisesListScreen, CustomExerciseFormScreen (create/edit custom exercises), and YoutubePickerScreen using android-youtube-player to preview and select a YouTube video ID.

## Phase
Features — Phase 5 of 6. Depends on KOTLIN-PLAN-07.

---

## Files to Create

### `feature/exercise/presentation/CustomExercisesListScreen.kt`
```kotlin
@Composable
fun CustomExercisesListScreen(
    onCreateExercise: () -> Unit,
    onEditExercise: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: CustomExercisesViewModel = hiltViewModel()
) {
    val exercises by viewModel.customExercises.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Exercises") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateExercise, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Create exercise")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (exercises.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏋️", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(16.dp))
                    Text("No custom exercises", style = MaterialTheme.typography.headlineSmall)
                    Text("Create your own exercises to add to workouts", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(exercises, key = { it.id }) { exercise ->
                    CustomExerciseCard(
                        exercise = exercise,
                        onEdit = { onEditExercise(exercise.id) },
                        onDelete = { viewModel.deleteExercise(exercise) }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomExerciseCard(exercise: Exercise, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Exercise?") },
            text = { Text("\"${exercise.name}\" will be permanently deleted.") },
            confirmButton = { TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${exercise.primaryMuscles.take(2).joinToString(", ")} • ${exercise.mainEquipment}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                if (exercise.youtubeVideoId != null) {
                    Text("📹 Video attached", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)) }
        }
    }
}
```

### `feature/exercise/presentation/CustomExercisesViewModel.kt`
```kotlin
@HiltViewModel
class CustomExercisesViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    val customExercises: StateFlow<List<Exercise>> = exerciseRepository.getCustomExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch { exerciseRepository.deleteCustomExercise(exercise) }
    }
}
```

### `feature/exercise/presentation/CustomExerciseFormScreen.kt`
```kotlin
@Composable
fun CustomExerciseFormScreen(
    exerciseId: String? = null,
    onSaved: () -> Unit,
    onPickYoutube: (currentVideoId: String?) -> Unit,
    onBack: () -> Unit,
    viewModel: CustomExerciseFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(exerciseId) { exerciseId?.let { viewModel.loadExercise(it) } }
    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onSaved() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (exerciseId == null) "New Exercise" else "Edit Exercise") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp, bottom = 32.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Exercise Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )
            }

            // Counter type selector
            item {
                Text("Counter Type", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ExerciseCounterType.entries) { type ->
                        FilterChip(
                            selected = uiState.counterType == type,
                            onClick = { viewModel.onCounterTypeChange(type) },
                            label = { Text(type.name.lowercase().replace("_", " ")) }
                        )
                    }
                }
            }

            // Primary muscles
            item {
                Text("Primary Muscles", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MuscleGroup.entries) { muscle ->
                        FilterChip(
                            selected = muscle.name in uiState.primaryMuscles,
                            onClick = { viewModel.togglePrimaryMuscle(muscle.name) },
                            label = { Text(muscle.displayName) }
                        )
                    }
                }
            }

            // Secondary muscles
            item {
                Text("Secondary Muscles", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MuscleGroup.entries) { muscle ->
                        FilterChip(
                            selected = muscle.name in uiState.secondaryMuscles,
                            onClick = { viewModel.toggleSecondaryMuscle(muscle.name) },
                            label = { Text(muscle.displayName) }
                        )
                    }
                }
            }

            // Equipment
            item {
                ExposedDropdownMenuBox(
                    expanded = uiState.equipmentDropdownExpanded,
                    onExpandedChange = viewModel::setEquipmentDropdownExpanded
                ) {
                    OutlinedTextField(
                        value = uiState.mainEquipment,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Equipment") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.equipmentDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = uiState.equipmentDropdownExpanded, onDismissRequest = { viewModel.setEquipmentDropdownExpanded(false) }) {
                        listOf("barbell", "dumbbell", "cable", "machine", "bodyweight", "kettlebell", "resistance_band", "smith_machine").forEach { equipment ->
                            DropdownMenuItem(text = { Text(equipment.replace("_", " ").replaceFirstChar { it.uppercase() }) }, onClick = { viewModel.onEquipmentChange(equipment); viewModel.setEquipmentDropdownExpanded(false) })
                        }
                    }
                }
            }

            // Difficulty
            item {
                Text("Difficulty: ${uiState.difficulty}", style = MaterialTheme.typography.titleSmall)
                Slider(value = uiState.difficulty.toFloat(), onValueChange = { viewModel.onDifficultyChange(it.toInt()) }, valueRange = 1f..3f, steps = 1)
            }

            // Instructions
            item {
                OutlinedTextField(
                    value = uiState.instructions,
                    onValueChange = viewModel::onInstructionsChange,
                    label = { Text("Instructions") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                )
            }

            // YouTube video
            item {
                Card(
                    onClick = { onPickYoutube(uiState.youtubeVideoId) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = if (uiState.youtubeVideoId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (uiState.youtubeVideoId != null) "Video Attached" else "Add YouTube Video", style = MaterialTheme.typography.bodyMedium)
                            uiState.youtubeVideoId?.let { id -> Text(id, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }

            item {
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = uiState.name.isNotBlank() && uiState.primaryMuscles.isNotEmpty()
                ) {
                    Text("Save Exercise")
                }
            }
        }
    }
}
```

### `feature/exercise/presentation/CustomExerciseFormViewModel.kt`
```kotlin
@HiltViewModel
class CustomExerciseFormViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomExerciseFormUiState())
    val uiState: StateFlow<CustomExerciseFormUiState> = _uiState.asStateFlow()

    fun loadExercise(id: String) {
        viewModelScope.launch {
            val exercise = exerciseRepository.getById(id) ?: return@launch
            _uiState.update {
                it.copy(
                    name = exercise.name,
                    counterType = exercise.counterType(),
                    primaryMuscles = exercise.primaryMuscles.toMutableSet(),
                    secondaryMuscles = exercise.secondaryMuscles.toMutableSet(),
                    mainEquipment = exercise.mainEquipment,
                    difficulty = exercise.difficulty,
                    instructions = exercise.instructions.joinToString("\n"),
                    youtubeVideoId = exercise.youtubeVideoId,
                    editingId = id,
                )
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, nameError = null) }
    fun onCounterTypeChange(type: ExerciseCounterType) = _uiState.update { it.copy(counterType = type) }
    fun togglePrimaryMuscle(muscle: String) = _uiState.update { it.copy(primaryMuscles = it.primaryMuscles.toggle(muscle)) }
    fun toggleSecondaryMuscle(muscle: String) = _uiState.update { it.copy(secondaryMuscles = it.secondaryMuscles.toggle(muscle)) }
    fun onEquipmentChange(eq: String) = _uiState.update { it.copy(mainEquipment = eq) }
    fun onDifficultyChange(d: Int) = _uiState.update { it.copy(difficulty = d) }
    fun onInstructionsChange(text: String) = _uiState.update { it.copy(instructions = text) }
    fun setYoutubeVideoId(id: String?) = _uiState.update { it.copy(youtubeVideoId = id) }
    fun setEquipmentDropdownExpanded(expanded: Boolean) = _uiState.update { it.copy(equipmentDropdownExpanded = expanded) }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) { _uiState.update { it.copy(nameError = "Name required") }; return }
        viewModelScope.launch {
            val exercise = Exercise(
                id = state.editingId ?: java.util.UUID.randomUUID().toString(),
                name = state.name,
                mainEquipment = state.mainEquipment,
                otherEquipment = emptyList(),
                primaryMuscles = state.primaryMuscles.toList(),
                secondaryMuscles = state.secondaryMuscles.toList(),
                splitCategories = emptyList(),
                exerciseCounterType = state.counterType.name.lowercase(),
                exerciseMechanics = "compound",
                difficulty = state.difficulty,
                instructions = state.instructions.lines().filter { it.isNotBlank() },
                tips = emptyList(), benefits = emptyList(), breathingInstructions = "",
                keywords = listOf(state.name.lowercase()),
                metabolicEquivalent = 4.0,
                repSupplement = null,
                isCustom = true,
                youtubeVideoId = state.youtubeVideoId,
            )
            exerciseRepository.saveCustomExercise(exercise)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    private fun Set<String>.toggle(item: String): MutableSet<String> =
        toMutableSet().also { if (item in it) it.remove(item) else it.add(item) }
}

data class CustomExerciseFormUiState(
    val name: String = "",
    val nameError: String? = null,
    val counterType: ExerciseCounterType = ExerciseCounterType.REPS_AND_WEIGHT,
    val primaryMuscles: Set<String> = emptySet(),
    val secondaryMuscles: Set<String> = emptySet(),
    val mainEquipment: String = "dumbbell",
    val difficulty: Int = 1,
    val instructions: String = "",
    val youtubeVideoId: String? = null,
    val equipmentDropdownExpanded: Boolean = false,
    val editingId: String? = null,
    val isSaved: Boolean = false,
)
```

### `feature/exercise/presentation/YoutubePickerScreen.kt`
```kotlin
@Composable
fun YoutubePickerScreen(
    currentVideoId: String? = null,
    onVideoSelected: (String) -> Unit,
    onBack: () -> Unit,
) {
    var videoIdInput by remember { mutableStateOf(currentVideoId ?: "") }
    var previewVideoId by remember { mutableStateOf(currentVideoId) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Exercise Video") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Paste a YouTube video ID or full URL to preview the exercise demonstration.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = videoIdInput,
                    onValueChange = { videoIdInput = it },
                    label = { Text("YouTube Video ID or URL") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("e.g. dQw4w9WgXcQ") }
                )
                Button(onClick = {
                    val parsed = parseYoutubeId(videoIdInput)
                    if (parsed != null) previewVideoId = parsed
                }) {
                    Text("Preview")
                }
            }

            // YouTube player preview
            previewVideoId?.let { videoId ->
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            YouTubePlayerView(ctx).apply {
                                addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                                    override fun onReady(youTubePlayer: YouTubePlayer) {
                                        youTubePlayer.cueVideo(videoId, 0f)
                                    }
                                })
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val parsed = parseYoutubeId(videoIdInput)
                    if (parsed != null) onVideoSelected(parsed)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = videoIdInput.isNotBlank()
            ) {
                Text("Use This Video")
            }

            if (currentVideoId != null) {
                OutlinedButton(
                    onClick = { onVideoSelected("") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove Video")
                }
            }
        }
    }
}

private fun parseYoutubeId(input: String): String? {
    if (input.isBlank()) return null
    // Full URL: https://www.youtube.com/watch?v=VIDEO_ID or https://youtu.be/VIDEO_ID
    val urlPattern = Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/)([A-Za-z0-9_-]{11})")
    val match = urlPattern.find(input)
    if (match != null) return match.groupValues[1]
    // Direct 11-char ID
    if (input.length == 11 && input.matches(Regex("[A-Za-z0-9_-]+"))) return input
    return null
}
```

---

## Dependencies needed in `app/build.gradle.kts`
The `android-youtube-player` library is already declared in PLAN-01's `build.gradle.kts`:
```kotlin
implementation(libs.android.youtube.player) // com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0
```

Import in YoutubePickerScreen:
```kotlin
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.ui.YouTubePlayerView
```

---

## Verification
1. CustomExercisesListScreen shows empty state with FAB
2. Create exercise → fill name, select muscles, select counter type → Save → appears in list
3. Edit exercise → form pre-fills with existing data → Save updates in Room
4. YoutubePickerScreen: paste full YouTube URL → player shows video preview
5. Select video → returns to form with video ID displayed
6. Custom exercise appears in ExercisePicker (ExerciseDao.getCustom() + combined in search)
