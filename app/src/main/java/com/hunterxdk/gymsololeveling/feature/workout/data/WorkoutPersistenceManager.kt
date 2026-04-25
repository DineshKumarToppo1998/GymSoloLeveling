package com.hunterxdk.gymsololeveling.feature.workout.data

import com.hunterxdk.gymsololeveling.core.data.local.dao.ActiveWorkoutDao
import com.hunterxdk.gymsololeveling.core.data.local.entity.ActiveWorkoutExerciseEntity
import com.hunterxdk.gymsololeveling.core.data.local.entity.ActiveWorkoutSessionEntity
import com.hunterxdk.gymsololeveling.core.data.local.entity.ActiveWorkoutSessionWithExercises
import com.hunterxdk.gymsololeveling.core.data.local.entity.ActiveWorkoutSetEntity
import com.hunterxdk.gymsololeveling.core.domain.model.WorkoutExercise
import com.hunterxdk.gymsololeveling.core.domain.model.WorkoutSession
import com.hunterxdk.gymsololeveling.core.domain.model.WorkoutSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutPersistenceManager @Inject constructor(
    private val activeWorkoutDao: ActiveWorkoutDao,
) {
    suspend fun startSession(session: WorkoutSession) {
        activeWorkoutDao.insertSession(session.toSessionEntity())
    }

    suspend fun addExercise(exercise: WorkoutExercise) {
        activeWorkoutDao.insertExercise(exercise.toExerciseEntity())
    }

    suspend fun persistSet(set: WorkoutSet) {
        activeWorkoutDao.insertSet(set.toSetEntity())
    }

    suspend fun updateSet(set: WorkoutSet) {
        activeWorkoutDao.updateSet(set.toSetEntity())
    }

    suspend fun deleteSet(setId: String) {
        activeWorkoutDao.deleteSet(setId)
    }

    suspend fun clearSession() {
        activeWorkoutDao.clearAllSessions()
    }

    suspend fun getResumableSession(): ActiveWorkoutSessionWithExercises? =
        activeWorkoutDao.getActiveSession()
}

private fun WorkoutSession.toSessionEntity() = ActiveWorkoutSessionEntity(
    id = id,
    userId = userId,
    title = title,
    workoutType = workoutType.name,
    startedAt = startedAt,
    notes = notes,
    templateId = templateId,
)

private fun WorkoutExercise.toExerciseEntity() = ActiveWorkoutExerciseEntity(
    id = id,
    sessionId = sessionId,
    exerciseId = exercise.id,
    exerciseName = exercise.name,
    orderIndex = orderIndex,
    notes = notes,
)

private fun WorkoutSet.toSetEntity() = ActiveWorkoutSetEntity(
    id = id,
    workoutExerciseId = workoutExerciseId,
    setNumber = setNumber,
    reps = reps,
    weightKg = weightKg,
    durationSeconds = durationSeconds,
    distanceKm = distanceKm,
    inclinePercent = inclinePercent,
    resistanceLevel = resistanceLevel,
    floors = floors,
    steps = steps,
    bandResistance = bandResistance,
    isPersonalRecord = isPersonalRecord,
    completedAt = completedAt,
)
