package com.hunterxdk.gymsololeveling.core.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "active_workout_sessions")
data class ActiveWorkoutSessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val workoutType: String,
    val startedAt: Long,
    val notes: String = "",
    val templateId: String? = null,
)

@Entity(
    tableName = "active_workout_exercises",
    foreignKeys = [ForeignKey(
        entity = ActiveWorkoutSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId")],
)
data class ActiveWorkoutExerciseEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val exerciseName: String,
    val orderIndex: Int,
    val notes: String = "",
)

@Entity(
    tableName = "active_workout_sets",
    foreignKeys = [ForeignKey(
        entity = ActiveWorkoutExerciseEntity::class,
        parentColumns = ["id"],
        childColumns = ["workoutExerciseId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("workoutExerciseId")],
)
data class ActiveWorkoutSetEntity(
    @PrimaryKey val id: String,
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
    val completedAt: Long,
)

@Entity(tableName = "muscle_ranks")
data class MuscleRankEntity(
    @PrimaryKey val muscleGroup: String,
    val totalXp: Int = 0,
    val currentRank: String = "UNTRAINED",
    val currentSubRank: String? = null,
    val xpToNextRank: Int = 50,
    val updatedAt: Long,
) {
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "muscleGroup" to muscleGroup,
        "totalXp" to totalXp,
        "currentRank" to currentRank,
        "currentSubRank" to currentSubRank,
        "xpToNextRank" to xpToNextRank,
        "updatedAt" to updatedAt,
    )
}

@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val weightKg: Double,
    val loggedAt: Long,
    val notes: String = "",
)

@Entity(tableName = "workout_templates")
data class WorkoutTemplateEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val workoutType: String,
    val exercisesJson: String,
    val estimatedDurationMinutes: Int = 45,
    val createdAt: Long,
    val lastUsedAt: Long? = null,
    val useCount: Int = 0,
)

// Relation helpers
data class ActiveWorkoutSessionWithExercises(
    @Embedded val session: ActiveWorkoutSessionEntity,
    @Relation(
        entity = ActiveWorkoutExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "sessionId",
    )
    val exercises: List<ActiveWorkoutExerciseWithSets>,
)

data class ActiveWorkoutExerciseWithSets(
    @Embedded val exercise: ActiveWorkoutExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutExerciseId")
    val sets: List<ActiveWorkoutSetEntity>,
)
