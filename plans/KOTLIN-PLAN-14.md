# KOTLIN-PLAN-14.md — TodaysWorkoutService & WeeklyReportScreen

## Goal
Implement TodaysWorkoutService (personalized workout recommendations using recovery data + user preferences) and WeeklyReportScreen (7-day stats summary).

## Phase
Features — Phase 5 of 6. Depends on KOTLIN-PLAN-12, KOTLIN-PLAN-13.

---

## Notes
- TodaysWorkoutService confirmed in libapp.so as `TodaysWorkoutService`
- Uses recovery state, priority muscles, available equipment, preferred workout days
- Generates a suggested workout structure (type + exercise selection), not a fixed template

---

## Files to Create

### `core/service/TodaysWorkoutService.kt`
```kotlin
package com.example.gymlevels.core.service

import com.example.gymlevels.core.domain.model.*
import com.example.gymlevels.core.domain.model.enums.MuscleGroup
import com.example.gymlevels.core.domain.model.enums.WorkoutType
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodaysWorkoutService @Inject constructor(
    private val recoveryService: RecoveryService,
) {
    data class WorkoutRecommendation(
        val workoutType: WorkoutType,
        val targetMuscles: List<MuscleGroup>,
        val suggestedExerciseIds: List<String>,
        val estimatedDurationMinutes: Int,
        val reason: String,
    )

    fun getRecommendation(
        muscleRanks: List<MuscleRank>,
        lastTrainedDates: Map<MuscleGroup, Long>,
        priorityMuscles: List<MuscleGroup>,
        availableEquipment: List<String>,
        preferredDays: List<DayOfWeek>,
        today: LocalDate = LocalDate.now(),
    ): WorkoutRecommendation? {
        val todayDow = today.dayOfWeek
        val isPreferredDay = preferredDays.isEmpty() || todayDow in preferredDays

        if (!isPreferredDay) return null

        val recoveries = recoveryService.calculateRecovery(lastTrainedDates)
        val readyMuscles = recoveries.filter { it.isReady }.map { it.muscleGroup }

        // Prioritize: user priority muscles that are also ready
        val priorityReady = priorityMuscles.filter { it in readyMuscles }
        val targetMuscles = if (priorityReady.isNotEmpty()) {
            priorityReady.take(4)
        } else {
            // Pick lowest-ranked ready muscles (they need work most)
            readyMuscles
                .sortedBy { muscle -> muscleRanks.firstOrNull { it.muscleGroup == muscle }?.totalXp ?: 0 }
                .take(4)
        }

        if (targetMuscles.isEmpty()) {
            return WorkoutRecommendation(
                workoutType = WorkoutType.FULL_BODY,
                targetMuscles = MuscleGroup.entries.take(4),
                suggestedExerciseIds = emptyList(),
                estimatedDurationMinutes = 30,
                reason = "Active recovery day — light movement recommended",
            )
        }

        val workoutType = inferWorkoutType(targetMuscles)
        return WorkoutRecommendation(
            workoutType = workoutType,
            targetMuscles = targetMuscles,
            suggestedExerciseIds = emptyList(), // Filled by ExerciseRepository in ViewModel
            estimatedDurationMinutes = 45,
            reason = buildReason(targetMuscles, priorityReady.isNotEmpty()),
        )
    }

    private fun inferWorkoutType(muscles: List<MuscleGroup>): WorkoutType {
        val hasChest = MuscleGroup.CHEST in muscles
        val hasShoulder = MuscleGroup.FRONT_SHOULDERS in muscles || MuscleGroup.BACK_SHOULDERS in muscles
        val hasTriceps = MuscleGroup.TRICEPS in muscles
        val hasBack = MuscleGroup.UPPER_BACK in muscles || MuscleGroup.LATS in muscles
        val hasBiceps = MuscleGroup.BICEPS in muscles
        val hasLegs = MuscleGroup.QUADRICEPS in muscles || MuscleGroup.HAMSTRINGS in muscles || MuscleGroup.GLUTES in muscles

        return when {
            hasChest && hasShoulder && hasTriceps -> WorkoutType.PUSH
            hasBack && hasBiceps -> WorkoutType.PULL
            hasLegs -> WorkoutType.LOWER_BODY
            muscles.size >= 4 -> WorkoutType.FULL_BODY
            else -> WorkoutType.CUSTOM
        }
    }

    private fun buildReason(muscles: List<MuscleGroup>, isPriority: Boolean): String {
        val muscleNames = muscles.take(3).joinToString(", ") { it.displayName }
        return if (isPriority) {
            "Your priority muscles are ready: $muscleNames"
        } else {
            "$muscleNames are fully recovered and ready to train"
        }
    }
}
```

### `feature/today/presentation/TodaysWorkoutViewModel.kt`
```kotlin
@HiltViewModel
class TodaysWorkoutViewModel @Inject constructor(
    private val todaysWorkoutService: TodaysWorkoutService,
    private val muscleRankRepository: MuscleRankRepository,
    private val exerciseRepository: ExerciseRepository,
    private val prefsDataStore: UserPreferencesDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodaysWorkoutUiState())
    val uiState: StateFlow<TodaysWorkoutUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { generateRecommendation() }
    }

    private suspend fun generateRecommendation() {
        _uiState.update { it.copy(isLoading = true) }

        val ranks = muscleRankRepository.allRanks.first()
        val recommendation = todaysWorkoutService.getRecommendation(
            muscleRanks = ranks,
            lastTrainedDates = emptyMap(), // TODO: wire from workout history
            priorityMuscles = emptyList(), // TODO: wire from prefs
            availableEquipment = emptyList(),
            preferredDays = emptyList(),
        )

        if (recommendation != null) {
            // Fetch suggested exercises for target muscles
            val exercises = recommendation.targetMuscles.flatMap { muscle ->
                exerciseRepository.getByMuscle(muscle.name.lowercase()).first().take(3)
            }.distinctBy { it.id }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    recommendation = recommendation,
                    suggestedExercises = exercises,
                )
            }
        } else {
            _uiState.update { it.copy(isLoading = false, isRestDay = true) }
        }
    }
}

data class TodaysWorkoutUiState(
    val isLoading: Boolean = false,
    val recommendation: TodaysWorkoutService.WorkoutRecommendation? = null,
    val suggestedExercises: List<Exercise> = emptyList(),
    val isRestDay: Boolean = false,
)
```

### `feature/today/presentation/TodaysWorkoutPreviewScreen.kt`
```kotlin
@Composable
fun TodaysWorkoutPreviewScreen(
    onStartWorkout: (WorkoutType) -> Unit,
    onBack: () -> Unit,
    viewModel: TodaysWorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today's Workout") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
                }
                uiState.isRestDay -> {
                    Column(
                        Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("😴", style = MaterialTheme.typography.displayLarge)
                        Spacer(Modifier.height(16.dp))
                        Text("Rest Day", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                        Text("Your muscles are still recovering. Take it easy today.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                    }
                }
                else -> {
                    uiState.recommendation?.let { rec ->
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(rec.workoutType.displayName, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                                        Text(rec.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                        Text("~${rec.estimatedDurationMinutes} minutes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    }
                                }
                            }

                            item {
                                Text("Suggested Exercises", style = MaterialTheme.typography.titleSmall)
                            }

                            items(uiState.suggestedExercises) { exercise ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(exercise.name, style = MaterialTheme.typography.bodyMedium)
                                            Text(exercise.primaryMuscles.take(2).joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        }
                                        DifficultyBadge(difficulty = exercise.difficulty)
                                    }
                                }
                            }

                            item {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { onStartWorkout(rec.workoutType) },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                ) {
                                    Text("Start ${rec.workoutType.displayName} Workout")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

### `feature/report/presentation/WeeklyReportScreen.kt`
```kotlin
@Composable
fun WeeklyReportScreen(
    onBack: () -> Unit,
    viewModel: WeeklyReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Report") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("This Week", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Workouts", "${uiState.weeklyWorkouts}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    StatCard("XP Gained", "+${uiState.weeklyXP}", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    StatCard("PRs", "${uiState.weeklyPRs} 🏆", GoldAccent, Modifier.weight(1f))
                }
            }

            item {
                // 7-day workout activity chart
                WeeklyActivityBar(activeDays = uiState.activeDays)
            }

            item {
                Text("Top Muscles", style = MaterialTheme.typography.titleSmall)
            }

            items(uiState.topMuscles) { (muscle, xp) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(muscle.displayName, style = MaterialTheme.typography.bodyMedium)
                    Text("+$xp XP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun WeeklyActivityBar(activeDays: Set<DayOfWeek>) {
    val days = DayOfWeek.entries
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        days.forEach { day ->
            val isActive = day in activeDays
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(36.dp)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(day.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}
```

### `feature/report/presentation/WeeklyReportViewModel.kt`
```kotlin
@HiltViewModel
class WeeklyReportViewModel @Inject constructor(
    private val historyRepository: WorkoutHistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyReportUiState())
    val uiState: StateFlow<WeeklyReportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { loadWeeklyData() }
    }

    private fun loadWeeklyData() {
        viewModelScope.launch {
            val weekStart = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
            val weekStartMillis = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            historyRepository.getWorkoutHistory().collect { workouts ->
                val thisWeek = workouts.filter { it.startedAt >= weekStartMillis }
                val activeDays = thisWeek.map { summary ->
                    Instant.ofEpochMilli(summary.startedAt).atZone(ZoneId.systemDefault()).dayOfWeek
                }.toSet()

                _uiState.update {
                    it.copy(
                        weeklyWorkouts = thisWeek.size,
                        weeklyXP = thisWeek.sumOf { w -> w.totalXp },
                        weeklyPRs = thisWeek.sumOf { w -> w.prCount },
                        activeDays = activeDays,
                    )
                }
            }
        }
    }
}

data class WeeklyReportUiState(
    val weeklyWorkouts: Int = 0,
    val weeklyXP: Int = 0,
    val weeklyPRs: Int = 0,
    val activeDays: Set<DayOfWeek> = emptySet(),
    val topMuscles: List<Pair<MuscleGroup, Int>> = emptyList(),
)
```

---

## Verification
1. TodaysWorkoutPreviewScreen shows recommendation based on recovery state
2. Muscles that were trained yesterday show as "recovering" — not recommended
3. Muscles at 100% recovery are recommended first
4. WeeklyReportScreen shows correct workout count and XP for current week
5. WeeklyActivityBar highlights days with workouts correctly
