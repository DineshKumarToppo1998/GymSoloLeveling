package com.hunterxdk.gymsololeveling.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hunterxdk.gymsololeveling.core.service.StreakService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import java.time.LocalTime
import java.util.concurrent.TimeUnit

@HiltWorker
class StreakCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val streakService: StreakService,
    private val firestore: FirebaseFirestore,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        return try {
            val workouts = firestore.collection("users").document(uid)
                .collection("workouts")
                .get().await()

            val dates = workouts.documents.mapNotNull { it.getLong("startedAt") }
            val streakResult = streakService.calculateStreak(dates)

            firestore.collection("users").document(uid).update(
                mapOf(
                    "currentStreak" to streakResult.currentStreak,
                    "longestStreak" to streakResult.longestStreak,
                )
            ).await()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "streak_check_worker"

        fun schedule(context: Context) {
            val now = LocalTime.now()
            // Minutes remaining until next midnight
            val delayMinutes = (24 * 60) - now.hour * 60 - now.minute

            val request = PeriodicWorkRequestBuilder<StreakCheckWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayMinutes.toLong(), TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
