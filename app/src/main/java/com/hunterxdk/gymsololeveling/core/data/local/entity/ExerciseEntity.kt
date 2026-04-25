package com.hunterxdk.gymsololeveling.core.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val mainEquipment: String,
    val otherEquipment: String,       // JSON array
    val primaryMuscles: String,       // JSON array
    val secondaryMuscles: String,     // JSON array
    val splitCategories: String,      // JSON array
    val exerciseCounterType: String,
    val exerciseMechanics: String,
    val difficulty: Int,
    val instructions: String,         // JSON array
    val tips: String,                 // JSON array
    val benefits: String,             // JSON array
    val breathingInstructions: String,
    val keywords: String,             // JSON array
    val metabolicEquivalent: Double,
    val repSupplement: String?,
    val isCustom: Boolean = false,
    val youtubeVideoId: String?,
)

@Fts4(contentEntity = ExerciseEntity::class)
@Entity(tableName = "exercises_fts")
data class ExerciseFtsEntity(
    val name: String,
    val keywords: String,
)
