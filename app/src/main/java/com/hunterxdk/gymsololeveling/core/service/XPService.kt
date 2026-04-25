package com.hunterxdk.gymsololeveling.core.service

import com.hunterxdk.gymsololeveling.core.domain.model.Exercise
import com.hunterxdk.gymsololeveling.core.domain.model.WorkoutSession
import com.hunterxdk.gymsololeveling.core.domain.model.WorkoutSet
import com.hunterxdk.gymsololeveling.core.domain.model.counterType
import com.hunterxdk.gymsololeveling.core.domain.model.enums.ExerciseCounterType

object XPService {

    fun calculateSetXP(set: WorkoutSet, exercise: Exercise): Int {
        val base = when (exercise.counterType()) {
            ExerciseCounterType.REPS_AND_WEIGHT -> {
                val volume = (set.reps ?: 0) * (set.weightKg ?: 0.0)
                when {
                    volume >= 2000 -> 50
                    volume >= 1000 -> 40
                    volume >= 500  -> 30
                    volume >= 100  -> 20
                    else           -> 10
                }
            }
            ExerciseCounterType.BODYWEIGHT,
            ExerciseCounterType.REPS,
            ExerciseCounterType.REPS_ONLY -> when {
                (set.reps ?: 0) >= 20 -> 30
                (set.reps ?: 0) >= 10 -> 20
                else -> 10
            }
            ExerciseCounterType.TIME,
            ExerciseCounterType.TIME_ONLY,
            ExerciseCounterType.TIME_AND_SETS ->
                ((set.durationSeconds ?: 0) / 60.0 * 5).coerceIn(10.0, 50.0).toInt()
            ExerciseCounterType.TIME_AND_DISTANCE_AND_INCLINE,
            ExerciseCounterType.TIME_AND_DISTANCE_AND_RESISTANCE -> {
                val distanceBonus = ((set.distanceKm ?: 0.0) * 10).toInt()
                (((set.durationSeconds ?: 0) / 60.0 * 5) + distanceBonus).coerceIn(10.0, 50.0).toInt()
            }
            ExerciseCounterType.TIME_AND_FLOORS_AND_STEPS -> {
                val floorsBonus = (set.floors ?: 0) * 2
                ((set.durationSeconds ?: 0) / 60.0 * 3 + floorsBonus).coerceIn(10.0, 50.0).toInt()
            }
            ExerciseCounterType.RESISTANCE_BAND_STRENGTH -> when {
                (set.reps ?: 0) >= 20 -> 25
                (set.reps ?: 0) >= 10 -> 15
                else -> 10
            }
            ExerciseCounterType.RESISTANCE_BAND_STRENGTH_AND_TIME ->
                ((set.durationSeconds ?: 0) / 60.0 * 5).coerceIn(10.0, 40.0).toInt()
        }

        val difficultyMult = 1.0 + (exercise.difficulty - 1) * 0.2
        val prBonus = if (set.isPersonalRecord) 25 else 0
        return (base * difficultyMult).toInt() + prBonus
    }

    fun calculateWorkoutCompletionBonus(session: WorkoutSession): Int {
        val exerciseCount = session.exercises.size
        val totalSets = session.exercises.sumOf { it.sets.size }
        val durationMinutes = session.durationSeconds / 60
        return when {
            totalSets >= 30 && durationMinutes >= 60 -> 200
            totalSets >= 20 && durationMinutes >= 45 -> 150
            totalSets >= 15 && durationMinutes >= 30 -> 100
            totalSets >= 10                          -> 75
            exerciseCount >= 5                       -> 60
            exerciseCount >= 3                       -> 50
            else                                     -> 25
        }
    }

    fun distributeXPToMuscles(set: WorkoutSet, exercise: Exercise, setXP: Int): Map<String, Int> {
        val distribution = mutableMapOf<String, Int>()
        val primaryMuscles = exercise.primaryMuscles
        val secondaryMuscles = exercise.secondaryMuscles

        if (primaryMuscles.isNotEmpty()) {
            val primaryXpEach = (setXP * 0.7 / primaryMuscles.size).toInt()
            primaryMuscles.forEach { muscle ->
                distribution[muscle] = (distribution[muscle] ?: 0) + primaryXpEach
            }
        }
        if (secondaryMuscles.isNotEmpty()) {
            val secondaryXpEach = (setXP * 0.3 / secondaryMuscles.size).toInt()
            secondaryMuscles.forEach { muscle ->
                distribution[muscle] = (distribution[muscle] ?: 0) + secondaryXpEach
            }
        }
        if (primaryMuscles.isEmpty() && secondaryMuscles.isEmpty()) {
            distribution["unknown"] = setXP
        }
        return distribution
    }

    data class WorkoutXPResult(
        val totalXp: Int,
        val completionBonus: Int,
        val muscleXpDistribution: Map<String, Int>,
        val prCount: Int,
    )

    fun calculateWorkoutXP(session: WorkoutSession, exercises: Map<String, Exercise>): WorkoutXPResult {
        val muscleXpMap = mutableMapOf<String, Int>()
        var totalSetXP = 0
        var prCount = 0

        session.exercises.forEach { workoutExercise ->
            val exercise = exercises[workoutExercise.exercise.id] ?: workoutExercise.exercise
            workoutExercise.sets.forEach { set ->
                val setXP = calculateSetXP(set, exercise)
                totalSetXP += setXP
                if (set.isPersonalRecord) prCount++
                distributeXPToMuscles(set, exercise, setXP).forEach { (muscle, xp) ->
                    muscleXpMap[muscle] = (muscleXpMap[muscle] ?: 0) + xp
                }
            }
        }

        val completionBonus = calculateWorkoutCompletionBonus(session)
        return WorkoutXPResult(
            totalXp = totalSetXP + completionBonus,
            completionBonus = completionBonus,
            muscleXpDistribution = muscleXpMap,
            prCount = prCount,
        )
    }
}
