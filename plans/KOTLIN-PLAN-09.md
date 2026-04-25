# KOTLIN-PLAN-09.md — XPService, RankService, WorkoutCompleteScreen & Firestore Sync

## Goal
Implement the full XP calculation engine, rank calculation engine, WorkoutCompleteScreen with animated XP gain display, and Firestore sync of completed workouts with muscle rank updates.

## Phase
Features — Phase 4 of 6. Depends on KOTLIN-PLAN-03, KOTLIN-PLAN-08.

---

## XP Values (confirmed from libapp.so strings)
50, 100, 150, 200, 500, 750, 1000, 2000, 3000, 7500, 17000, 22000, 30000, 75000, 200000

---

## Files to Create

### `core/service/XPService.kt`
```kotlin
package com.example.gymlevels.core.service

import com.example.gymlevels.core.domain.model.*

object XPService {

    fun calculateSetXP(set: WorkoutSet, exercise: Exercise): Int {
        val base = when (exercise.counterType()) {
            ExerciseCounterType.REPS_AND_WEIGHT -> {
                val volume = (set.reps ?: 0) * (set.weightKg ?: 0.0)
                when {
                    volume >= 2000 -> 50
                    volume >= 1000 -> 40
                    volume >= 500  -> 30
                    volume >= 100  -> 20
                    else           -> 10
                }
            }
            ExerciseCounterType.BODYWEIGHT,
            ExerciseCounterType.REPS,
            ExerciseCounterType.REPS_ONLY -> when {
                (set.reps ?: 0) >= 20 -> 30
                (set.reps ?: 0) >= 10 -> 20
                else -> 10
            }
            ExerciseCounterType.TIME,
            ExerciseCounterType.TIME_ONLY,
            ExerciseCounterType.TIME_AND_SETS -> {
                ((set.durationSeconds ?: 0) / 60.0 * 5).coerceIn(10.0, 50.0).toInt()
            }
            ExerciseCounterType.TIME_AND_DISTANCE_AND_INCLINE,
            ExerciseCounterType.TIME_AND_DISTANCE_AND_RESISTANCE -> {
                val distanceBonus = ((set.distanceKm ?: 0.0) * 10).toInt()
                (((set.durationSeconds ?: 0) / 60.0 * 5) + distanceBonus).coerceIn(10.0, 50.0).toInt()
            }
            ExerciseCounterType.TIME_AND_FLOORS_AND_STEPS -> {
                val floorsBonus = (set.floors ?: 0) * 2
                ((set.durationSeconds ?: 0) / 60.0 * 3 + floorsBonus).coerceIn(10.0, 50.0).toInt()
            }
            ExerciseCounterType.RESISTANCE_BAND_STRENGTH -> when {
                (set.reps ?: 0) >= 20 -> 25
                (set.reps ?: 0) >= 10 -> 15
                else -> 10
            }
            ExerciseCounterType.RESISTANCE_BAND_STRENGTH_AND_TIME -> {
                ((set.durationSeconds ?: 0) / 60.0 * 5).coerceIn(10.0, 40.0).toInt()
            }
        }

        val difficultyMult = 1.0 + (exercise.difficulty - 1) * 0.2
        val prBonus = if (set.isPersonalRecord) 25 else 0
        return (base * difficultyMult).toInt() + prBonus
    }

    fun calculateWorkoutCompletionBonus(session: WorkoutSession): Int {
        val exerciseCount = session.exercises.size
        val totalSets = session.exercises.sumOf { it.sets.size }
        val durationMinutes = session.durationSeconds / 60

        return when {
            totalSets >= 30 && durationMinutes >= 60 -> 200
            totalSets >= 20 && durationMinutes >= 45 -> 150
            totalSets >= 15 && durationMinutes >= 30 -> 100
            totalSets >= 10 -> 75
            exerciseCount >= 5 -> 60
            exerciseCount >= 3 -> 50
            else -> 25
        }
    }

    /**
     * Distribute XP to muscles worked.
     * Primary muscles receive 70%, secondary muscles receive 30% of set XP.
     */
    fun distributeXPToMuscles(set: WorkoutSet, exercise: Exercise, setXP: Int): Map<String, Int> {
        val distribution = mutableMapOf<String, Int>()

        val primaryMuscles = exercise.primaryMuscles
        val secondaryMuscles = exercise.secondaryMuscles

        if (primaryMuscles.isNotEmpty()) {
            val primaryXpEach = (setXP * 0.7 / primaryMuscles.size).toInt()
            primaryMuscles.forEach { muscle ->
                distribution[muscle] = (distribution[muscle] ?: 0) + primaryXpEach
            }
        }

        if (secondaryMuscles.isNotEmpty()) {
            val secondaryXpEach = (setXP * 0.3 / secondaryMuscles.size).toInt()
            secondaryMuscles.forEach { muscle ->
                distribution[muscle] = (distribution[muscle] ?: 0) + secondaryXpEach
            }
        }

        // If no muscles defined, give full XP to unknown
        if (primaryMuscles.isEmpty() && secondaryMuscles.isEmpty()) {
            distribution["unknown"] = setXP
        }

        return distribution
    }

    /**
     * Calculate total XP gained from a completed workout session.
     * Returns per-muscle XP map + completion bonus.
     */
    data class WorkoutXPResult(
        val totalXp: Int,
        val completionBonus: Int,
        val muscleXpDistribution: Map<String, Int>,
        val prCount: Int,
    )

    fun calculateWorkoutXP(session: WorkoutSession, exercises: Map<String, Exercise>): WorkoutXPResult {
        val muscleXpMap = mutableMapOf<String, Int>()
        var totalSetXP = 0
        var prCount = 0

        session.exercises.forEach { workoutExercise ->
            val exercise = exercises[workoutExercise.exercise.id] ?: workoutExercise.exercise
            workoutExercise.sets.forEach { set ->
                val setXP = calculateSetXP(set, exercise)
                totalSetXP += setXP
                if (set.isPersonalRecord) prCount++

                val distribution = distributeXPToMuscles(set, exercise, setXP)
                distribution.forEach { (muscle, xp) ->
                    muscleXpMap[muscle] = (muscleXpMap[muscle] ?: 0) + xp
                }
            }
        }

        val completionBonus = calculateWorkoutCompletionBonus(session)
        return WorkoutXPResult(
            totalXp = totalSetXP + completionBonus,
            completionBonus = completionBonus,
            muscleXpDistribution = muscleXpMap,
            prCount = prCount,
        )
    }
}
```

### `core/service/RankService.kt`
```kotlin
package com.example.gymlevels.core.service

import com.example.gymlevels.core.domain.model.*

object RankService {

    fun calculateRankFromXP(totalXp: Int): MuscleRankResult {
        val thresholds = XPThresholds.SUB_RANK_THRESHOLDS
        var currentRank = RankTier.UNTRAINED
        var currentSubRank: RankSubTier? = null
        var xpToNext = thresholds[0].third

        for (i in thresholds.indices) {
            val (rank, subRank, threshold) = thresholds[i]
            if (totalXp >= threshold) {
                currentRank = rank
                currentSubRank = subRank
                xpToNext = thresholds.getOrNull(i + 1)?.third?.minus(totalXp) ?: 0
            } else {
                xpToNext = threshold - totalXp
                break
            }
        }

        return MuscleRankResult(
            rank = currentRank,
            subRank = currentSubRank,
            xpToNext = xpToNext.coerceAtLeast(0),
        )
    }

    fun calculatePlayerLevel(totalXp: Int): Int {
        val thresholds = XPThresholds.PLAYER_LEVEL_THRESHOLDS
        for (i in thresholds.indices.reversed()) {
            if (totalXp >= thresholds[i]) return i + 1
        }
        return 1
    }

    data class MuscleRankResult(
        val rank: RankTier,
        val subRank: RankSubTier?,
        val xpToNext: Int,
    )

    fun getOverallRank(muscleRanks: List<MuscleRank>): RankTier {
        if (muscleRanks.isEmpty()) return RankTier.UNTRAINED
        val avgXp = muscleRanks.sumOf { it.totalXp } / muscleRanks.size
        return calculateRankFromXP(avgXp).rank
    }

    fun getRankDisplayName(rank: RankTier, subRank: RankSubTier?): String = buildString {
        append(rank.name.lowercase().replaceFirstChar { it.uppercase() })
        subRank?.let { append(" ${it.name}") }
    }

    fun getRankColor(rank: RankTier): androidx.compose.ui.graphics.Color = when (rank) {
        RankTier.UNTRAINED -> com.example.gymlevels.core.theme.RankUntrainedColor
        RankTier.BRONZE -> com.example.gymlevels.core.theme.RankBronzeColor
        RankTier.SILVER -> com.example.gymlevels.core.theme.RankSilverColor
        RankTier.GOLD -> com.example.gymlevels.core.theme.RankGoldColor
        RankTier.PLATINUM -> com.example.gymlevels.core.theme.RankPlatinumColor
        RankTier.DIAMOND -> com.example.gymlevels.core.theme.RankDiamondColor
        RankTier.MASTER -> com.example.gymlevels.core.theme.RankMasterColor
        RankTier.LEGEND -> com.example.gymlevels.core.theme.RankLegendColor
    }
}
```

### `feature/workout/data/WorkoutRepository.kt`
```kotlin
@Singleton
class WorkoutRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val muscleRankDao: MuscleRankDao,
    private val persistenceManager: WorkoutPersistenceManager,
    private val exerciseRepository: ExerciseRepository,
) {
    suspend fun saveCompletedWorkout(
        session: WorkoutSession,
        xpResult: XPService.WorkoutXPResult
    ): Result<Unit> = runCatching {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("Not signed in")

        // 1. Update muscle ranks in Room
        xpResult.muscleXpDistribution.forEach { (muscleStr, xp) ->
            val existing = muscleRankDao.get(muscleStr)
            val newTotalXp = (existing?.totalXp ?: 0) + xp
            val rankResult = RankService.calculateRankFromXP(newTotalXp)
            muscleRankDao.upsert(MuscleRankEntity(
                muscleGroup = muscleStr,
                totalXp = newTotalXp,
                currentRank = rankResult.rank.name,
                currentSubRank = rankResult.subRank?.name,
                xpToNextRank = rankResult.xpToNext,
                updatedAt = System.currentTimeMillis(),
            ))
        }

        // 2. Sync workout to Firestore
        val workoutData = mapOf(
            "id" to session.id,
            "userId" to uid,
            "title" to session.title,
            "workoutType" to session.workoutType.name,
            "startedAt" to session.startedAt,
            "endedAt" to System.currentTimeMillis(),
            "totalXp" to xpResult.totalXp,
            "completionBonus" to xpResult.completionBonus,
            "prCount" to xpResult.prCount,
            "exerciseCount" to session.exercises.size,
            "totalSets" to session.exercises.sumOf { it.sets.size },
            "totalVolumeKg" to session.totalVolumeKg,
        )
        firestore.collection("users").document(uid)
            .collection("workouts").document(session.id).set(workoutData).await()

        // 3. Update player stats in Firestore
        val statsRef = firestore.collection("users").document(uid)
        firestore.runTransaction { tx ->
            val snap = tx.get(statsRef)
            val currentTotalXp = snap.getLong("totalXp")?.toInt() ?: 0
            val currentWorkouts = snap.getLong("totalWorkouts")?.toInt() ?: 0
            val currentPRs = snap.getLong("totalPRs")?.toInt() ?: 0
            val newTotalXp = currentTotalXp + xpResult.totalXp
            tx.update(statsRef, mapOf(
                "totalXp" to newTotalXp,
                "totalWorkouts" to (currentWorkouts + 1),
                "totalPRs" to (currentPRs + xpResult.prCount),
                "playerLevel" to RankService.calculatePlayerLevel(newTotalXp),
                "lastWorkoutAt" to System.currentTimeMillis(),
            ))
        }.await()

        // 4. Clear crash recovery state
        persistenceManager.clearSession()
    }
}
```

### `feature/workout/presentation/WorkoutCompleteViewModel.kt`
```kotlin
@HiltViewModel
class WorkoutCompleteViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutCompleteUiState())
    val uiState: StateFlow<WorkoutCompleteUiState> = _uiState.asStateFlow()

    fun processWorkout(session: WorkoutSession) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val exerciseLookup = session.exercises.associate { it.exercise.id to it.exercise }
            val xpResult = XPService.calculateWorkoutXP(session, exerciseLookup)

            workoutRepository.saveCompletedWorkout(session, xpResult)
                .onSuccess {
                    _uiState.update { it.copy(isProcessing = false, xpResult = xpResult, session = session) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isProcessing = false, error = e.message) }
                }
        }
    }
}

data class WorkoutCompleteUiState(
    val isProcessing: Boolean = false,
    val session: WorkoutSession? = null,
    val xpResult: XPService.WorkoutXPResult? = null,
    val error: String? = null,
)
```

### `feature/workout/presentation/WorkoutCompleteScreen.kt`
```kotlin
@Composable
fun WorkoutCompleteScreen(
    session: WorkoutSession,
    onDone: () -> Unit,
    viewModel: WorkoutCompleteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(session) { viewModel.processWorkout(session) }

    // XP counter animation
    val animatedXP by animateIntAsState(
        targetValue = uiState.xpResult?.totalXp ?: 0,
        animationSpec = tween(durationMillis = 1500),
        label = "xpCounter"
    )

    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.xpResult) {
        if (uiState.xpResult != null) { delay(200); showContent = true }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        if (uiState.isProcessing) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else {
            AnimatedVisibility(visible = showContent, enter = fadeIn() + slideInVertically { it / 2 }) {
                LazyColumn(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("Workout Complete!", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                    }

                    item {
                        // XP gained display
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("XP Earned", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                Text(
                                    text = "+$animatedXP XP",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                uiState.xpResult?.completionBonus?.let { bonus ->
                                    Text("Includes +$bonus completion bonus", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }

                    // Workout stats
                    uiState.session?.let { s ->
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                StatChip("${s.exercises.size}", "Exercises")
                                StatChip("${s.exercises.sumOf { it.sets.size }}", "Sets")
                                StatChip("${s.durationSeconds / 60}m", "Duration")
                            }
                        }
                    }

                    // PR count
                    uiState.xpResult?.let { result ->
                        if (result.prCount > 0) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("🏆", style = MaterialTheme.typography.titleLarge)
                                        Text("${result.prCount} Personal Record${if (result.prCount > 1) "s" else ""}!", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }

                    // Top muscle XP distribution
                    uiState.xpResult?.muscleXpDistribution?.let { dist ->
                        if (dist.isNotEmpty()) {
                            item {
                                Text("Muscles Trained", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            items(dist.entries.sortedByDescending { it.value }.take(5).toList()) { (muscle, xp) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(muscle.replace("_", " ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
                                    Text("+$xp XP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = onDone,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        ) {
                            Text("Done", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}
```

---

## Verification
1. Complete a workout → WorkoutCompleteScreen shows with animated XP counter (0 → earned)
2. Check Room `muscle_ranks` table — rows updated with correct XP distribution
3. Check Firestore `users/{uid}/workouts/{workoutId}` — document created
4. Check Firestore `users/{uid}` — `totalXp`, `totalWorkouts`, `playerLevel` updated
5. Bench press set (100kg × 10 reps = 1000 volume) → 40 base × difficulty mult → chest/tricep XP
6. PR detection: second set heavier than first → `isPersonalRecord = true` in Room
