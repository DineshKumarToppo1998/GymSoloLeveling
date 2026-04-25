package com.hunterxdk.gymsololeveling.core.domain.model

import com.hunterxdk.gymsololeveling.core.domain.model.enums.ExerciseCounterType
import com.hunterxdk.gymsololeveling.core.domain.model.enums.toExerciseCounterType

data class Exercise(
    val id: String,
    val name: String,
    val mainEquipment: String,
    val otherEquipment: List<String>,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val splitCategories: List<String>,
    val exerciseCounterType: String,
    val exerciseMechanics: String,
    val difficulty: Int,
    val instructions: List<String>,
    val tips: List<String>,
    val benefits: List<String>,
    val breathingInstructions: String,
    val keywords: List<String>,
    val metabolicEquivalent: Double,
    val repSupplement: String?,
    val isCustom: Boolean = false,
    val youtubeVideoId: String?,
)

fun Exercise.counterType(): ExerciseCounterType = exerciseCounterType.toExerciseCounterType()
