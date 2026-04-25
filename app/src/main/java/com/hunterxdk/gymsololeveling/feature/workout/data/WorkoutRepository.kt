package com.hunterxdk.gymsololeveling.feature.workout.data

import com.google.firebase.firestore.FirebaseFirestore
import com.hunterxdk.gymsololeveling.core.data.local.dao.MuscleRankDao
import com.hunterxdk.gymsololeveling.core.data.local.entity.MuscleRankEntity
import com.hunterxdk.gymsololeveling.core.domain.SessionManager
import com.hunterxdk.gymsololeveling.core.domain.model.WorkoutSession
import com.hunterxdk.gymsololeveling.core.service.RankService
import com.hunterxdk.gymsololeveling.core.service.XPService
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val muscleRankDao: MuscleRankDao,
    private val persistenceManager: WorkoutPersistenceManager,
    private val sessionManager: SessionManager,
) {
    suspend fun saveCompletedWorkout(
        session: WorkoutSession,
        xpResult: XPService.WorkoutXPResult,
    ): Result<Unit> = runCatching {
        val uid = sessionManager.currentUid

        // 1. Update muscle ranks in Room (always, guest or authenticated)
        xpResult.muscleXpDistribution.forEach { (muscleStr, xp) ->
            val existing = muscleRankDao.get(muscleStr)
            val newTotalXp = (existing?.totalXp ?: 0) + xp
            val rankResult = RankService.calculateRankFromXP(newTotalXp)
            muscleRankDao.upsert(
                MuscleRankEntity(
                    muscleGroup = muscleStr,
                    totalXp = newTotalXp,
                    currentRank = rankResult.rank.name,
                    currentSubRank = rankResult.subRank?.name,
                    xpToNextRank = rankResult.xpToNext,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }

        // 2. Firestore sync — authenticated users only
        if (sessionManager.shouldSync && uid != null) {
            val endedAt = System.currentTimeMillis()

            val workoutData = mapOf(
                "id" to session.id,
                "userId" to uid,
                "title" to session.title,
                "workoutType" to session.workoutType.name,
                "startedAt" to session.startedAt,
                "endedAt" to endedAt,
                "totalXp" to xpResult.totalXp,
                "completionBonus" to xpResult.completionBonus,
                "prCount" to xpResult.prCount,
                "exerciseCount" to session.exercises.size,
                "totalSets" to session.exercises.sumOf { it.sets.size },
                "totalVolumeKg" to session.totalVolumeKg,
            )

            firestore.collection("users").document(uid)
                .collection("workouts").document(session.id)
                .set(workoutData).await()

            val statsRef = firestore.collection("users").document(uid)
            firestore.runTransaction { tx ->
                val snap = tx.get(statsRef)
                val currentTotalXp = snap.getLong("totalXp")?.toInt() ?: 0
                val currentWorkouts = snap.getLong("totalWorkouts")?.toInt() ?: 0
                val currentPRs = snap.getLong("totalPRs")?.toInt() ?: 0
                val newTotalXp = currentTotalXp + xpResult.totalXp
                tx.update(
                    statsRef, mapOf(
                        "totalXp" to newTotalXp,
                        "totalWorkouts" to (currentWorkouts + 1),
                        "totalPRs" to (currentPRs + xpResult.prCount),
                        "playerLevel" to RankService.calculatePlayerLevel(newTotalXp),
                        "lastWorkoutAt" to endedAt,
                    )
                )
            }.await()
        }

        // 3. Clear crash recovery state
        persistenceManager.clearSession()
    }
}
