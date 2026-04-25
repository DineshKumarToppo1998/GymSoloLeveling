package com.hunterxdk.gymsololeveling.core.service

import com.hunterxdk.gymsololeveling.core.domain.model.MuscleRank
import com.hunterxdk.gymsololeveling.core.domain.model.PlayerStats
import com.hunterxdk.gymsololeveling.core.domain.model.enums.BadgeTier
import com.hunterxdk.gymsololeveling.core.domain.model.enums.BadgeType
import com.hunterxdk.gymsololeveling.core.domain.model.enums.RankTier
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
        val threshold: Int,   // numeric value needed to unlock — drives progress bar
        val condition: (PlayerStats) -> Boolean,
    )

    val ACHIEVEMENT_DEFINITIONS: List<AchievementDef> = listOf(
        // MILESTONE — workout count
        AchievementDef("milestone_1", BadgeType.MILESTONE, 1, "First Steps", "Complete your first workout", 100, 1) { it.totalWorkouts >= 1 },
        AchievementDef("milestone_2", BadgeType.MILESTONE, 2, "Getting Serious", "Complete 10 workouts", 250, 10) { it.totalWorkouts >= 10 },
        AchievementDef("milestone_3", BadgeType.MILESTONE, 3, "Dedicated", "Complete 50 workouts", 500, 50) { it.totalWorkouts >= 50 },
        AchievementDef("milestone_4", BadgeType.MILESTONE, 4, "Century Club", "Complete 100 workouts", 1000, 100) { it.totalWorkouts >= 100 },
        AchievementDef("milestone_5", BadgeType.MILESTONE, 5, "Iron Will", "Complete 365 workouts", 2000, 365) { it.totalWorkouts >= 365 },

        // CONSISTENCY — streaks
        AchievementDef("streak_3",  BadgeType.CONSISTENCY, 1, "3 Day Streak",    "Work out 3 days in a row",   150, 3)   { it.currentStreak >= 3 },
        AchievementDef("streak_7",  BadgeType.CONSISTENCY, 2, "7 Day Streak",    "Work out 7 days in a row",   300, 7)   { it.currentStreak >= 7 },
        AchievementDef("streak_14", BadgeType.CONSISTENCY, 3, "2 Week Grind",    "Work out 14 days in a row",  600, 14)  { it.currentStreak >= 14 },
        AchievementDef("streak_30",  BadgeType.CONSISTENCY, 4, "Month Strong",     "Work out 30 days in a row",  1000, 30)  { it.currentStreak >= 30 },
        AchievementDef("streak_100", BadgeType.CONSISTENCY, 5, "Iron Discipline",  "Work out 100 days in a row", 3000, 100) { it.currentStreak >= 100 },

        // PR_HUNTER
        AchievementDef("pr_1",  BadgeType.PR_HUNTER, 1, "First PR",       "Set your first personal record", 200, 1)  { it.totalPRs >= 1 },
        AchievementDef("pr_10", BadgeType.PR_HUNTER, 2, "Record Breaker", "Set 10 personal records",        400, 10) { it.totalPRs >= 10 },
        AchievementDef("pr_50", BadgeType.PR_HUNTER, 3, "PR Machine",     "Set 50 personal records",        800, 50) { it.totalPRs >= 50 },

        // VOLUME
        AchievementDef("volume_10k",  BadgeType.VOLUME, 1, "10 Tonne Club",      "Move 10,000kg total volume",     200,  10_000)    { it.totalVolumeKg >= 10_000 },
        AchievementDef("volume_100k", BadgeType.VOLUME, 2, "100 Tonne Legend",   "Move 100,000kg total volume",    500,  100_000)   { it.totalVolumeKg >= 100_000 },
        AchievementDef("volume_1m",   BadgeType.VOLUME, 3, "Million Kilo Beast", "Move 1,000,000kg total volume",  2000, 1_000_000) { it.totalVolumeKg >= 1_000_000 },

        // EXPLORER
        AchievementDef("explorer_10",  BadgeType.EXPLORER, 1, "Exercise Explorer",    "Try 10 different exercises",  150, 10)  { it.uniqueExercisesCount >= 10 },
        AchievementDef("explorer_50",  BadgeType.EXPLORER, 2, "Movement Master",      "Try 50 different exercises",  400, 50)  { it.uniqueExercisesCount >= 50 },
        AchievementDef("explorer_100", BadgeType.EXPLORER, 3, "Exercise Encyclopedia","Try 100 different exercises", 800, 100) { it.uniqueExercisesCount >= 100 },

        // MUSCLE_MASTER — condition evaluated via muscleRanks in checkNewAchievements
        AchievementDef("bronze_all",  BadgeType.MUSCLE_MASTER, 1, "Bronze Body",       "Reach Bronze rank on all muscles",  500,  17) { false },
        AchievementDef("gold_all",    BadgeType.MUSCLE_MASTER, 2, "Golden Physique",   "Reach Gold rank on all muscles",    2000, 17) { false },
        AchievementDef("legend_any",  BadgeType.MUSCLE_MASTER, 3, "Legend Muscle",     "Reach Legend rank on any muscle",   5000, 1)  { false },

        // STRENGTH — player level milestones
        AchievementDef("level_10", BadgeType.STRENGTH, 1, "Level 10",  "Reach Player Level 10", 500,  10) { it.playerLevel >= 10 },
        AchievementDef("level_20", BadgeType.STRENGTH, 2, "Level 20",  "Reach Player Level 20", 1000, 20) { it.playerLevel >= 20 },
        AchievementDef("level_30", BadgeType.STRENGTH, 3, "Max Level", "Reach Player Level 30", 3000, 30) { it.playerLevel >= 30 },
    )

    fun checkNewAchievements(
        stats: PlayerStats,
        muscleRanks: List<MuscleRank>,
        existingAchievements: Set<String>,
    ): List<AchievementDef> {
        val newlyEarned = mutableListOf<AchievementDef>()

        ACHIEVEMENT_DEFINITIONS.forEach { def ->
            if (def.id in existingAchievements) return@forEach

            val earned = when (def.type) {
                BadgeType.MUSCLE_MASTER -> when (def.id) {
                    "bronze_all" -> muscleRanks.isNotEmpty() && muscleRanks.all { it.currentRank >= RankTier.BRONZE }
                    "gold_all"   -> muscleRanks.isNotEmpty() && muscleRanks.all { it.currentRank >= RankTier.GOLD }
                    "legend_any" -> muscleRanks.any { it.currentRank == RankTier.LEGEND }
                    else         -> false
                }
                else -> def.condition(stats)
            }

            if (earned) newlyEarned.add(def)
        }

        return newlyEarned
    }

    data class BadgeProgress(
        val type: BadgeType,
        val currentTier: BadgeTier?,
        val currentValue: Int,
        val nextThreshold: Int?,
        val progress: Float,
    )

    fun computeAllBadgeProgress(stats: PlayerStats): List<BadgeProgress> {
        val badgeTypes = BadgeType.entries.toList()
        return badgeTypes.map { type ->
            val tierDefs = ACHIEVEMENT_DEFINITIONS.filter { it.type == type }.sortedBy { it.tier }
            val currentDef = tierDefs.lastOrNull { it.condition(stats) }
            val nextDef = tierDefs.firstOrNull { !it.condition(stats) }

            val currentTier: BadgeTier? = when (currentDef?.tier) {
                1 -> BadgeTier.BRONZE
                2 -> BadgeTier.SILVER
                3 -> BadgeTier.GOLD
                else -> null
            }

            val currentValue = when (type) {
                BadgeType.MILESTONE -> stats.totalWorkouts
                BadgeType.CONSISTENCY -> stats.currentStreak
                BadgeType.PR_HUNTER -> stats.totalPRs
                BadgeType.VOLUME -> stats.totalVolumeKg.toInt()
                BadgeType.EXPLORER -> stats.uniqueExercisesCount
                BadgeType.STRENGTH -> stats.playerLevel
                BadgeType.MUSCLE_MASTER -> 0
                BadgeType.SPECIAL -> 0
            }

            val nextThreshold = nextDef?.threshold

            val progress = if (nextThreshold != null && nextThreshold > 0) {
                (currentValue.toFloat() / nextThreshold).coerceIn(0f, 1f)
            } else 1f

            BadgeProgress(
                type = type,
                currentTier = currentTier,
                currentValue = currentValue,
                nextThreshold = nextThreshold,
                progress = progress,
            )
        }
    }
}
