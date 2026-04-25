# KOTLIN-PLAN-11.md — AchievementService, ChallengesScreen & ChallengeService

## Goal
Implement the full AchievementService (8 badge types, multi-tier), ChallengeService (date-seeded deterministic generation), and ChallengesScreen with progress tracking.

## Phase
Features — Phase 4 of 6. Depends on KOTLIN-PLAN-09.

---

## Challenge Names (confirmed from libapp.so)
DailyGrind, WeeklyWarrior, PerfectWeek, RepCounter, SetCrusher, TonCrusher, HeavyWeek, VolumeDay, PRWeek, FullBody, BeatYourself, DedicatedWeek, ExtendedSession, VarietyPack, ExerciseExplorer, BossChallenge, SpecialChallenge

---

## Files to Create

### `core/service/ChallengeService.kt`
```kotlin
package com.example.gymlevels.core.service

import com.example.gymlevels.core.domain.model.Challenge
import com.example.gymlevels.core.domain.model.enums.ChallengeDifficulty
import java.time.LocalDate
import java.time.ZoneId
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChallengeService @Inject constructor() {

    /**
     * Generates daily + weekly challenges seeded by LocalDate.now()
     * Same date always produces same challenges (deterministic).
     */
    fun getDailyAndWeeklyChallenges(): List<Challenge> {
        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        return generateDailyChallenges(today) + generateWeeklyChallenges(weekStart)
    }

    fun generateDailyChallenges(date: LocalDate, count: Int = 3): List<Challenge> {
        val seed = date.toEpochDay()
        val rng = Random(seed)
        val expiresAt = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val pool = buildDailyChallengePool(rng)
        return pool.shuffled(rng).take(count).mapIndexed { i, def ->
            Challenge(
                id = "daily_${date}_$i",
                name = def.name,
                description = def.description,
                targetValue = def.targetValue,
                currentValue = 0.0,
                xpReward = def.xpReward,
                expiresAt = expiresAt,
                difficulty = def.difficulty,
            )
        }
    }

    fun generateWeeklyChallenges(weekStart: LocalDate, count: Int = 2): List<Challenge> {
        val seed = weekStart.toEpochDay() + 999999L
        val rng = Random(seed)
        val expiresAt = weekStart.plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val pool = buildWeeklyChallengePool(rng)
        return pool.shuffled(rng).take(count).mapIndexed { i, def ->
            Challenge(
                id = "weekly_${weekStart}_$i",
                name = def.name,
                description = def.description,
                targetValue = def.targetValue,
                currentValue = 0.0,
                xpReward = def.xpReward,
                expiresAt = expiresAt,
                difficulty = def.difficulty,
            )
        }
    }

    private data class ChallengeDef(
        val name: String,
        val description: String,
        val targetValue: Double,
        val xpReward: Int,
        val difficulty: ChallengeDifficulty,
    )

    private fun buildDailyChallengePool(rng: Random): List<ChallengeDef> = listOf(
        ChallengeDef("DailyGrind", "Complete a workout today", 1.0, 100, ChallengeDifficulty.EASY),
        ChallengeDef("RepCounter", "Log ${50 + rng.nextInt(50)} total reps", (50 + rng.nextInt(50)).toDouble(), 150, ChallengeDifficulty.MEDIUM),
        ChallengeDef("SetCrusher", "Complete ${5 + rng.nextInt(10)} sets today", (5 + rng.nextInt(10)).toDouble(), 120, ChallengeDifficulty.EASY),
        ChallengeDef("VolumeDay", "Move ${1000 + rng.nextInt(2000)}kg total volume", (1000 + rng.nextInt(2000)).toDouble(), 200, ChallengeDifficulty.HARD),
        ChallengeDef("ExtendedSession", "Work out for at least ${30 + rng.nextInt(30)} minutes", (30 + rng.nextInt(30)).toDouble(), 150, ChallengeDifficulty.MEDIUM),
        ChallengeDef("FullBody", "Train ${3 + rng.nextInt(3)} different muscle groups", (3 + rng.nextInt(3)).toDouble(), 175, ChallengeDifficulty.MEDIUM),
        ChallengeDef("ExerciseExplorer", "Try ${2 + rng.nextInt(2)} different exercises", (2 + rng.nextInt(2)).toDouble(), 100, ChallengeDifficulty.EASY),
        ChallengeDef("BeatYourself", "Break a personal record today", 1.0, 250, ChallengeDifficulty.HARD),
    )

    private fun buildWeeklyChallengePool(rng: Random): List<ChallengeDef> = listOf(
        ChallengeDef("WeeklyWarrior", "Complete ${3 + rng.nextInt(3)} workouts this week", (3 + rng.nextInt(3)).toDouble(), 500, ChallengeDifficulty.MEDIUM),
        ChallengeDef("PerfectWeek", "Work out ${5 + rng.nextInt(2)} days this week", (5 + rng.nextInt(2)).toDouble(), 750, ChallengeDifficulty.HARD),
        ChallengeDef("TonCrusher", "Move ${5000 + rng.nextInt(5000)}kg total volume this week", (5000 + rng.nextInt(5000)).toDouble(), 600, ChallengeDifficulty.HARD),
        ChallengeDef("HeavyWeek", "Complete ${15 + rng.nextInt(10)} sets this week", (15 + rng.nextInt(10)).toDouble(), 400, ChallengeDifficulty.MEDIUM),
        ChallengeDef("DedicatedWeek", "Log workouts on ${4 + rng.nextInt(3)} consecutive days", (4 + rng.nextInt(3)).toDouble(), 700, ChallengeDifficulty.HARD),
        ChallengeDef("PRWeek", "Set ${2 + rng.nextInt(3)} personal records this week", (2 + rng.nextInt(3)).toDouble(), 650, ChallengeDifficulty.HARD),
        ChallengeDef("VarietyPack", "Try ${5 + rng.nextInt(5)} different exercises this week", (5 + rng.nextInt(5)).toDouble(), 450, ChallengeDifficulty.MEDIUM),
        ChallengeDef("BossChallenge", "Complete a Full Body workout this week", 1.0, 800, ChallengeDifficulty.HARD),
        ChallengeDef("SpecialChallenge", "Train every muscle group at least once", 17.0, 1000, ChallengeDifficulty.HARD),
    )
}
```

### `core/service/AchievementService.kt`
```kotlin
package com.example.gymlevels.core.service

import com.example.gymlevels.core.domain.model.*
import com.example.gymlevels.core.domain.model.enums.BadgeType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementService @Inject constructor() {

    data class AchievementDef(
        val id: String,
        val type: BadgeType,
        val tier: Int,
        val name: String,
        val description: String,
        val xpReward: Int,
        val condition: (PlayerStats) -> Boolean,
    )

    val ACHIEVEMENT_DEFINITIONS: List<AchievementDef> = listOf(
        // MILESTONE — workout count
        AchievementDef("milestone_1", BadgeType.MILESTONE, 1, "First Steps", "Complete your first workout", 100) { it.totalWorkouts >= 1 },
        AchievementDef("milestone_2", BadgeType.MILESTONE, 2, "Getting Serious", "Complete 10 workouts", 250) { it.totalWorkouts >= 10 },
        AchievementDef("milestone_3", BadgeType.MILESTONE, 3, "Dedicated", "Complete 50 workouts", 500) { it.totalWorkouts >= 50 },
        AchievementDef("milestone_4", BadgeType.MILESTONE, 4, "Century Club", "Complete 100 workouts", 1000) { it.totalWorkouts >= 100 },
        AchievementDef("milestone_5", BadgeType.MILESTONE, 5, "Iron Will", "Complete 365 workouts", 2000) { it.totalWorkouts >= 365 },

        // CONSISTENCY — streaks
        AchievementDef("streak_3", BadgeType.CONSISTENCY, 1, "3 Day Streak", "Work out 3 days in a row", 150) { it.currentStreak >= 3 },
        AchievementDef("streak_7", BadgeType.CONSISTENCY, 2, "7 Day Streak", "Work out 7 days in a row", 300) { it.currentStreak >= 7 },
        AchievementDef("streak_14", BadgeType.CONSISTENCY, 3, "2 Week Grind", "Work out 14 days in a row", 600) { it.currentStreak >= 14 },
        AchievementDef("streak_30", BadgeType.CONSISTENCY, 4, "Month Strong", "Work out 30 days in a row", 1000) { it.currentStreak >= 30 },
        AchievementDef("streak_100", BadgeType.CONSISTENCY, 5, "Iron Discipline", "Work out 100 days in a row", 3000) { it.currentStreak >= 100 },

        // PR_HUNTER
        AchievementDef("pr_1", BadgeType.PR_HUNTER, 1, "First PR", "Set your first personal record", 200) { it.totalPRs >= 1 },
        AchievementDef("pr_10", BadgeType.PR_HUNTER, 2, "Record Breaker", "Set 10 personal records", 400) { it.totalPRs >= 10 },
        AchievementDef("pr_50", BadgeType.PR_HUNTER, 3, "PR Machine", "Set 50 personal records", 800) { it.totalPRs >= 50 },

        // VOLUME
        AchievementDef("volume_10k", BadgeType.VOLUME, 1, "10 Tonne Club", "Move 10,000kg total volume", 200) { it.totalVolumeKg >= 10000 },
        AchievementDef("volume_100k", BadgeType.VOLUME, 2, "100 Tonne Legend", "Move 100,000kg total volume", 500) { it.totalVolumeKg >= 100000 },
        AchievementDef("volume_1m", BadgeType.VOLUME, 3, "Million Kilo Beast", "Move 1,000,000kg total volume", 2000) { it.totalVolumeKg >= 1000000 },

        // EXPLORER
        AchievementDef("explorer_10", BadgeType.EXPLORER, 1, "Exercise Explorer", "Try 10 different exercises", 150) { it.uniqueExercisesCount >= 10 },
        AchievementDef("explorer_50", BadgeType.EXPLORER, 2, "Movement Master", "Try 50 different exercises", 400) { it.uniqueExercisesCount >= 50 },
        AchievementDef("explorer_100", BadgeType.EXPLORER, 3, "Exercise Encyclopedia", "Try 100 different exercises", 800) { it.uniqueExercisesCount >= 100 },

        // MUSCLE_MASTER — all muscles ranked
        AchievementDef("bronze_all", BadgeType.MUSCLE_MASTER, 1, "Bronze Body", "Reach Bronze rank on all muscles", 500) { false }, // Checked separately
        AchievementDef("gold_all", BadgeType.MUSCLE_MASTER, 2, "Golden Physique", "Reach Gold rank on all muscles", 2000) { false },
        AchievementDef("legend_any", BadgeType.MUSCLE_MASTER, 3, "Legend Muscle", "Reach Legend rank on any muscle", 5000) { false },

        // LEVEL milestones
        AchievementDef("level_10", BadgeType.STRENGTH, 1, "Level 10", "Reach Player Level 10", 500) { it.playerLevel >= 10 },
        AchievementDef("level_20", BadgeType.STRENGTH, 2, "Level 20", "Reach Player Level 20", 1000) { it.playerLevel >= 20 },
        AchievementDef("level_30", BadgeType.STRENGTH, 3, "Max Level", "Reach Player Level 30", 3000) { it.playerLevel >= 30 },
    )

    fun checkNewAchievements(
        stats: PlayerStats,
        muscleRanks: List<MuscleRank>,
        existingAchievements: Set<String>
    ): List<AchievementDef> {
        val newlyEarned = mutableListOf<AchievementDef>()

        ACHIEVEMENT_DEFINITIONS.forEach { def ->
            if (def.id in existingAchievements) return@forEach

            val earned = when (def.type) {
                BadgeType.MUSCLE_MASTER -> when (def.id) {
                    "bronze_all" -> muscleRanks.isNotEmpty() && muscleRanks.all { it.currentRank >= RankTier.BRONZE }
                    "gold_all" -> muscleRanks.isNotEmpty() && muscleRanks.all { it.currentRank >= RankTier.GOLD }
                    "legend_any" -> muscleRanks.any { it.currentRank == RankTier.LEGEND }
                    else -> false
                }
                else -> def.condition(stats)
            }

            if (earned) newlyEarned.add(def)
        }

        return newlyEarned
    }
}
```

### `feature/challenges/presentation/ChallengesViewModel.kt`
```kotlin
@HiltViewModel
class ChallengesViewModel @Inject constructor(
    private val challengeService: ChallengeService,
    private val achievementService: AchievementService,
    private val playerStatsRepository: PlayerStatsRepository,
    private val muscleRankRepository: MuscleRankRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChallengesUiState())
    val uiState: StateFlow<ChallengesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { loadChallenges() }
    }

    private fun loadChallenges() {
        val challenges = challengeService.getDailyAndWeeklyChallenges()
        val daily = challenges.filter { it.id.startsWith("daily_") }
        val weekly = challenges.filter { it.id.startsWith("weekly_") }
        _uiState.update { it.copy(dailyChallenges = daily, weeklyChallenges = weekly) }
    }
}

data class ChallengesUiState(
    val dailyChallenges: List<Challenge> = emptyList(),
    val weeklyChallenges: List<Challenge> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val recentlyEarned: List<Achievement> = emptyList(),
    val isLoading: Boolean = false,
)
```

### `feature/challenges/presentation/ChallengesScreen.kt`
```kotlin
@Composable
fun ChallengesScreen(
    onBack: () -> Unit,
    viewModel: ChallengesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Challenges") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Daily Challenges", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }

            items(uiState.dailyChallenges) { challenge ->
                ChallengeCard(challenge = challenge)
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("Weekly Challenges", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }

            items(uiState.weeklyChallenges) { challenge ->
                ChallengeCard(challenge = challenge)
            }
        }
    }
}

@Composable
fun ChallengeCard(challenge: Challenge) {
    val progress = (challenge.currentValue / challenge.targetValue).coerceIn(0.0, 1.0).toFloat()
    val difficultyColor = when (challenge.difficulty) {
        ChallengeDifficulty.EASY -> Color(0xFF4CAF50)
        ChallengeDifficulty.MEDIUM -> Color(0xFFFF9800)
        ChallengeDifficulty.HARD -> Color(0xFFE91E63)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(challenge.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(challenge.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("+${challenge.xpReward} XP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = difficultyColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            challenge.difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
                            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = difficultyColor
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = if (challenge.isCompleted) MaterialTheme.colorScheme.secondary else difficultyColor,
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${challenge.currentValue.toInt()} / ${challenge.targetValue.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                if (challenge.isCompleted) {
                    Text("Completed ✓", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
```

---

## Verification
1. ChallengesScreen shows 3 daily + 2 weekly challenges
2. Run app on 2 different days → different challenges generated (date-seeded)
3. Run app on same day twice → same challenges (deterministic)
4. Challenge card shows difficulty color-coded badge (green/orange/red)
5. AchievementService.checkNewAchievements() returns "First Steps" after first workout completes
