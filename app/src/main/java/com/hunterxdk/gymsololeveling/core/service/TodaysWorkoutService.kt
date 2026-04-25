package com.hunterxdk.gymsololeveling.core.service

import com.hunterxdk.gymsololeveling.core.domain.model.MuscleRank
import com.hunterxdk.gymsololeveling.core.domain.model.enums.MuscleGroup
import com.hunterxdk.gymsololeveling.core.domain.model.enums.WorkoutType
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodaysWorkoutService @Inject constructor(
    private val recoveryService: RecoveryService,
) {
    data class WorkoutRecommendation(
        val workoutType: WorkoutType,
        val targetMuscles: List<MuscleGroup>,
        val suggestedExerciseIds: List<String>,
        val estimatedDurationMinutes: Int,
        val reason: String,
    )

    fun getRecommendation(
        muscleRanks: List<MuscleRank>,
        lastTrainedDates: Map<MuscleGroup, Long>,
        priorityMuscles: List<MuscleGroup>,
        availableEquipment: List<String>,
        preferredDays: List<DayOfWeek>,
        today: LocalDate = LocalDate.now(),
    ): WorkoutRecommendation? {
        val todayDow = today.dayOfWeek
        val isPreferredDay = preferredDays.isEmpty() || todayDow in preferredDays

        if (!isPreferredDay) return null

        val recoveries = recoveryService.calculateRecovery(lastTrainedDates)
        val readyMuscles = recoveries.filter { it.isReady }.map { it.muscleGroup }

        val priorityReady = priorityMuscles.filter { it in readyMuscles }
        val targetMuscles = if (priorityReady.isNotEmpty()) {
            priorityReady.take(4)
        } else {
            readyMuscles
                .sortedBy { muscle -> muscleRanks.firstOrNull { it.muscleGroup == muscle }?.totalXp ?: 0 }
                .take(4)
        }

        if (targetMuscles.isEmpty()) {
            return WorkoutRecommendation(
                workoutType = WorkoutType.FULL_BODY,
                targetMuscles = MuscleGroup.entries.take(4),
                suggestedExerciseIds = emptyList(),
                estimatedDurationMinutes = 30,
                reason = "Active recovery day — light movement recommended",
            )
        }

        val workoutType = inferWorkoutType(targetMuscles)
        return WorkoutRecommendation(
            workoutType = workoutType,
            targetMuscles = targetMuscles,
            suggestedExerciseIds = emptyList(),
            estimatedDurationMinutes = 45,
            reason = buildReason(targetMuscles, priorityReady.isNotEmpty()),
        )
    }

    private fun inferWorkoutType(muscles: List<MuscleGroup>): WorkoutType {
        val hasChest = MuscleGroup.CHEST in muscles
        val hasShoulder = MuscleGroup.FRONT_SHOULDERS in muscles || MuscleGroup.BACK_SHOULDERS in muscles
        val hasTriceps = MuscleGroup.TRICEPS in muscles
        val hasBack = MuscleGroup.UPPER_BACK in muscles || MuscleGroup.LATS in muscles
        val hasBiceps = MuscleGroup.BICEPS in muscles
        val hasLegs = MuscleGroup.QUADRICEPS in muscles || MuscleGroup.HAMSTRINGS in muscles || MuscleGroup.GLUTES in muscles

        return when {
            hasChest && hasShoulder && hasTriceps -> WorkoutType.PUSH
            hasBack && hasBiceps -> WorkoutType.PULL
            hasLegs -> WorkoutType.LOWER_BODY
            muscles.size >= 4 -> WorkoutType.FULL_BODY
            else -> WorkoutType.CUSTOM
        }
    }

    private fun buildReason(muscles: List<MuscleGroup>, isPriority: Boolean): String {
        val muscleNames = muscles.take(3).joinToString(", ") { it.displayName }
        return if (isPriority) {
            "Your priority muscles are ready: $muscleNames"
        } else {
            "$muscleNames are fully recovered and ready to train"
        }
    }
}
