package com.hunterxdk.gymsololeveling.core.domain.model

import com.hunterxdk.gymsololeveling.core.domain.model.enums.BadgeType
import com.hunterxdk.gymsololeveling.core.domain.model.enums.ChallengeDifficulty
import com.hunterxdk.gymsololeveling.core.domain.model.enums.MuscleGroup
import com.hunterxdk.gymsololeveling.core.domain.model.enums.PlayerClass
import com.hunterxdk.gymsololeveling.core.domain.model.enums.RankSubTier
import com.hunterxdk.gymsololeveling.core.domain.model.enums.RankTier

data class MuscleRank(
    val muscleGroup: MuscleGroup,
    val totalXp: Int = 0,
    val currentRank: RankTier = RankTier.UNTRAINED,
    val currentSubRank: RankSubTier? = null,
    val xpToNextRank: Int = 50,
)

object XPThresholds {
    val SUB_RANK_THRESHOLDS: List<Triple<RankTier, RankSubTier?, Int>> = listOf(
        Triple(RankTier.BRONZE, RankSubTier.I, 50),
        Triple(RankTier.BRONZE, RankSubTier.II, 100),
        Triple(RankTier.BRONZE, RankSubTier.III, 150),
        Triple(RankTier.SILVER, RankSubTier.I, 200),
        Triple(RankTier.SILVER, RankSubTier.II, 500),
        Triple(RankTier.SILVER, RankSubTier.III, 750),
        Triple(RankTier.GOLD, RankSubTier.I, 1000),
        Triple(RankTier.GOLD, RankSubTier.II, 2000),
        Triple(RankTier.GOLD, RankSubTier.III, 3000),
        Triple(RankTier.PLATINUM, RankSubTier.I, 7500),
        Triple(RankTier.PLATINUM, RankSubTier.II, 17000),
        Triple(RankTier.PLATINUM, RankSubTier.III, 22000),
        Triple(RankTier.DIAMOND, null, 30000),
        Triple(RankTier.MASTER, null, 75000),
        Triple(RankTier.LEGEND, null, 200000),
    )

    val PLAYER_LEVEL_THRESHOLDS = listOf(
        0, 100, 300, 600, 1000, 1500, 2200, 3000, 4000, 5200,
        6600, 8200, 10000, 12000, 14500, 17000, 20000, 23500, 27500, 32000,
        37000, 42500, 48500, 55000, 62000, 69500, 77500, 86000, 95000, 105000,
    )
}

data class PlayerStats(
    val userId: String,
    val totalWorkouts: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val totalXp: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val playerLevel: Int = 1,
    val playerClass: PlayerClass = PlayerClass.BALANCE_WARRIOR,
    val overallRank: RankTier = RankTier.UNTRAINED,
    val lastWorkoutAt: Long? = null,
    val totalPRs: Int = 0,
    val uniqueExercisesCount: Int = 0,
)

data class User(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val playerClass: PlayerClass = PlayerClass.BALANCE_WARRIOR,
    val hasCompletedOnboarding: Boolean = false,
    val availableEquipment: List<String> = emptyList(),
    val injuries: List<String> = emptyList(),
    val priorityMuscles: List<String> = emptyList(),
    val preferredWorkoutDays: List<Int> = emptyList(),
)

data class Achievement(
    val id: String,
    val type: BadgeType,
    val tier: Int,
    val earnedAt: Long?,
    val isClaimed: Boolean = false,
)

data class Challenge(
    val id: String,
    val name: String,
    val description: String,
    val targetValue: Double,
    val currentValue: Double = 0.0,
    val xpReward: Int,
    val expiresAt: Long,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val difficulty: ChallengeDifficulty,
)
