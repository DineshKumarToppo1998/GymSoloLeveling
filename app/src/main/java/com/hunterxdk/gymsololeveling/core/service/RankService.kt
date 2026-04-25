package com.hunterxdk.gymsololeveling.core.service

import androidx.compose.ui.graphics.Color
import com.hunterxdk.gymsololeveling.core.domain.model.MuscleRank
import com.hunterxdk.gymsololeveling.core.domain.model.XPThresholds
import com.hunterxdk.gymsololeveling.core.domain.model.enums.RankSubTier
import com.hunterxdk.gymsololeveling.core.domain.model.enums.RankTier
import com.hunterxdk.gymsololeveling.ui.theme.RankBronzeColor
import com.hunterxdk.gymsololeveling.ui.theme.RankDiamondColor
import com.hunterxdk.gymsololeveling.ui.theme.RankGoldColor
import com.hunterxdk.gymsololeveling.ui.theme.RankLegendColor
import com.hunterxdk.gymsololeveling.ui.theme.RankMasterColor
import com.hunterxdk.gymsololeveling.ui.theme.RankPlatinumColor
import com.hunterxdk.gymsololeveling.ui.theme.RankSilverColor
import com.hunterxdk.gymsololeveling.ui.theme.RankUntrainedColor

object RankService {

    data class MuscleRankResult(
        val rank: RankTier,
        val subRank: RankSubTier?,
        val xpToNext: Int,
    )

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

    fun getOverallRank(muscleRanks: List<MuscleRank>): RankTier {
        if (muscleRanks.isEmpty()) return RankTier.UNTRAINED
        val avgXp = muscleRanks.sumOf { it.totalXp } / muscleRanks.size
        return calculateRankFromXP(avgXp).rank
    }

    fun getRankDisplayName(rank: RankTier, subRank: RankSubTier?): String = buildString {
        append(rank.name.lowercase().replaceFirstChar { it.uppercase() })
        subRank?.let { append(" ${it.name}") }
    }

    fun getRankColor(rank: RankTier): Color = when (rank) {
        RankTier.UNTRAINED -> RankUntrainedColor
        RankTier.BRONZE    -> RankBronzeColor
        RankTier.SILVER    -> RankSilverColor
        RankTier.GOLD      -> RankGoldColor
        RankTier.PLATINUM  -> RankPlatinumColor
        RankTier.DIAMOND   -> RankDiamondColor
        RankTier.MASTER    -> RankMasterColor
        RankTier.LEGEND    -> RankLegendColor
    }
}
