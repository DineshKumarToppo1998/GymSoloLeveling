package com.hunterxdk.gymsololeveling.feature.profile.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hunterxdk.gymsololeveling.core.domain.model.PlayerStats
import com.hunterxdk.gymsololeveling.core.domain.model.enums.PlayerClass
import com.hunterxdk.gymsololeveling.core.domain.model.enums.RankTier
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerStatsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    val stats: Flow<PlayerStats?> = callbackFlow {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: run { trySend(null); awaitClose {}; return@callbackFlow }

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
                        playerClass = runCatching {
                            PlayerClass.valueOf(doc.getString("playerClass") ?: "")
                        }.getOrDefault(PlayerClass.BALANCE_WARRIOR),
                        overallRank = RankTier.UNTRAINED,
                        lastWorkoutAt = doc.getLong("lastWorkoutAt"),
                        totalPRs = doc.getLong("totalPRs")?.toInt() ?: 0,
                        uniqueExercisesCount = doc.getLong("uniqueExercisesCount")?.toInt() ?: 0,
                    )
                }
                trySend(stats)
            }
        awaitClose { listener.remove() }
    }

    val currentStreak: Flow<Int> = stats.map { it?.currentStreak ?: 0 }
}
