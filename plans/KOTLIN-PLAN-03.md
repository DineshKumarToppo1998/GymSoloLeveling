# KOTLIN-PLAN-03.md — Domain Models, DI Modules & Navigation Scaffold

## Goal
Create all domain model data classes, enums, Hilt DI modules, DataStore preferences wrapper, and the navigation scaffold with all screen routes.

## Phase
Foundation — Phase 3 of 6. Depends on KOTLIN-PLAN-01, KOTLIN-PLAN-02.

---

## Files to Create

### `core/domain/model/enums/`

#### `RankTier.kt`
```kotlin
enum class RankTier { UNTRAINED, BRONZE, SILVER, GOLD, PLATINUM, DIAMOND, MASTER, LEGEND }
```

#### `RankSubTier.kt`
```kotlin
enum class RankSubTier { I, II, III }
```

#### `MuscleGroup.kt`
```kotlin
enum class MuscleGroup(val displayName: String) {
    CHEST("Chest"),
    FRONT_SHOULDERS("Front Shoulders"),
    BACK_SHOULDERS("Back Shoulders"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    FOREARMS("Forearms"),
    UPPER_BACK("Upper Back"),
    MIDDLE_BACK("Middle Back"),
    LOWER_BACK("Lower Back"),
    LATS("Lats"),
    TRAPS("Traps"),
    ABS("Abs"),
    OBLIQUES("Obliques"),
    QUADRICEPS("Quadriceps"),
    HAMSTRINGS("Hamstrings"),
    GLUTES("Glutes"),
    CALVES("Calves"),
}

fun String.toMuscleGroup(): MuscleGroup? = MuscleGroup.entries.firstOrNull {
    it.name.lowercase() == this.lowercase().replace(" ", "_") ||
    it.displayName.lowercase() == this.lowercase()
}
```

#### `ExerciseCounterType.kt`
```kotlin
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
    ExerciseCounterType.entries.firstOrNull { it.name.lowercase() == this.lowercase() }
        ?: ExerciseCounterType.REPS_AND_WEIGHT
```

#### `WorkoutType.kt`
```kotlin
enum class WorkoutType(val displayName: String) {
    PUSH("Push"),
    PULL("Pull"),
    LOWER_BODY("Lower Body"),
    UPPER_BODY("Upper Body"),
    FULL_BODY("Full Body"),
    CARDIO("Cardio"),
    CUSTOM("Custom"),
}
```

#### `PlayerClass.kt`
```kotlin
enum class PlayerClass(val displayName: String, val description: String) {
    STRENGTH_SEEKER("Strength Seeker", "You chase heavy lifts and progressive overload"),
    MASS_BUILDER("Mass Builder", "Hypertrophy is your primary objective"),
    ENDURANCE_HUNTER("Endurance Hunter", "Your stamina knows no limits"),
    RECOVERY_SPECIALIST("Recovery Specialist", "Smart training, maximum adaptation"),
    BALANCE_WARRIOR("Balance Warrior", "You pursue well-rounded fitness"),
    ATHLETE_ELITE("Athlete Elite", "Peak performance across all domains"),
}
```

#### `FitnessGoal.kt`
```kotlin
enum class FitnessGoal { BUILD_MUSCLE, GET_STRONGER, LOSE_FAT, IMPROVE_HEALTH, IMPROVE_ENDURANCE, INCREASE_FLEXIBILITY }
```

#### `ActivityLevel.kt`
```kotlin
enum class ActivityLevel { SEDENTARY, LIGHTLY_ACTIVE, MODERATELY_ACTIVE, VERY_ACTIVE, EXTREMELY_ACTIVE }
```

#### `TrainingExperience.kt`
```kotlin
enum class TrainingExperience { BEGINNER, INTERMEDIATE, ADVANCED, VETERAN }
```

#### `BadgeType.kt`
```kotlin
enum class BadgeType { MILESTONE, CONSISTENCY, PR_HUNTER, VOLUME, EXPLORER, STRENGTH, MUSCLE_MASTER, SPECIAL }
```

#### `ChallengeDifficulty.kt`
```kotlin
enum class ChallengeDifficulty { EASY, MEDIUM, HARD }
```

---

### `core/domain/model/`

#### `Exercise.kt`
```kotlin
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
```

#### `WorkoutSet.kt`
```kotlin
data class WorkoutSet(
    val id: String,
    val workoutExerciseId: String,
    val setNumber: Int,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val distanceKm: Double? = null,
    val inclinePercent: Double? = null,
    val resistanceLevel: Int? = null,
    val floors: Int? = null,
    val steps: Int? = null,
    val bandResistance: String? = null,
    val isPersonalRecord: Boolean = false,
    val completedAt: Long = System.currentTimeMillis(),
)
```

#### `WorkoutExercise.kt`
```kotlin
data class WorkoutExercise(
    val id: String,
    val sessionId: String,
    val exercise: Exercise,
    val orderIndex: Int,
    val sets: List<WorkoutSet> = emptyList(),
    val notes: String = "",
)
```

#### `WorkoutSession.kt`
```kotlin
data class WorkoutSession(
    val id: String,
    val userId: String,
    val title: String,
    val workoutType: WorkoutType,
    val startedAt: Long,
    val endedAt: Long? = null,
    val exercises: List<WorkoutExercise> = emptyList(),
    val notes: String = "",
    val templateId: String? = null,
) {
    val durationSeconds: Long get() = ((endedAt ?: System.currentTimeMillis()) - startedAt) / 1000
    val totalVolumeKg: Double get() = exercises.flatMap { it.sets }.sumOf {
        ((it.reps ?: 0) * (it.weightKg ?: 0.0))
    }
    val totalXpEarned: Int get() = 0 // populated by XPService
}
```

#### `MuscleRank.kt`
```kotlin
data class MuscleRank(
    val muscleGroup: MuscleGroup,
    val totalXp: Int = 0,
    val currentRank: RankTier = RankTier.UNTRAINED,
    val currentSubRank: RankSubTier? = null,
    val xpToNextRank: Int = 50,
)

object XPThresholds {
    val SUB_RANK_THRESHOLDS: List<Triple<RankTier, RankSubTier?, Int>> = listOf(
        Triple(RankTier.BRONZE, RankSubTier.I, 50),
        Triple(RankTier.BRONZE, RankSubTier.II, 100),
        Triple(RankTier.BRONZE, RankSubTier.III, 150),
        Triple(RankTier.SILVER, RankSubTier.I, 200),
        Triple(RankTier.SILVER, RankSubTier.II, 500),
        Triple(RankTier.SILVER, RankSubTier.III, 750),
        Triple(RankTier.GOLD, RankSubTier.I, 1000),
        Triple(RankTier.GOLD, RankSubTier.II, 2000),
        Triple(RankTier.GOLD, RankSubTier.III, 3000),
        Triple(RankTier.PLATINUM, RankSubTier.I, 7500),
        Triple(RankTier.PLATINUM, RankSubTier.II, 17000),
        Triple(RankTier.PLATINUM, RankSubTier.III, 22000),
        Triple(RankTier.DIAMOND, null, 30000),
        Triple(RankTier.MASTER, null, 75000),
        Triple(RankTier.LEGEND, null, 200000),
    )

    val PLAYER_LEVEL_THRESHOLDS = listOf(
        0, 100, 300, 600, 1000, 1500, 2200, 3000, 4000, 5200, 6600, 8200, 10000,
        12000, 14500, 17000, 20000, 23500, 27500, 32000, 37000, 42500, 48500,
        55000, 62000, 69500, 77500, 86000, 95000, 105000
    )
}
```

#### `PlayerStats.kt`
```kotlin
data class PlayerStats(
    val userId: String,
    val totalWorkouts: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val totalXp: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val playerLevel: Int = 1,
    val playerClass: PlayerClass = PlayerClass.BALANCE_WARRIOR,
    val overallRank: RankTier = RankTier.UNTRAINED,
    val lastWorkoutAt: Long? = null,
    val totalPRs: Int = 0,
    val uniqueExercisesCount: Int = 0,
)
```

#### `Achievement.kt`
```kotlin
data class Achievement(
    val id: String,
    val type: BadgeType,
    val tier: Int,
    val earnedAt: Long?,
    val isClaimed: Boolean = false,
)
```

#### `Challenge.kt`
```kotlin
data class Challenge(
    val id: String,
    val name: String,
    val description: String,
    val targetValue: Double,
    val currentValue: Double = 0.0,
    val xpReward: Int,
    val expiresAt: Long,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val difficulty: ChallengeDifficulty,
)
```

#### `User.kt`
```kotlin
data class User(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val playerClass: PlayerClass = PlayerClass.BALANCE_WARRIOR,
    val hasCompletedOnboarding: Boolean = false,
    val availableEquipment: List<String> = emptyList(),
    val injuries: List<String> = emptyList(),
    val priorityMuscles: List<String> = emptyList(),
    val preferredWorkoutDays: List<Int> = emptyList(),
)
```

#### `WorkoutTemplate.kt`
```kotlin
data class WorkoutTemplate(
    val id: String,
    val userId: String,
    val name: String,
    val workoutType: WorkoutType,
    val exercises: List<WorkoutExercise>,
    val estimatedDurationMinutes: Int = 45,
    val createdAt: Long,
    val lastUsedAt: Long? = null,
    val useCount: Int = 0,
)
```

#### `WeightEntry.kt`
```kotlin
data class WeightEntry(
    val id: String,
    val userId: String,
    val weightKg: Double,
    val recordedAt: Long,
    val notes: String = "",
)
```

---

### `core/data/preferences/UserPreferencesDataStore.kt`
```kotlin
package com.example.gymlevels.core.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store = context.dataStore

    val hasCompletedOnboarding: Flow<Boolean> = store.data.map { it[Keys.ONBOARDING_DONE] ?: false }
    val isDarkTheme: Flow<Boolean> = store.data.map { it[Keys.DARK_THEME] ?: true }
    val preferredUnit: Flow<String> = store.data.map { it[Keys.UNIT] ?: "kg" }
    val workoutReminderEnabled: Flow<Boolean> = store.data.map { it[Keys.REMINDER_ENABLED] ?: false }
    val workoutReminderHour: Flow<Int> = store.data.map { it[Keys.REMINDER_HOUR] ?: 7 }
    val workoutReminderMinute: Flow<Int> = store.data.map { it[Keys.REMINDER_MINUTE] ?: 0 }
    val challengeDifficulty: Flow<String> = store.data.map { it[Keys.CHALLENGE_DIFFICULTY] ?: "MEDIUM" }
    val availableEquipment: Flow<String> = store.data.map { it[Keys.EQUIPMENT] ?: "" }
    val preferredWorkoutDays: Flow<String> = store.data.map { it[Keys.WORKOUT_DAYS] ?: "" }
    val priorityMuscles: Flow<String> = store.data.map { it[Keys.PRIORITY_MUSCLES] ?: "" }

    suspend fun setOnboardingDone() = store.edit { it[Keys.ONBOARDING_DONE] = true }
    suspend fun setDarkTheme(dark: Boolean) = store.edit { it[Keys.DARK_THEME] = dark }
    suspend fun setUnit(unit: String) = store.edit { it[Keys.UNIT] = unit }
    suspend fun setReminderEnabled(enabled: Boolean) = store.edit { it[Keys.REMINDER_ENABLED] = enabled }
    suspend fun setReminderTime(hour: Int, minute: Int) = store.edit {
        it[Keys.REMINDER_HOUR] = hour; it[Keys.REMINDER_MINUTE] = minute
    }
    suspend fun setChallengeDifficulty(d: String) = store.edit { it[Keys.CHALLENGE_DIFFICULTY] = d }
    suspend fun setAvailableEquipment(json: String) = store.edit { it[Keys.EQUIPMENT] = json }
    suspend fun setPreferredWorkoutDays(json: String) = store.edit { it[Keys.WORKOUT_DAYS] = json }
    suspend fun setPriorityMuscles(json: String) = store.edit { it[Keys.PRIORITY_MUSCLES] = json }

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val UNIT = stringPreferencesKey("preferred_unit")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val CHALLENGE_DIFFICULTY = stringPreferencesKey("challenge_difficulty")
        val EQUIPMENT = stringPreferencesKey("available_equipment")
        val WORKOUT_DAYS = stringPreferencesKey("workout_days")
        val PRIORITY_MUSCLES = stringPreferencesKey("priority_muscles")
    }
}
```

---

### `core/di/FirebaseModule.kt`
```kotlin
@Module @InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides @Singleton fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth
    @Provides @Singleton fun provideFirestore(): FirebaseFirestore = Firebase.firestore
    @Provides @Singleton fun provideStorage(): FirebaseStorage = Firebase.storage
}
```

### `core/di/AppModule.kt`
```kotlin
@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true; isLenient = true }
}
```

---

### `core/navigation/Screen.kt`
```kotlin
package com.example.gymlevels.core.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable object Splash : Screen
    @Serializable object Onboarding : Screen
    @Serializable object OnboardingQuiz : Screen
    @Serializable object PlayerClassReveal : Screen
    @Serializable object SignUp : Screen
    @Serializable object Home : Screen
    @Serializable object ActiveWorkout : Screen
    @Serializable data class WorkoutDetail(val workoutId: String) : Screen
    @Serializable object WorkoutHistory : Screen
    @Serializable object WorkoutComplete : Screen
    @Serializable object TodaysWorkoutPreview : Screen
    @Serializable object SavedWorkouts : Screen
    @Serializable object TemplateBuilder : Screen
    @Serializable data class ExercisePicker(val sessionId: String) : Screen
    @Serializable object MuscleRankings : Screen
    @Serializable object Profile : Screen
    @Serializable object EditProfile : Screen
    @Serializable object Achievements : Screen
    @Serializable object Challenges : Screen
    @Serializable object StreakDetail : Screen
    @Serializable object WeightTracker : Screen
    @Serializable object WeeklyReport : Screen
    @Serializable object Settings : Screen
    @Serializable object CustomExercises : Screen
    @Serializable object CustomExerciseForm : Screen
    @Serializable object YoutubePicker : Screen
    @Serializable object TrainingSchedule : Screen
    @Serializable object EquipmentSettings : Screen
    @Serializable object InjurySettings : Screen
    @Serializable object PriorityMuscles : Screen
}
```

### `core/navigation/NavGraph.kt`
```kotlin
package com.example.gymlevels.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun GymLevelsNavGraph() {
    val navController = rememberNavController()
    val startDestination: Screen = if (FirebaseAuth.getInstance().currentUser == null) {
        Screen.SignUp
    } else {
        Screen.Home
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable<Screen.Splash> { StubScreen("Splash") }
        composable<Screen.Onboarding> { StubScreen("Onboarding") }
        composable<Screen.OnboardingQuiz> { StubScreen("OnboardingQuiz") }
        composable<Screen.PlayerClassReveal> { StubScreen("PlayerClassReveal") }
        composable<Screen.SignUp> { StubScreen("SignUp") }
        composable<Screen.Home> { StubScreen("Home") }
        composable<Screen.ActiveWorkout> { StubScreen("ActiveWorkout") }
        composable<Screen.WorkoutDetail> { StubScreen("WorkoutDetail") }
        composable<Screen.WorkoutHistory> { StubScreen("WorkoutHistory") }
        composable<Screen.WorkoutComplete> { StubScreen("WorkoutComplete") }
        composable<Screen.TodaysWorkoutPreview> { StubScreen("TodaysWorkoutPreview") }
        composable<Screen.SavedWorkouts> { StubScreen("SavedWorkouts") }
        composable<Screen.TemplateBuilder> { StubScreen("TemplateBuilder") }
        composable<Screen.ExercisePicker> { StubScreen("ExercisePicker") }
        composable<Screen.MuscleRankings> { StubScreen("MuscleRankings") }
        composable<Screen.Profile> { StubScreen("Profile") }
        composable<Screen.EditProfile> { StubScreen("EditProfile") }
        composable<Screen.Achievements> { StubScreen("Achievements") }
        composable<Screen.Challenges> { StubScreen("Challenges") }
        composable<Screen.StreakDetail> { StubScreen("StreakDetail") }
        composable<Screen.WeightTracker> { StubScreen("WeightTracker") }
        composable<Screen.WeeklyReport> { StubScreen("WeeklyReport") }
        composable<Screen.Settings> { StubScreen("Settings") }
        composable<Screen.CustomExercises> { StubScreen("CustomExercises") }
        composable<Screen.CustomExerciseForm> { StubScreen("CustomExerciseForm") }
        composable<Screen.YoutubePicker> { StubScreen("YoutubePicker") }
        composable<Screen.TrainingSchedule> { StubScreen("TrainingSchedule") }
        composable<Screen.EquipmentSettings> { StubScreen("EquipmentSettings") }
        composable<Screen.InjurySettings> { StubScreen("InjurySettings") }
        composable<Screen.PriorityMuscles> { StubScreen("PriorityMuscles") }
    }
}

@Composable
private fun StubScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(name, color = Color.White)
    }
}
```

---

## Verification
1. `./gradlew assembleDebug` — all Screen @Serializable objects compile
2. App launches → navigates to SignUp screen (no logged-in user)
3. No Hilt compilation errors for DatabaseModule, FirebaseModule, AppModule
