# KOTLIN-PLAN-12.md — StreakService, StreakCheckWorker, ProfileScreen & RecoveryService

## Goal
Implement streak calculation, WorkManager midnight streak check, ProfileScreen with full stats display, and RecoveryService (muscle recovery time tracking).

## Phase
Features — Phase 4 of 6. Depends on KOTLIN-PLAN-09.

---

## Files to Create

### `core/service/StreakService.kt`
```kotlin
package com.example.gymlevels.core.service

import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakService @Inject constructor() {

    /**
     * Calculate current streak from a sorted list of workout dates (epoch millis).
     * A streak is consecutive days where at least one workout was logged.
     */
    fun calculateStreak(workoutDates: List<Long>): StreakResult {
        if (workoutDates.isEmpty()) return StreakResult(0, 0, null)

        val sortedDates = workoutDates
            .map { millis -> LocalDate.ofInstant(java.time.Instant.ofEpochMilli(millis), ZoneId.systemDefault()) }
            .distinct()
            .sortedDescending()

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        // Streak must include today or yesterday to still be active
        if (sortedDates.first() != today && sortedDates.first() != yesterday) {
            return StreakResult(currentStreak = 0, longestStreak = calculateLongest(sortedDates), lastWorkoutDate = sortedDates.firstOrNull())
        }

        var currentStreak = 0
        var expectedDate = if (sortedDates.first() == today) today else yesterday

        for (date in sortedDates) {
            if (date == expectedDate) {
                currentStreak++
                expectedDate = expectedDate.minusDays(1)
            } else if (date.isBefore(expectedDate)) {
                break
            }
        }

        return StreakResult(
            currentStreak = currentStreak,
            longestStreak = calculateLongest(sortedDates),
            lastWorkoutDate = sortedDates.firstOrNull(),
        )
    }

    private fun calculateLongest(sortedDates: List<LocalDate>): Int {
        if (sortedDates.isEmpty()) return 0
        var longest = 1
        var current = 1
        for (i in 1 until sortedDates.size) {
            if (sortedDates[i] == sortedDates[i - 1].minusDays(1)) {
                current++
                if (current > longest) longest = current
            } else {
                current = 1
            }
        }
        return longest
    }

    data class StreakResult(
        val currentStreak: Int,
        val longestStreak: Int,
        val lastWorkoutDate: LocalDate?,
    )
}
```

### `core/worker/StreakCheckWorker.kt`
```kotlin
package com.example.gymlevels.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.gymlevels.core.data.preferences.UserPreferencesDataStore
import com.example.gymlevels.core.service.StreakService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

@HiltWorker
class StreakCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val streakService: StreakService,
    private val firestore: FirebaseFirestore,
    private val prefsDataStore: UserPreferencesDataStore,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        return try {
            // Fetch all workout dates from Firestore
            val workouts = firestore.collection("users").document(uid)
                .collection("workouts")
                .get().await()

            val dates = workouts.documents.mapNotNull { it.getLong("startedAt") }
            val streakResult = streakService.calculateStreak(dates)

            // Update streak in Firestore
            firestore.collection("users").document(uid).update(mapOf(
                "currentStreak" to streakResult.currentStreak,
                "longestStreak" to streakResult.longestStreak,
            )).await()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "streak_check_worker"

        fun schedule(context: Context) {
            val now = LocalTime.now()
            val midnight = LocalTime.MIDNIGHT
            val delayMinutes = if (now.isBefore(midnight)) {
                java.time.Duration.between(now, midnight).toMinutes()
            } else {
                (24 * 60) - now.hour * 60 - now.minute
            }

            val request = PeriodicWorkRequestBuilder<StreakCheckWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
```

### `core/service/RecoveryService.kt`
```kotlin
package com.example.gymlevels.core.service

import com.example.gymlevels.core.domain.model.enums.MuscleGroup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecoveryService @Inject constructor() {

    // Recovery time in hours per muscle group (based on volume sensitivity)
    private val RECOVERY_HOURS = mapOf(
        MuscleGroup.CHEST to 48,
        MuscleGroup.FRONT_SHOULDERS to 36,
        MuscleGroup.BACK_SHOULDERS to 36,
        MuscleGroup.BICEPS to 36,
        MuscleGroup.TRICEPS to 36,
        MuscleGroup.FOREARMS to 24,
        MuscleGroup.UPPER_BACK to 48,
        MuscleGroup.MIDDLE_BACK to 48,
        MuscleGroup.LOWER_BACK to 72,
        MuscleGroup.LATS to 48,
        MuscleGroup.TRAPS to 36,
        MuscleGroup.ABS to 24,
        MuscleGroup.OBLIQUES to 24,
        MuscleGroup.QUADRICEPS to 72,
        MuscleGroup.HAMSTRINGS to 72,
        MuscleGroup.GLUTES to 48,
        MuscleGroup.CALVES to 24,
    )

    data class MuscleRecovery(
        val muscleGroup: MuscleGroup,
        val lastTrainedAt: Long?,
        val recoveryHours: Int,
        val recoveryPercent: Float,  // 0.0 = just trained, 1.0 = fully recovered
        val isReady: Boolean,
    )

    fun calculateRecovery(
        lastTrainedDates: Map<MuscleGroup, Long>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<MuscleRecovery> {
        return MuscleGroup.entries.map { muscle ->
            val lastTrained = lastTrainedDates[muscle]
            val recoveryHours = RECOVERY_HOURS[muscle] ?: 48

            if (lastTrained == null) {
                MuscleRecovery(muscle, null, recoveryHours, 1.0f, true)
            } else {
                val hoursElapsed = (nowMillis - lastTrained) / (1000 * 60 * 60f)
                val recoveryPercent = (hoursElapsed / recoveryHours).coerceIn(0f, 1f)
                MuscleRecovery(
                    muscleGroup = muscle,
                    lastTrainedAt = lastTrained,
                    recoveryHours = recoveryHours,
                    recoveryPercent = recoveryPercent,
                    isReady = recoveryPercent >= 1.0f,
                )
            }
        }
    }

    fun getReadyMuscles(lastTrainedDates: Map<MuscleGroup, Long>): List<MuscleGroup> =
        calculateRecovery(lastTrainedDates).filter { it.isReady }.map { it.muscleGroup }
}
```

### `feature/profile/data/PlayerStatsRepository.kt`
```kotlin
@Singleton
class PlayerStatsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    val stats: Flow<PlayerStats?> = callbackFlow {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run { trySend(null); return@callbackFlow }
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snap, _ ->
                val stats = snap?.let { doc ->
                    PlayerStats(
                        userId = uid,
                        totalWorkouts = doc.getLong("totalWorkouts")?.toInt() ?: 0,
                        totalVolumeKg = doc.getDouble("totalVolumeKg") ?: 0.0,
                        totalXp = doc.getLong("totalXp")?.toInt() ?: 0,
                        currentStreak = doc.getLong("currentStreak")?.toInt() ?: 0,
                        longestStreak = doc.getLong("longestStreak")?.toInt() ?: 0,
                        playerLevel = doc.getLong("playerLevel")?.toInt() ?: 1,
                        playerClass = runCatching { PlayerClass.valueOf(doc.getString("playerClass") ?: "") }.getOrDefault(PlayerClass.BALANCE_WARRIOR),
                        overallRank = RankTier.UNTRAINED,
                        lastWorkoutAt = doc.getLong("lastWorkoutAt"),
                        totalPRs = doc.getLong("totalPRs")?.toInt() ?: 0,
                    )
                }
                trySend(stats)
            }
        awaitClose { listener.remove() }
    }

    val currentStreak: Flow<Int> = stats.map { it?.currentStreak ?: 0 }
}
```

### `feature/profile/presentation/ProfileScreen.kt`
```kotlin
@Composable
fun ProfileScreen(
    onSettings: () -> Unit,
    onSignOut: () -> Unit,
    onAchievements: () -> Unit,
    onWeightTracker: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stats = uiState.stats

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                },
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
            // User header
            item {
                PlayerHeader(user = uiState.user, stats = stats)
            }

            // XP Progress card
            stats?.let { s ->
                item { XPProgressCard(stats = s) }
            }

            // Stats grid
            stats?.let { s ->
                item {
                    ProfileStatsGrid(stats = s)
                }
            }

            // Streak stats
            stats?.let { s ->
                item {
                    StreakCard(current = s.currentStreak, longest = s.longestStreak)
                }
            }

            // Quick nav cards
            item {
                Card(onClick = onAchievements, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Achievements", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }

            item {
                Card(onClick = onWeightTracker, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(12.dp))
                        Text("Weight Tracker", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out")
                }
            }
        }
    }
}

@Composable
fun PlayerHeader(user: User?, stats: PlayerStats?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Avatar
        Box(
            Modifier.size(72.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (user?.photoUrl != null) {
                AsyncImage(model = user.photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape))
            } else {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }

        Column {
            Text(user?.displayName ?: "Warrior", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
            stats?.let {
                Text(it.playerClass.displayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Text("Level ${it.playerLevel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun ProfileStatsGrid(stats: PlayerStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Workouts", stats.totalWorkouts.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            StatCard("PRs", stats.totalPRs.toString(), GoldAccent, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Volume", "${(stats.totalVolumeKg / 1000).toInt()}t", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            StatCard("Exercises", stats.uniqueExercisesCount.toString(), MaterialTheme.colorScheme.tertiary ?: MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        }
    }
}

@Composable
fun StreakCard(current: Int, longest: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔥", style = MaterialTheme.typography.headlineMedium)
                Text(current.toString(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Current Streak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Divider(modifier = Modifier.height(80.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏆", style = MaterialTheme.typography.headlineMedium)
                Text(longest.toString(), style = MaterialTheme.typography.headlineSmall, color = GoldAccent, fontWeight = FontWeight.Bold)
                Text("Best Streak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}
```

### `feature/profile/presentation/ProfileViewModel.kt`
```kotlin
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val playerStatsRepository: PlayerStatsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                authRepository.currentUser,
                playerStatsRepository.stats
            ) { user, stats ->
                ProfileUiState(user = user, stats = stats)
            }.collect { _uiState.value = it }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}

data class ProfileUiState(
    val user: User? = null,
    val stats: PlayerStats? = null,
    val isLoading: Boolean = false,
)
```

---

## WorkManager Setup in GymLevelsApp.kt
```kotlin
// Add to GymLevelsApp.onCreate():
override fun onCreate() {
    super.onCreate()
    StreakCheckWorker.schedule(this)
}
```

---

## Verification
1. ProfileScreen displays name, class, level, all stats
2. StreakCard shows current streak and best streak
3. StreakCheckWorker schedules successfully (WorkManager inspector in Android Studio)
4. Force-clear streak (skip a day) → next midnight run → currentStreak resets to 0 in Firestore
5. RecoveryService returns correct ready/recovering state per muscle
