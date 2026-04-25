# KOTLIN-PLAN-08.md — ActiveWorkoutScreen & WorkoutPersistenceManager

## Goal
Implement the active workout session screen with all 12 ExerciseCounterType input forms, real-time set logging, rest timer, and WorkoutPersistenceManager that writes every set to Room immediately for crash recovery.

## Phase
Features — Phase 4 of 6. Depends on KOTLIN-PLAN-02, KOTLIN-PLAN-07.

---

## Key Strings from libapp.so (crash recovery)
- Storage key: `active_workout_session`
- Timestamp key: `workout_last_save_timestamp`
- Dialog: `ResumeWorkoutDialog`

---

## Files to Create

### `feature/workout/data/WorkoutPersistenceManager.kt`
```kotlin
package com.example.gymlevels.feature.workout.data

import com.example.gymlevels.core.data.local.dao.ActiveWorkoutDao
import com.example.gymlevels.core.data.local.entity.*
import com.example.gymlevels.core.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutPersistenceManager @Inject constructor(
    private val activeWorkoutDao: ActiveWorkoutDao,
) {
    // Called once when user taps "Start Workout"
    suspend fun startSession(session: WorkoutSession) {
        activeWorkoutDao.insertSession(session.toSessionEntity())
    }

    // Called when an exercise is added to the workout
    suspend fun addExercise(exercise: WorkoutExercise) {
        activeWorkoutDao.insertExercise(exercise.toExerciseEntity())
    }

    // Called every time a set is logged — this is the crash recovery write
    suspend fun persistSet(set: WorkoutSet) {
        activeWorkoutDao.insertSet(set.toSetEntity())
    }

    suspend fun updateSet(set: WorkoutSet) {
        activeWorkoutDao.updateSet(set.toSetEntity())
    }

    suspend fun deleteSet(setId: String) {
        activeWorkoutDao.deleteSet(setId)
    }

    // Called after workout is saved to Firestore — clears local crash recovery state
    suspend fun clearSession() {
        activeWorkoutDao.clearAllSessions()
    }

    // Check if there's a crash-recovered session
    suspend fun getResumableSession(): ActiveWorkoutSessionWithExercises? =
        activeWorkoutDao.getActiveSession()
}

private fun WorkoutSession.toSessionEntity() = ActiveWorkoutSessionEntity(
    id = id, userId = userId, title = title,
    workoutType = workoutType.name, startedAt = startedAt,
    notes = notes, templateId = templateId,
)

private fun WorkoutExercise.toExerciseEntity() = ActiveWorkoutExerciseEntity(
    id = id, sessionId = sessionId,
    exerciseId = exercise.id, exerciseName = exercise.name,
    orderIndex = orderIndex, notes = notes,
)

private fun WorkoutSet.toSetEntity() = ActiveWorkoutSetEntity(
    id = id, workoutExerciseId = workoutExerciseId,
    setNumber = setNumber, reps = reps, weightKg = weightKg,
    durationSeconds = durationSeconds, distanceKm = distanceKm,
    inclinePercent = inclinePercent, resistanceLevel = resistanceLevel,
    floors = floors, steps = steps, bandResistance = bandResistance,
    isPersonalRecord = isPersonalRecord, completedAt = completedAt,
)
```

### `feature/workout/presentation/ActiveWorkoutViewModel.kt`
```kotlin
@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val persistenceManager: WorkoutPersistenceManager,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val _session = MutableStateFlow<WorkoutSession?>(null)
    val session: StateFlow<WorkoutSession?> = _session.asStateFlow()

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds: StateFlow<Int> = _restTimerSeconds.asStateFlow()

    private var restTimerJob: Job? = null
    private val _hasResumableSession = MutableStateFlow(false)
    val hasResumableSession: StateFlow<Boolean> = _hasResumableSession.asStateFlow()

    init {
        viewModelScope.launch { checkForResumableSession() }
    }

    private suspend fun checkForResumableSession() {
        val saved = persistenceManager.getResumableSession()
        if (saved != null) _hasResumableSession.value = true
    }

    fun startNewWorkout(title: String, workoutType: WorkoutType) {
        val session = WorkoutSession(
            id = UUID.randomUUID().toString(),
            userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            title = title,
            workoutType = workoutType,
            startedAt = System.currentTimeMillis(),
        )
        _session.value = session
        viewModelScope.launch { persistenceManager.startSession(session) }
    }

    fun addExercise(exercise: Exercise) {
        val current = _session.value ?: return
        val workoutExercise = WorkoutExercise(
            id = UUID.randomUUID().toString(),
            sessionId = current.id,
            exercise = exercise,
            orderIndex = current.exercises.size,
        )
        _session.update { it?.copy(exercises = it.exercises + workoutExercise) }
        viewModelScope.launch { persistenceManager.addExercise(workoutExercise) }
    }

    fun logSet(workoutExerciseId: String, setData: WorkoutSetData) {
        val current = _session.value ?: return
        val exerciseIndex = current.exercises.indexOfFirst { it.id == workoutExerciseId }
        if (exerciseIndex < 0) return

        val exercise = current.exercises[exerciseIndex]
        val isPR = detectPersonalRecord(exercise, setData)
        val set = setData.toWorkoutSet(
            id = UUID.randomUUID().toString(),
            workoutExerciseId = workoutExerciseId,
            setNumber = exercise.sets.size + 1,
            isPersonalRecord = isPR,
        )

        val updatedExercises = current.exercises.toMutableList().also {
            it[exerciseIndex] = exercise.copy(sets = exercise.sets + set)
        }
        _session.update { it?.copy(exercises = updatedExercises) }

        // Persist immediately — crash recovery
        viewModelScope.launch { persistenceManager.persistSet(set) }

        if (isPR) _uiState.update { it.copy(newPR = set) }

        startRestTimer(setData.recommendedRestSeconds)
    }

    fun deleteSet(workoutExerciseId: String, setId: String) {
        val current = _session.value ?: return
        val exerciseIndex = current.exercises.indexOfFirst { it.id == workoutExerciseId }
        if (exerciseIndex < 0) return
        val exercise = current.exercises[exerciseIndex]
        val updatedExercises = current.exercises.toMutableList().also {
            it[exerciseIndex] = exercise.copy(sets = exercise.sets.filter { s -> s.id != setId })
        }
        _session.update { it?.copy(exercises = updatedExercises) }
        viewModelScope.launch { persistenceManager.deleteSet(setId) }
    }

    private fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        _restTimerSeconds.value = seconds
        restTimerJob = viewModelScope.launch {
            while (_restTimerSeconds.value > 0) {
                delay(1000)
                _restTimerSeconds.update { it - 1 }
            }
        }
    }

    fun skipRestTimer() {
        restTimerJob?.cancel()
        _restTimerSeconds.value = 0
    }

    fun discardWorkout() {
        viewModelScope.launch { persistenceManager.clearSession() }
        _session.value = null
    }

    private fun detectPersonalRecord(exercise: WorkoutExercise, setData: WorkoutSetData): Boolean {
        // Simple PR detection: highest weight × reps combo
        val newVolume = (setData.reps ?: 0) * (setData.weightKg ?: 0.0)
        val existingMax = exercise.sets.maxOfOrNull { ((it.reps ?: 0) * (it.weightKg ?: 0.0)) } ?: 0.0
        return newVolume > 0 && newVolume > existingMax
    }
}

data class ActiveWorkoutUiState(
    val newPR: WorkoutSet? = null,
    val error: String? = null,
)

// Sealed class for each counter type's input data
sealed class WorkoutSetData {
    abstract val recommendedRestSeconds: Int

    data class RepsAndWeight(val reps: Int, val weightKg: Double, override val recommendedRestSeconds: Int = 90) : WorkoutSetData()
    data class RepsOnly(val reps: Int, override val recommendedRestSeconds: Int = 60) : WorkoutSetData()
    data class Bodyweight(val reps: Int, override val recommendedRestSeconds: Int = 60) : WorkoutSetData()
    data class TimeOnly(val durationSeconds: Int, override val recommendedRestSeconds: Int = 60) : WorkoutSetData()
    data class TimeAndDistanceAndIncline(val durationSeconds: Int, val distanceKm: Double, val inclinePercent: Double, override val recommendedRestSeconds: Int = 120) : WorkoutSetData()
    data class TimeAndDistanceAndResistance(val durationSeconds: Int, val distanceKm: Double, val resistanceLevel: Int, override val recommendedRestSeconds: Int = 120) : WorkoutSetData()
    data class TimeAndFloorsAndSteps(val durationSeconds: Int, val floors: Int, val steps: Int, override val recommendedRestSeconds: Int = 90) : WorkoutSetData()
    data class ResistanceBand(val reps: Int, val bandResistance: String, override val recommendedRestSeconds: Int = 60) : WorkoutSetData()
    data class ResistanceBandTime(val durationSeconds: Int, val bandResistance: String, override val recommendedRestSeconds: Int = 60) : WorkoutSetData()

    fun toWorkoutSet(id: String, workoutExerciseId: String, setNumber: Int, isPersonalRecord: Boolean): WorkoutSet = when (this) {
        is RepsAndWeight -> WorkoutSet(id, workoutExerciseId, setNumber, reps = reps, weightKg = weightKg, isPersonalRecord = isPersonalRecord)
        is RepsOnly -> WorkoutSet(id, workoutExerciseId, setNumber, reps = reps, isPersonalRecord = isPersonalRecord)
        is Bodyweight -> WorkoutSet(id, workoutExerciseId, setNumber, reps = reps, isPersonalRecord = isPersonalRecord)
        is TimeOnly -> WorkoutSet(id, workoutExerciseId, setNumber, durationSeconds = durationSeconds, isPersonalRecord = isPersonalRecord)
        is TimeAndDistanceAndIncline -> WorkoutSet(id, workoutExerciseId, setNumber, durationSeconds = durationSeconds, distanceKm = distanceKm, inclinePercent = inclinePercent)
        is TimeAndDistanceAndResistance -> WorkoutSet(id, workoutExerciseId, setNumber, durationSeconds = durationSeconds, distanceKm = distanceKm, resistanceLevel = resistanceLevel)
        is TimeAndFloorsAndSteps -> WorkoutSet(id, workoutExerciseId, setNumber, durationSeconds = durationSeconds, floors = floors, steps = steps)
        is ResistanceBand -> WorkoutSet(id, workoutExerciseId, setNumber, reps = reps, bandResistance = bandResistance)
        is ResistanceBandTime -> WorkoutSet(id, workoutExerciseId, setNumber, durationSeconds = durationSeconds, bandResistance = bandResistance)
    }
}
```

### `feature/workout/presentation/ActiveWorkoutScreen.kt`
```kotlin
@Composable
fun ActiveWorkoutScreen(
    onFinishWorkout: (WorkoutSession) -> Unit,
    onAddExercise: (sessionId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: ActiveWorkoutViewModel = hiltViewModel()
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val restTimer by viewModel.restTimerSeconds.collectAsStateWithLifecycle()
    val hasResumable by viewModel.hasResumableSession.collectAsStateWithLifecycle()
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showWorkoutTypeSheet by remember { mutableStateOf(session == null) }

    // Resume dialog
    if (hasResumable) {
        ResumeWorkoutDialog(
            onResume = { /* Load saved session from Room */ },
            onDiscard = { viewModel.discardWorkout() }
        )
    }

    // Workout type picker bottom sheet
    if (showWorkoutTypeSheet) {
        WorkoutTypePickerSheet(
            onSelected = { type ->
                viewModel.startNewWorkout(type.displayName, type)
                showWorkoutTypeSheet = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session?.title ?: "New Workout", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { showDiscardDialog = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    TextButton(onClick = { session?.let { onFinishWorkout(it) } }, enabled = session != null) {
                        Text("Finish", color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Rest timer banner
            AnimatedVisibility(visible = restTimer > 0) {
                RestTimerBanner(seconds = restTimer, onSkip = viewModel::skipRestTimer)
            }

            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                session?.exercises?.forEach { exercise ->
                    item(key = exercise.id) {
                        WorkoutExerciseCard(
                            workoutExercise = exercise,
                            onLogSet = { setData -> viewModel.logSet(exercise.id, setData) },
                            onDeleteSet = { setId -> viewModel.deleteSet(exercise.id, setId) },
                        )
                    }
                }

                item {
                    TextButton(
                        onClick = { session?.let { onAddExercise(it.id) } },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Exercise")
                    }
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Workout?") },
            text = { Text("All sets will be lost.") },
            confirmButton = { TextButton(onClick = { viewModel.discardWorkout(); onBack() }) { Text("Discard", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ResumeWorkoutDialog(onResume: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Resume Workout?") },
        text = { Text("You have an unfinished workout. Would you like to continue?") },
        confirmButton = { Button(onClick = onResume) { Text("Resume") } },
        dismissButton = { OutlinedButton(onClick = onDiscard) { Text("Start New") } }
    )
}

@Composable
fun RestTimerBanner(seconds: Int, onSkip: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Rest: ${seconds}s", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            TextButton(onClick = onSkip) { Text("Skip") }
        }
    }
}

@Composable
fun WorkoutExerciseCard(
    workoutExercise: WorkoutExercise,
    onLogSet: (WorkoutSetData) -> Unit,
    onDeleteSet: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(workoutExercise.exercise.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)

            // Show logged sets
            workoutExercise.sets.forEachIndexed { index, set ->
                LoggedSetRow(setNumber = index + 1, set = set, onDelete = { onDeleteSet(set.id) })
            }

            // Input form based on counter type
            SetInputForm(
                counterType = workoutExercise.exercise.counterType(),
                setNumber = workoutExercise.sets.size + 1,
                onLogSet = onLogSet
            )
        }
    }
}

@Composable
fun LoggedSetRow(setNumber: Int, set: WorkoutSet, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Set $setNumber", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.width(48.dp))
        Text(set.toDisplayString(), style = MaterialTheme.typography.bodyMedium, color = if (set.isPersonalRecord) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        if (set.isPersonalRecord) Text("🏆 PR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        }
    }
}

private fun WorkoutSet.toDisplayString(): String = buildString {
    reps?.let { append("${it} reps") }
    weightKg?.let { if (it > 0) append(" × ${it}kg") }
    durationSeconds?.let { append("${it}s") }
    distanceKm?.let { if (it > 0) append(" ${it}km") }
    floors?.let { append("${it} floors") }
    steps?.let { append(" ${it} steps") }
    bandResistance?.let { append(it) }
}

@Composable
fun SetInputForm(counterType: ExerciseCounterType, setNumber: Int, onLogSet: (WorkoutSetData) -> Unit) {
    when (counterType) {
        ExerciseCounterType.REPS_AND_WEIGHT -> RepsAndWeightForm(setNumber, onLogSet)
        ExerciseCounterType.REPS, ExerciseCounterType.REPS_ONLY -> RepsOnlyForm(setNumber, onLogSet)
        ExerciseCounterType.BODYWEIGHT -> BodyweightForm(setNumber, onLogSet)
        ExerciseCounterType.TIME, ExerciseCounterType.TIME_ONLY, ExerciseCounterType.TIME_AND_SETS -> TimeForm(setNumber, onLogSet)
        ExerciseCounterType.TIME_AND_DISTANCE_AND_INCLINE -> TimeDistanceInclineForm(setNumber, onLogSet)
        ExerciseCounterType.TIME_AND_DISTANCE_AND_RESISTANCE -> TimeDistanceResistanceForm(setNumber, onLogSet)
        ExerciseCounterType.TIME_AND_FLOORS_AND_STEPS -> TimeFloorsStepsForm(setNumber, onLogSet)
        ExerciseCounterType.RESISTANCE_BAND_STRENGTH -> ResistanceBandForm(setNumber, onLogSet)
        ExerciseCounterType.RESISTANCE_BAND_STRENGTH_AND_TIME -> ResistanceBandTimeForm(setNumber, onLogSet)
    }
}

@Composable
private fun RepsAndWeightForm(setNumber: Int, onLog: (WorkoutSetData) -> Unit) {
    var reps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Set $setNumber", Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(reps, { reps = it }, label = { Text("Reps") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
        OutlinedTextField(weight, { weight = it }, label = { Text("kg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
        IconButton(onClick = {
            val r = reps.toIntOrNull() ?: return@IconButton
            val w = weight.toDoubleOrNull() ?: 0.0
            onLog(WorkoutSetData.RepsAndWeight(r, w))
            reps = ""; weight = ""
        }) { Icon(Icons.Default.Check, contentDescription = "Log set", tint = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun RepsOnlyForm(setNumber: Int, onLog: (WorkoutSetData) -> Unit) {
    var reps by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Set $setNumber", Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(reps, { reps = it }, label = { Text("Reps") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
        IconButton(onClick = {
            val r = reps.toIntOrNull() ?: return@IconButton
            onLog(WorkoutSetData.RepsOnly(r))
            reps = ""
        }) { Icon(Icons.Default.Check, contentDescription = "Log set", tint = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun BodyweightForm(setNumber: Int, onLog: (WorkoutSetData) -> Unit) {
    var reps by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Set $setNumber", Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(reps, { reps = it }, label = { Text("Reps") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
        IconButton(onClick = {
            val r = reps.toIntOrNull() ?: return@IconButton
            onLog(WorkoutSetData.Bodyweight(r))
            reps = ""
        }) { Icon(Icons.Default.Check, contentDescription = "Log set", tint = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun TimeForm(setNumber: Int, onLog: (WorkoutSetData) -> Unit) {
    var duration by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Set $setNumber", Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(duration, { duration = it }, label = { Text("Seconds") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
        IconButton(onClick = {
            val d = duration.toIntOrNull() ?: return@IconButton
            onLog(WorkoutSetData.TimeOnly(d))
            duration = ""
        }) { Icon(Icons.Default.Check, contentDescription = "Log set", tint = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun TimeDistanceInclineForm(setNumber: Int, onLog: (WorkoutSetData) -> Unit) {
    var duration by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var incline by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Set $setNumber", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(duration, { duration = it }, label = { Text("Sec") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(distance, { distance = it }, label = { Text("km") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(incline, { incline = it }, label = { Text("%") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            IconButton(onClick = {
                val d = duration.toIntOrNull() ?: return@IconButton
                onLog(WorkoutSetData.TimeAndDistanceAndIncline(d, distance.toDoubleOrNull() ?: 0.0, incline.toDoubleOrNull() ?: 0.0))
                duration = ""; distance = ""; incline = ""
            }) { Icon(Icons.Default.Check, contentDescription = "Log", tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun TimeDistanceResistanceForm(setNumber: Int, onLog: (WorkoutSetData) -> Unit) {
    var duration by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var resistance by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Set $setNumber", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(duration, { duration = it }, label = { Text("Sec") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(distance, { distance = it }, label = { Text("km") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(resistance, { resistance = it }, label = { Text("Lvl") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            IconButton(onClick = {
                val d = duration.toIntOrNull() ?: return@IconButton
                onLog(WorkoutSetData.TimeAndDistanceAndResistance(d, distance.toDoubleOrNull() ?: 0.0, resistance.toIntOrNull() ?: 1))
                duration = ""; distance = ""; resistance = ""
            }) { Icon(Icons.Default.Check, contentDescription = "Log", tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun TimeFloorsStepsForm(setNumber: Int, onLog: (WorkoutSetData) -> Unit) {
    var duration by remember { mutableStateOf("") }
    var floors by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Set $setNumber", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(duration, { duration = it }, label = { Text("Sec") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(floors, { floors = it }, label = { Text("Floors") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(steps, { steps = it }, label = { Text("Steps") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            IconButton(onClick = {
                val d = duration.toIntOrNull() ?: return@IconButton
                onLog(WorkoutSetData.TimeAndFloorsAndSteps(d, floors.toIntOrNull() ?: 0, steps.toIntOrNull() ?: 0))
                duration = ""; floors = ""; steps = ""
            }) { Icon(Icons.Default.Check, contentDescription = "Log", tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun ResistanceBandForm(setNumber: Int, onLog: (WorkoutSetData) -> Unit) {
    val bandOptions = listOf("Light", "Medium", "Heavy", "X-Heavy")
    var reps by remember { mutableStateOf("") }
    var band by remember { mutableStateOf(bandOptions[1]) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Set $setNumber", Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(reps, { reps = it }, label = { Text("Reps") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        ExposedDropdownMenuBox(expanded = false, onExpandedChange = {}) {
            OutlinedTextField(band, {}, readOnly = true, label = { Text("Band") }, modifier = Modifier.weight(1f).menuAnchor(), singleLine = true)
        }
        IconButton(onClick = {
            val r = reps.toIntOrNull() ?: return@IconButton
            onLog(WorkoutSetData.ResistanceBand(r, band))
            reps = ""
        }) { Icon(Icons.Default.Check, contentDescription = "Log", tint = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun ResistanceBandTimeForm(setNumber: Int, onLog: (WorkoutSetData) -> Unit) {
    val bandOptions = listOf("Light", "Medium", "Heavy", "X-Heavy")
    var duration by remember { mutableStateOf("") }
    var band by remember { mutableStateOf(bandOptions[1]) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Set $setNumber", Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(duration, { duration = it }, label = { Text("Sec") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Text(band, Modifier.weight(1f).padding(8.dp), style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = {
            val d = duration.toIntOrNull() ?: return@IconButton
            onLog(WorkoutSetData.ResistanceBandTime(d, band))
            duration = ""
        }) { Icon(Icons.Default.Check, contentDescription = "Log", tint = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun WorkoutTypePickerSheet(onSelected: (WorkoutType) -> Unit) {
    var showSheet by remember { mutableStateOf(true) }
    if (!showSheet) return

    ModalBottomSheet(onDismissRequest = { showSheet = false }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Select Workout Type", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            WorkoutType.entries.forEach { type ->
                Card(
                    onClick = { onSelected(type) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(type.displayName, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
```

---

## Verification
1. Tap "Start Workout" → WorkoutTypePickerSheet appears
2. Select type → session starts, ExercisePicker opens
3. Select exercise → WorkoutExerciseCard appears with correct input form per counter type
4. Log a set → set appears in card, Room `active_workout_sets` has the row (verify in DB Inspector)
5. Force-kill app → relaunch → ResumeWorkoutDialog appears
6. Rest timer counts down after each set, skip button works
7. Discard workout → Room tables cleared, back to Home
