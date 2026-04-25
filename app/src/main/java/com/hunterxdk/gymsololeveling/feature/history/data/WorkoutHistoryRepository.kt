package com.hunterxdk.gymsololeveling.feature.history.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hunterxdk.gymsololeveling.core.domain.model.WorkoutExercise
import com.hunterxdk.gymsololeveling.core.domain.model.enums.WorkoutType
import com.hunterxdk.gymsololeveling.feature.exercise.data.ExerciseRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutHistoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val exerciseRepository: ExerciseRepository,
) {
    fun getWorkoutHistory(): Flow<List<WorkoutSummary>> = callbackFlow {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: run { close(); return@callbackFlow }

        val listener = firestore.collection("users").document(uid)
            .collection("workouts")
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val summaries = snap?.documents?.mapNotNull { doc ->
                    WorkoutSummary(
                        id = doc.id,
                        title = doc.getString("title") ?: "Workout",
                        workoutType = runCatching {
                            WorkoutType.valueOf(doc.getString("workoutType") ?: "")
                        }.getOrDefault(WorkoutType.CUSTOM),
                        startedAt = doc.getLong("startedAt") ?: 0L,
                        endedAt = doc.getLong("endedAt"),
                        totalXp = doc.getLong("totalXp")?.toInt() ?: 0,
                        exerciseCount = doc.getLong("exerciseCount")?.toInt() ?: 0,
                        totalSets = doc.getLong("totalSets")?.toInt() ?: 0,
                        totalVolumeKg = doc.getDouble("totalVolumeKg") ?: 0.0,
                        prCount = doc.getLong("prCount")?.toInt() ?: 0,
                    )
                } ?: emptyList()
                trySend(summaries)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getWorkoutDetail(workoutId: String): WorkoutDetailData? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null

        val doc = firestore.collection("users").document(uid)
            .collection("workouts").document(workoutId)
            .get().await()

        if (!doc.exists()) return null

        return WorkoutDetailData(
            id = doc.id,
            title = doc.getString("title") ?: "Workout",
            workoutType = runCatching {
                WorkoutType.valueOf(doc.getString("workoutType") ?: "")
            }.getOrDefault(WorkoutType.CUSTOM),
            startedAt = doc.getLong("startedAt") ?: 0L,
            endedAt = doc.getLong("endedAt"),
            totalXp = doc.getLong("totalXp")?.toInt() ?: 0,
            totalVolumeKg = doc.getDouble("totalVolumeKg") ?: 0.0,
            prCount = doc.getLong("prCount")?.toInt() ?: 0,
            exercises = emptyList(),
        )
    }

    fun getWorkoutDates(): Flow<List<LocalDate>> = callbackFlow {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: run { close(); return@callbackFlow }

        val zone = ZoneId.systemDefault()
        val listener = firestore.collection("users").document(uid)
            .collection("workouts")
            .addSnapshotListener { snap, _ ->
                val dates = snap?.documents
                    ?.mapNotNull { doc -> doc.getLong("startedAt") }
                    ?.map { millis -> Instant.ofEpochMilli(millis).atZone(zone).toLocalDate() }
                    ?.distinct()
                    ?: emptyList()
                trySend(dates)
            }
        awaitClose { listener.remove() }
    }
}

data class WorkoutSummary(
    val id: String,
    val title: String,
    val workoutType: WorkoutType,
    val startedAt: Long,
    val endedAt: Long?,
    val totalXp: Int,
    val exerciseCount: Int,
    val totalSets: Int,
    val totalVolumeKg: Double,
    val prCount: Int,
)

data class WorkoutDetailData(
    val id: String,
    val title: String,
    val workoutType: WorkoutType,
    val startedAt: Long,
    val endedAt: Long?,
    val totalXp: Int,
    val totalVolumeKg: Double,
    val prCount: Int,
    val exercises: List<WorkoutExercise>,
)
