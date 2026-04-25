package com.hunterxdk.gymsololeveling.core.service

import com.hunterxdk.gymsololeveling.core.domain.model.enums.MuscleGroup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecoveryService @Inject constructor() {

    // Recovery time in hours per muscle group (based on volume sensitivity)
    private val RECOVERY_HOURS = mapOf(
        MuscleGroup.CHEST          to 48,
        MuscleGroup.FRONT_SHOULDERS to 36,
        MuscleGroup.BACK_SHOULDERS  to 36,
        MuscleGroup.BICEPS         to 36,
        MuscleGroup.TRICEPS        to 36,
        MuscleGroup.FOREARMS       to 24,
        MuscleGroup.UPPER_BACK     to 48,
        MuscleGroup.MIDDLE_BACK    to 48,
        MuscleGroup.LOWER_BACK     to 72,
        MuscleGroup.LATS           to 48,
        MuscleGroup.TRAPS          to 36,
        MuscleGroup.ABS            to 24,
        MuscleGroup.OBLIQUES       to 24,
        MuscleGroup.QUADRICEPS     to 72,
        MuscleGroup.HAMSTRINGS     to 72,
        MuscleGroup.GLUTES         to 48,
        MuscleGroup.CALVES         to 24,
    )

    data class MuscleRecovery(
        val muscleGroup: MuscleGroup,
        val lastTrainedAt: Long?,
        val recoveryHours: Int,
        val recoveryPercent: Float, // 0.0 = just trained, 1.0 = fully recovered
        val isReady: Boolean,
    )

    fun calculateRecovery(
        lastTrainedDates: Map<MuscleGroup, Long>,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<MuscleRecovery> {
        return MuscleGroup.entries.map { muscle ->
            val lastTrained = lastTrainedDates[muscle]
            val recoveryHours = RECOVERY_HOURS[muscle] ?: 48

            if (lastTrained == null) {
                MuscleRecovery(muscle, null, recoveryHours, 1.0f, true)
            } else {
                val hoursElapsed = (nowMillis - lastTrained) / (1000f * 60f * 60f)
                val recoveryPercent = (hoursElapsed / recoveryHours).coerceIn(0f, 1f)
                MuscleRecovery(
                    muscleGroup = muscle,
                    lastTrainedAt = lastTrained,
                    recoveryHours = recoveryHours,
                    recoveryPercent = recoveryPercent,
                    isReady = recoveryPercent >= 1.0f,
                )
            }
        }
    }

    fun getReadyMuscles(lastTrainedDates: Map<MuscleGroup, Long>): List<MuscleGroup> =
        calculateRecovery(lastTrainedDates).filter { it.isReady }.map { it.muscleGroup }
}
