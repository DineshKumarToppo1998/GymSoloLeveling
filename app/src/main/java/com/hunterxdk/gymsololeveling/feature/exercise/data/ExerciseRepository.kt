package com.hunterxdk.gymsololeveling.feature.exercise.data

import android.content.Context
import com.hunterxdk.gymsololeveling.core.data.local.dao.ExerciseDao
import com.hunterxdk.gymsololeveling.core.data.local.entity.ExerciseEntity
import com.hunterxdk.gymsololeveling.core.domain.model.Exercise
import com.hunterxdk.gymsololeveling.core.util.ExerciseJsonLoader
import com.hunterxdk.gymsololeveling.core.data.preferences.UserPreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    @param:ApplicationContext private val context: Context,
    private val json: Json,
    private val prefs: UserPreferencesDataStore,
) {
    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        val prefLocale = prefs.exerciseLocale.firstOrNull()
        val lastSeedLocale = prefs.lastSeedLocale.firstOrNull()
        val systemLocale = Locale.getDefault()
        val resolvedLocale = if (prefLocale?.isNotBlank() == true) prefLocale
                              else ExerciseJsonLoader.resolveLocaleTag(systemLocale)
        android.util.Log.d("ExerciseSeed", "resolvedLocale=$resolvedLocale lastSeedLocale=$lastSeedLocale count=${exerciseDao.count()}")

        if (lastSeedLocale == resolvedLocale && exerciseDao.count() > 0) {
            android.util.Log.d("ExerciseSeed", "skipping — already seeded")
            return@withContext
        }

        val exercises = ExerciseJsonLoader.load(context, resolvedLocale)
        android.util.Log.d("ExerciseSeed", "loaded ${exercises.size} exercises from asset")
        exerciseDao.clearNonCustom()
        exerciseDao.insertAll(exercises.map { it.toEntity(json) })
        android.util.Log.d("ExerciseSeed", "inserted — count now ${exerciseDao.count()}")
        prefs.setLastSeedLocale(resolvedLocale)
    }

    suspend fun ensureLoaded() = withContext(Dispatchers.IO) {
        if (exerciseDao.count() == 0) {
            val exercises = ExerciseJsonLoader.load(context)
            exerciseDao.insertAll(exercises.map { it.toEntity(json) })
        } else {
            // Verify we actually have exercises (not just empty DB)
            val customOnly = exerciseDao.getCustom().firstOrNull()?.isEmpty() ?: true
            if (customOnly && exerciseDao.count() <= 1) {
                val exercises = ExerciseJsonLoader.load(context)
                exerciseDao.clearNonCustom()
                exerciseDao.insertAll(exercises.map { it.toEntity(json) })
            }
        }
    }

    fun searchExercises(query: String): Flow<List<Exercise>> {
        val ftsQuery = if (query.isBlank()) "*" else "$query*"
        return exerciseDao.searchExercises(ftsQuery).map { list -> list.map { it.toDomain(json) } }
    }

    fun getAllExercises(): Flow<List<Exercise>> =
        exerciseDao.getAll().map { list -> list.map { it.toDomain(json) } }

    fun getCustomExercises(): Flow<List<Exercise>> =
        exerciseDao.getCustom().map { list -> list.map { it.toDomain(json) } }

    fun getByMuscle(muscle: String): Flow<List<Exercise>> =
        exerciseDao.getByMuscle(muscle).map { list -> list.map { it.toDomain(json) } }

    fun getByEquipment(equipment: String): Flow<List<Exercise>> =
        exerciseDao.getByEquipment(equipment).map { list -> list.map { it.toDomain(json) } }

    suspend fun getById(id: String): Exercise? =
        exerciseDao.getById(id)?.toDomain(json)

    suspend fun saveCustomExercise(exercise: Exercise) =
        exerciseDao.insertAll(listOf(exercise.toEntity(json)))

    suspend fun deleteCustomExercise(exercise: Exercise) =
        exerciseDao.delete(exercise.toEntity(json))
}

private fun Exercise.toEntity(json: Json) = ExerciseEntity(
    id = id, name = name, mainEquipment = mainEquipment,
    otherEquipment = json.encodeToString(otherEquipment),
    primaryMuscles = json.encodeToString(primaryMuscles),
    secondaryMuscles = json.encodeToString(secondaryMuscles),
    splitCategories = json.encodeToString(splitCategories),
    exerciseCounterType = exerciseCounterType,
    exerciseMechanics = exerciseMechanics, difficulty = difficulty,
    instructions = json.encodeToString(instructions),
    tips = json.encodeToString(tips), benefits = json.encodeToString(benefits),
    breathingInstructions = breathingInstructions,
    keywords = json.encodeToString(keywords),
    metabolicEquivalent = metabolicEquivalent, repSupplement = repSupplement,
    isCustom = isCustom, youtubeVideoId = youtubeVideoId,
)

private fun ExerciseEntity.toDomain(json: Json) = Exercise(
    id = id, name = name, mainEquipment = mainEquipment,
    otherEquipment = json.decodeFromString(otherEquipment),
    primaryMuscles = json.decodeFromString(primaryMuscles),
    secondaryMuscles = json.decodeFromString(secondaryMuscles),
    splitCategories = json.decodeFromString(splitCategories),
    exerciseCounterType = exerciseCounterType,
    exerciseMechanics = exerciseMechanics, difficulty = difficulty,
    instructions = json.decodeFromString(instructions),
    tips = json.decodeFromString(tips), benefits = json.decodeFromString(benefits),
    breathingInstructions = breathingInstructions,
    keywords = json.decodeFromString(keywords),
    metabolicEquivalent = metabolicEquivalent, repSupplement = repSupplement,
    isCustom = isCustom, youtubeVideoId = youtubeVideoId,
)
