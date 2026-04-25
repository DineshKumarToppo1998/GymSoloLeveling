package com.hunterxdk.gymsololeveling.core.domain.model

import com.hunterxdk.gymsololeveling.core.domain.model.enums.WorkoutType

data class WorkoutSet(
    val id: String,
    val workoutExerciseId: String,
    val setNumber: Int,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val distanceKm: Double? = null,
    val inclinePercent: Double? = null,
    val resistanceLevel: Int? = null,
    val floors: Int? = null,
    val steps: Int? = null,
    val bandResistance: String? = null,
    val isPersonalRecord: Boolean = false,
    val completedAt: Long = System.currentTimeMillis(),
)

data class WorkoutExercise(
    val id: String,
    val sessionId: String,
    val exercise: Exercise,
    val orderIndex: Int,
    val sets: List<WorkoutSet> = emptyList(),
    val notes: String = "",
)

data class WorkoutSession(
    val id: String,
    val userId: String,
    val title: String,
    val workoutType: WorkoutType,
    val startedAt: Long,
    val endedAt: Long? = null,
    val exercises: List<WorkoutExercise> = emptyList(),
    val notes: String = "",
    val templateId: String? = null,
) {
    val durationSeconds: Long get() = ((endedAt ?: System.currentTimeMillis()) - startedAt) / 1000
    val totalVolumeKg: Double get() = exercises.flatMap { it.sets }
        .sumOf { (it.reps ?: 0) * (it.weightKg ?: 0.0) }
}

data class WorkoutTemplate(
    val id: String,
    val userId: String,
    val name: String,
    val workoutType: WorkoutType,
    val exercises: List<WorkoutExercise>,
    val estimatedDurationMinutes: Int = 45,
    val createdAt: Long,
    val lastUsedAt: Long? = null,
    val useCount: Int = 0,
)

data class WeightEntry(
    val id: String,
    val userId: String,
    val weightKg: Double,
    val recordedAt: Long,
    val notes: String = "",
)
