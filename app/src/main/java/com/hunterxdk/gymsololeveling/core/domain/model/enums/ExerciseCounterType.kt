package com.hunterxdk.gymsololeveling.core.domain.model.enums

enum class ExerciseCounterType {
    REPS_AND_WEIGHT,
    REPS,
    REPS_ONLY,
    BODYWEIGHT,
    TIME,
    TIME_ONLY,
    TIME_AND_SETS,
    TIME_AND_DISTANCE_AND_INCLINE,
    TIME_AND_DISTANCE_AND_RESISTANCE,
    TIME_AND_FLOORS_AND_STEPS,
    RESISTANCE_BAND_STRENGTH,
    RESISTANCE_BAND_STRENGTH_AND_TIME,
}

fun String.toExerciseCounterType(): ExerciseCounterType =
    ExerciseCounterType.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }
        ?: ExerciseCounterType.REPS_AND_WEIGHT
