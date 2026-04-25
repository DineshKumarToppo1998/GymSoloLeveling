package com.hunterxdk.gymsololeveling.core.service

import com.hunterxdk.gymsololeveling.core.domain.model.Challenge
import com.hunterxdk.gymsololeveling.core.domain.model.enums.ChallengeDifficulty
import java.time.LocalDate
import java.time.ZoneId
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChallengeService @Inject constructor() {

    /**
     * Generates daily + weekly challenges seeded by LocalDate.now().
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
