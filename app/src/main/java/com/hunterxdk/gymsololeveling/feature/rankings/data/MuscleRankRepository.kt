package com.hunterxdk.gymsololeveling.feature.rankings.data

import com.hunterxdk.gymsololeveling.core.data.local.dao.MuscleRankDao
import com.hunterxdk.gymsololeveling.core.domain.model.MuscleRank
import com.hunterxdk.gymsololeveling.core.domain.model.enums.MuscleGroup
import com.hunterxdk.gymsololeveling.core.domain.model.enums.RankSubTier
import com.hunterxdk.gymsololeveling.core.domain.model.enums.RankTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MuscleRankRepository @Inject constructor(
    private val muscleRankDao: MuscleRankDao,
) {
    val allRanks: Flow<List<MuscleRank>> = muscleRankDao.observeAll().map { entities ->
        MuscleGroup.entries.map { muscle ->
            val entity = entities.firstOrNull { it.muscleGroup.equals(muscle.name, ignoreCase = true) }
            MuscleRank(
                muscleGroup = muscle,
                totalXp = entity?.totalXp ?: 0,
                currentRank = entity?.currentRank
                    ?.let { runCatching { RankTier.valueOf(it) }.getOrDefault(RankTier.UNTRAINED) }
                    ?: RankTier.UNTRAINED,
                currentSubRank = entity?.currentSubRank
                    ?.let { runCatching { RankSubTier.valueOf(it) }.getOrNull() },
                xpToNextRank = entity?.xpToNextRank ?: 50,
            )
        }
    }
}
