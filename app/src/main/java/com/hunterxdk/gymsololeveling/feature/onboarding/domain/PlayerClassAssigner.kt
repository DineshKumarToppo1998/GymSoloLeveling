package com.hunterxdk.gymsololeveling.feature.onboarding.domain

import com.hunterxdk.gymsololeveling.core.domain.model.enums.PlayerClass

object PlayerClassAssigner {

    fun assign(answers: Map<Int, String>): PlayerClass {
        val goal      = answers[0] ?: ""
        val style     = answers[3] ?: ""
        val physique  = answers[5] ?: ""
        val motive    = answers[7] ?: ""
        val equipment = answers[4] ?: ""

        val scores = mutableMapOf<PlayerClass, Int>()

        fun add(cls: PlayerClass, pts: Int) { scores[cls] = (scores[cls] ?: 0) + pts }

        when (goal) {
            "build_muscle"   -> { add(PlayerClass.MASS_BUILDER, 3); add(PlayerClass.STRENGTH_SEEKER, 1) }
            "get_stronger"   -> { add(PlayerClass.STRENGTH_SEEKER, 3); add(PlayerClass.MASS_BUILDER, 1) }
            "lose_fat"       -> { add(PlayerClass.ENDURANCE_HUNTER, 3); add(PlayerClass.BALANCE_WARRIOR, 1) }
            "improve_health" -> { add(PlayerClass.RECOVERY_SPECIALIST, 3); add(PlayerClass.BALANCE_WARRIOR, 2) }
        }

        when (style) {
            "powerlifting" -> add(PlayerClass.STRENGTH_SEEKER, 3)
            "hypertrophy"  -> add(PlayerClass.MASS_BUILDER, 3)
            "hiit"         -> add(PlayerClass.ENDURANCE_HUNTER, 3)
            "functional"   -> add(PlayerClass.BALANCE_WARRIOR, 3)
        }

        when (physique) {
            "lean_toned"          -> add(PlayerClass.ENDURANCE_HUNTER, 2)
            "muscular_defined"    -> add(PlayerClass.MASS_BUILDER, 2)
            "balanced_functional" -> add(PlayerClass.BALANCE_WARRIOR, 2)
            "strong_powerful"     -> add(PlayerClass.STRENGTH_SEEKER, 2)
        }

        when (motive) {
            "data_driven"    -> add(PlayerClass.ATHLETE_ELITE, 2)
            "competitive"    -> add(PlayerClass.ATHLETE_ELITE, 3)
            "feeling_strong" -> add(PlayerClass.STRENGTH_SEEKER, 1)
            "visual_results" -> add(PlayerClass.MASS_BUILDER, 1)
        }

        if (equipment == "bodyweight") add(PlayerClass.ENDURANCE_HUNTER, 1)

        return scores.maxByOrNull { it.value }?.key ?: PlayerClass.BALANCE_WARRIOR
    }
}