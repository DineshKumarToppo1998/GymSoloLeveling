package com.hunterxdk.gymsololeveling.core.util

import android.content.Context
import com.hunterxdk.gymsololeveling.core.domain.model.Exercise
import java.util.Locale
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ExerciseJsonLoader {

    private val json = Json { ignoreUnknownKeys = true }

    private val AVAILABLE_LOCALES = setOf(
        "ar", "ar-EG", "ar-SA", "ca", "cs", "da", "de", "de-DE", "el",
        "en", "en-AU", "en-CA", "en-GB", "en-US", "es", "es-ES", "es-MX",
        "fi", "fr", "fr-CA", "fr-FR", "he", "hi", "hr", "hu", "id", "it",
        "ja", "ko", "ms", "nl", "nl-NL", "no", "pl", "pt", "pt-BR", "pt-PT",
        "ro", "ru", "sk", "sv", "th", "tr", "uk", "vi", "zh", "zh-Hans", "zh-Hant",
    )

    fun resolveLocaleTag(systemLocale: Locale): String {
        val full = "${systemLocale.language}-${systemLocale.country}"
        val lang = systemLocale.language
        return when {
            full in AVAILABLE_LOCALES -> full
            lang in AVAILABLE_LOCALES -> lang
            else -> "en"
        }
    }

    suspend fun load(context: Context, localeTag: String = "en"): List<Exercise> = withContext(Dispatchers.IO) {
        // AAPT2 decompresses .gz assets and strips the extension at build time,
        // so the file is stored as exercises_en.json in the APK.
        // Try plain JSON first, fall back to gzip for local dev/test scenarios.
        val jsonFile = "exercises/exercises_$localeTag.json"
        val gzFile   = "exercises/exercises_$localeTag.json.gz"
        try {
            val text = try {
                context.assets.open(jsonFile).bufferedReader().readText()
            } catch (e: java.io.FileNotFoundException) {
                GZIPInputStream(context.assets.open(gzFile)).bufferedReader().readText()
            }
            val raw = json.decodeFromString<Map<String, ExerciseRaw>>(text)
            raw.map { (id, e) -> e.toDomain(id) }
        } catch (e: java.io.FileNotFoundException) {
            if (localeTag != "en") load(context, "en") else emptyList()
        }
    }
}

@Serializable
private data class ExerciseRaw(
    val name: String = "",
    val mainEquipment: String = "bodyweight",
    val otherEquipment: List<String> = emptyList(),
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val splitCategories: List<String> = emptyList(),
    val exerciseCounterType: String = "reps_and_weight",
    val exerciseMechanics: String = "compound",
    val difficulty: Int = 1,
    @SerialName("instructionsArray") val instructions: List<String> = emptyList(),
    @SerialName("tipsArray") val tips: List<String> = emptyList(),
    @SerialName("benefitsArray") val benefits: List<String> = emptyList(),
    val breathingInstructions: String = "",
    val keywords: List<String> = emptyList(),
    val metabolicEquivalent: Double = 4.0,
    val repSupplement: String? = null,
    val youtubeVideoId: String? = null,
) {
    fun toDomain(id: String) = com.hunterxdk.gymsololeveling.core.domain.model.Exercise(
        id = id,
        name = name,
        mainEquipment = mainEquipment,
        otherEquipment = otherEquipment,
        primaryMuscles = primaryMuscles,
        secondaryMuscles = secondaryMuscles,
        splitCategories = splitCategories,
        exerciseCounterType = exerciseCounterType,
        exerciseMechanics = exerciseMechanics,
        difficulty = difficulty,
        instructions = instructions,
        tips = tips,
        benefits = benefits,
        breathingInstructions = breathingInstructions,
        keywords = keywords,
        metabolicEquivalent = metabolicEquivalent,
        repSupplement = repSupplement,
        isCustom = false,
        youtubeVideoId = youtubeVideoId,
    )
}
