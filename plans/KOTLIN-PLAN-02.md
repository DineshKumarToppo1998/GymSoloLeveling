# KOTLIN-PLAN-02.md — Asset Pipeline & Room Database

## Goal
Copy all APK assets into the Android project, set up Room database with all entities and DAOs, and implement ExerciseJsonLoader.

## Phase
Foundation — Phase 2 of 6. Depends on KOTLIN-PLAN-01.

---

## Step 1: Copy Assets (shell commands)

```bash
# Exercises JSON (gzipped)
mkdir -p <project>/app/src/main/assets/exercises/
cp /home/dinesh-linux/Downloads/gymlevels/assets/flutter_assets/assets/exercises/exercises_en.json.gz \
   <project>/app/src/main/assets/exercises/

# Rank badge images → res/drawable
cp /home/dinesh-linux/Downloads/gymlevels/assets/flutter_assets/assets/images/ranks/rank_*.webp \
   <project>/app/src/main/res/drawable/

# Achievement badge images
cp /home/dinesh-linux/Downloads/gymlevels/assets/flutter_assets/assets/images/achievements/badge_*.webp \
   <project>/app/src/main/res/drawable/

# Player class images
cp /home/dinesh-linux/Downloads/gymlevels/assets/flutter_assets/assets/images/classes/*.webp \
   <project>/app/src/main/res/drawable/

# Onboarding images (all subfolders)
cp -r /home/dinesh-linux/Downloads/gymlevels/assets/flutter_assets/assets/images/onboarding/ \
   <project>/app/src/main/assets/onboarding/

# Fonts (already handled in PLAN-01, verify they exist)
ls <project>/app/src/main/res/font/
```

---

## Step 2: Files to Create

### `core/util/ExerciseJsonLoader.kt`
```kotlin
package com.example.gymlevels.core.util

import android.content.Context
import com.example.gymlevels.core.domain.model.Exercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.zip.GZIPInputStream

object ExerciseJsonLoader {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(context: Context): List<Exercise> = withContext(Dispatchers.IO) {
        val gzip = GZIPInputStream(context.assets.open("exercises/exercises_en.json.gz"))
        val text = gzip.bufferedReader().readText()
        val raw = json.decodeFromString<Map<String, ExerciseRaw>>(text)
        raw.map { (id, e) -> e.toDomain(id) }
    }
}

@Serializable
data class ExerciseRaw(
    val name: String = "",
    @SerialName("main_equipment") val mainEquipment: String = "bodyweight",
    @SerialName("other_equipment") val otherEquipment: List<String> = emptyList(),
    @SerialName("primary_muscles") val primaryMuscles: List<String> = emptyList(),
    @SerialName("secondary_muscles") val secondaryMuscles: List<String> = emptyList(),
    @SerialName("split_categories") val splitCategories: List<String> = emptyList(),
    @SerialName("exercise_counter_type") val exerciseCounterType: String = "reps_and_weight",
    @SerialName("exercise_mechanics") val exerciseMechanics: String = "compound",
    val difficulty: Int = 1,
    val instructions: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val benefits: List<String> = emptyList(),
    @SerialName("breathing_instructions") val breathingInstructions: String = "",
    val keywords: List<String> = emptyList(),
    @SerialName("metabolic_equivalent") val metabolicEquivalent: Double = 4.0,
    @SerialName("rep_supplement") val repSupplement: String? = null,
    @SerialName("youtube_video_id") val youtubeVideoId: String? = null,
) {
    fun toDomain(id: String) = Exercise(
        id = id, name = name, mainEquipment = mainEquipment,
        otherEquipment = otherEquipment, primaryMuscles = primaryMuscles,
        secondaryMuscles = secondaryMuscles, splitCategories = splitCategories,
        exerciseCounterType = exerciseCounterType, exerciseMechanics = exerciseMechanics,
        difficulty = difficulty, instructions = instructions, tips = tips,
        benefits = benefits, breathingInstructions = breathingInstructions,
        keywords = keywords, metabolicEquivalent = metabolicEquivalent,
        repSupplement = repSupplement, isCustom = false, youtubeVideoId = youtubeVideoId
    )
}
```

### `core/data/local/GymLevelsDatabase.kt`
```kotlin
package com.example.gymlevels.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.gymlevels.core.data.local.dao.*
import com.example.gymlevels.core.data.local.entity.*

@Database(
    entities = [
        ExerciseEntity::class,
        ExerciseFtsEntity::class,
        ActiveWorkoutSessionEntity::class,
        ActiveWorkoutExerciseEntity::class,
        ActiveWorkoutSetEntity::class,
        MuscleRankEntity::class,
        WeightEntryEntity::class,
        WorkoutTemplateEntity::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class GymLevelsDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun activeWorkoutDao(): ActiveWorkoutDao
    abstract fun muscleRankDao(): MuscleRankDao
    abstract fun weightEntryDao(): WeightEntryDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
}
```

### `core/data/local/entity/ExerciseEntity.kt`
```kotlin
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val mainEquipment: String,
    val otherEquipment: String,       // JSON array text
    val primaryMuscles: String,       // JSON array text
    val secondaryMuscles: String,     // JSON array text
    val splitCategories: String,      // JSON array text
    val exerciseCounterType: String,
    val exerciseMechanics: String,
    val difficulty: Int,
    val instructions: String,         // JSON array text
    val tips: String,                 // JSON array text
    val benefits: String,             // JSON array text
    val breathingInstructions: String,
    val keywords: String,             // JSON array text
    val metabolicEquivalent: Double,
    val repSupplement: String?,
    val isCustom: Boolean = false,
    val youtubeVideoId: String?,
)
```

### `core/data/local/entity/ExerciseFtsEntity.kt`
```kotlin
@Fts4(contentEntity = ExerciseEntity::class)
@Entity(tableName = "exercises_fts")
data class ExerciseFtsEntity(
    val name: String,
    val keywords: String,
)
```

### `core/data/local/entity/ActiveWorkoutSessionEntity.kt`
```kotlin
@Entity(tableName = "active_workout_sessions")
data class ActiveWorkoutSessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val workoutType: String,
    val startedAt: Long,
    val notes: String = "",
    val templateId: String? = null,
)
```

### `core/data/local/entity/ActiveWorkoutExerciseEntity.kt`
```kotlin
@Entity(
    tableName = "active_workout_exercises",
    foreignKeys = [ForeignKey(
        entity = ActiveWorkoutSessionEntity::class,
        parentColumns = ["id"], childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class ActiveWorkoutExerciseEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val exerciseName: String,
    val orderIndex: Int,
    val notes: String = "",
)
```

### `core/data/local/entity/ActiveWorkoutSetEntity.kt`
```kotlin
@Entity(
    tableName = "active_workout_sets",
    foreignKeys = [ForeignKey(
        entity = ActiveWorkoutExerciseEntity::class,
        parentColumns = ["id"], childColumns = ["workoutExerciseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("workoutExerciseId")]
)
data class ActiveWorkoutSetEntity(
    @PrimaryKey val id: String,
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
    val completedAt: Long,
)
```

### `core/data/local/entity/MuscleRankEntity.kt`
```kotlin
@Entity(tableName = "muscle_ranks")
data class MuscleRankEntity(
    @PrimaryKey val muscleGroup: String,
    val totalXp: Int = 0,
    val currentRank: String = "UNTRAINED",
    val currentSubRank: String? = null,
    val xpToNextRank: Int = 50,
    val updatedAt: Long,
)
```

### `core/data/local/entity/WeightEntryEntity.kt`
```kotlin
@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val weightKg: Double,
    val recordedAt: Long,
    val notes: String = "",
    val isSynced: Boolean = false,
)
```

### `core/data/local/entity/WorkoutTemplateEntity.kt`
```kotlin
@Entity(tableName = "workout_templates")
data class WorkoutTemplateEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val workoutType: String,
    val exercisesJson: String,
    val estimatedDurationMinutes: Int = 45,
    val createdAt: Long,
    val lastUsedAt: Long? = null,
    val useCount: Int = 0,
)
```

### `core/data/local/dao/ExerciseDao.kt`
```kotlin
@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Query("SELECT exercises.* FROM exercises JOIN exercises_fts ON exercises.id = exercises_fts.rowid WHERE exercises_fts MATCH :query")
    fun searchExercises(query: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE primaryMuscles LIKE '%' || :muscle || '%'")
    fun getByMuscle(muscle: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE mainEquipment = :equipment OR otherEquipment LIKE '%' || :equipment || '%'")
    fun getByEquipment(equipment: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE splitCategories LIKE '%' || :category || '%'")
    fun getByCategory(category: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises")
    fun getAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE isCustom = 1")
    fun getCustom(): Flow<List<ExerciseEntity>>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Delete
    suspend fun delete(exercise: ExerciseEntity)
}
```

### `core/data/local/dao/ActiveWorkoutDao.kt`
```kotlin
@Dao
interface ActiveWorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ActiveWorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ActiveWorkoutExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: ActiveWorkoutSetEntity)

    @Update
    suspend fun updateSet(set: ActiveWorkoutSetEntity)

    @Query("DELETE FROM active_workout_sets WHERE id = :setId")
    suspend fun deleteSet(setId: String)

    @Transaction
    @Query("SELECT * FROM active_workout_sessions LIMIT 1")
    suspend fun getActiveSession(): ActiveWorkoutSessionWithExercises?

    @Query("DELETE FROM active_workout_sessions")
    suspend fun clearAllSessions()

    @Query("DELETE FROM active_workout_exercises WHERE sessionId = :sessionId")
    suspend fun clearExercisesForSession(sessionId: String)
}

data class ActiveWorkoutSessionWithExercises(
    @Embedded val session: ActiveWorkoutSessionEntity,
    @Relation(
        entity = ActiveWorkoutExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val exercises: List<ActiveWorkoutExerciseWithSets>
)

data class ActiveWorkoutExerciseWithSets(
    @Embedded val exercise: ActiveWorkoutExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutExerciseId")
    val sets: List<ActiveWorkoutSetEntity>
)
```

### `core/data/local/dao/MuscleRankDao.kt`
```kotlin
@Dao
interface MuscleRankDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rank: MuscleRankEntity)

    @Query("SELECT * FROM muscle_ranks")
    suspend fun getAll(): List<MuscleRankEntity>

    @Query("SELECT * FROM muscle_ranks WHERE muscleGroup = :muscle")
    suspend fun get(muscle: String): MuscleRankEntity?
}
```

### `core/data/local/dao/WeightEntryDao.kt`
```kotlin
@Dao
interface WeightEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WeightEntryEntity)

    @Query("SELECT * FROM weight_entries WHERE userId = :userId ORDER BY recordedAt DESC")
    fun getAll(userId: String): Flow<List<WeightEntryEntity>>

    @Delete
    suspend fun delete(entry: WeightEntryEntity)
}
```

### `core/data/local/dao/WorkoutTemplateDao.kt`
```kotlin
@Dao
interface WorkoutTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: WorkoutTemplateEntity)

    @Query("SELECT * FROM workout_templates WHERE userId = :userId ORDER BY lastUsedAt DESC")
    fun getAll(userId: String): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun getById(id: String): WorkoutTemplateEntity?

    @Delete
    suspend fun delete(template: WorkoutTemplateEntity)
}
```

### `core/di/DatabaseModule.kt`
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GymLevelsDatabase =
        Room.databaseBuilder(context, GymLevelsDatabase::class.java, "gymlevels.db").build()

    @Provides fun provideExerciseDao(db: GymLevelsDatabase) = db.exerciseDao()
    @Provides fun provideActiveWorkoutDao(db: GymLevelsDatabase) = db.activeWorkoutDao()
    @Provides fun provideMuscleRankDao(db: GymLevelsDatabase) = db.muscleRankDao()
    @Provides fun provideWeightEntryDao(db: GymLevelsDatabase) = db.weightEntryDao()
    @Provides fun provideWorkoutTemplateDao(db: GymLevelsDatabase) = db.workoutTemplateDao()
}
```

---

## Verification
1. `./gradlew assembleDebug` — no Room compilation errors
2. Launch app → check Logcat for no DB migration errors
3. Android Studio DB Inspector: `exercises` table exists (may be empty until PLAN-03 loads data)
4. After adding ExerciseRepository in PLAN-03, call `ensureLoaded()` once and verify 265 rows in `exercises` table
