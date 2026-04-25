package com.hunterxdk.gymsololeveling.feature.templates.data

import com.hunterxdk.gymsololeveling.core.data.local.entity.WorkoutTemplateEntity
import com.hunterxdk.gymsololeveling.core.domain.model.Exercise
import com.hunterxdk.gymsololeveling.core.domain.model.WorkoutExercise
import com.hunterxdk.gymsololeveling.core.domain.model.WorkoutTemplate
import com.hunterxdk.gymsololeveling.core.domain.model.enums.WorkoutType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TemplateExerciseRef(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val orderIndex: Int,
)

fun WorkoutTemplateEntity.toDomain(json: Json): WorkoutTemplate {
    val exerciseRefs = runCatching {
        json.decodeFromString<List<TemplateExerciseRef>>(exercisesJson)
    }.getOrDefault(emptyList())

    return WorkoutTemplate(
        id = id,
        userId = userId,
        name = name,
        workoutType = runCatching { WorkoutType.valueOf(workoutType) }.getOrDefault(WorkoutType.CUSTOM),
        exercises = exerciseRefs.map { ref ->
            WorkoutExercise(
                id = ref.id,
                sessionId = id,
                exercise = Exercise(
                    id = ref.exerciseId,
                    name = ref.exerciseName,
                    mainEquipment = "",
                    otherEquipment = emptyList(),
                    primaryMuscles = emptyList(),
                    secondaryMuscles = emptyList(),
                    splitCategories = emptyList(),
                    exerciseCounterType = "REPS_WEIGHT",
                    exerciseMechanics = "",
                    difficulty = 1,
                    instructions = emptyList(),
                    tips = emptyList(),
                    benefits = emptyList(),
                    breathingInstructions = "",
                    keywords = emptyList(),
                    metabolicEquivalent = 0.0,
                    repSupplement = null,
                    youtubeVideoId = null,
                ),
                orderIndex = ref.orderIndex,
            )
        },
        estimatedDurationMinutes = estimatedDurationMinutes,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
        useCount = useCount,
    )
}

fun WorkoutTemplate.toEntity(json: Json): WorkoutTemplateEntity {
    val refs = exercises.mapIndexed { index, we ->
        TemplateExerciseRef(
            id = we.id,
            exerciseId = we.exercise.id,
            exerciseName = we.exercise.name,
            orderIndex = index,
        )
    }
    return WorkoutTemplateEntity(
        id = id,
        userId = userId,
        name = name,
        workoutType = workoutType.name,
        exercisesJson = json.encodeToString(refs),
        estimatedDurationMinutes = estimatedDurationMinutes,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
        useCount = useCount,
    )
}
