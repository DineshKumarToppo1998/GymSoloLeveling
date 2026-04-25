package com.hunterxdk.gymsololeveling.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hunterxdk.gymsololeveling.core.data.local.dao.ActiveWorkoutDao
import com.hunterxdk.gymsololeveling.core.data.local.dao.ExerciseDao
import com.hunterxdk.gymsololeveling.core.data.local.dao.MuscleRankDao
import com.hunterxdk.gymsololeveling.core.data.local.dao.WeightEntryDao
import com.hunterxdk.gymsololeveling.core.data.local.dao.WorkoutTemplateDao
import com.hunterxdk.gymsololeveling.core.data.local.entity.ActiveWorkoutExerciseEntity
import com.hunterxdk.gymsololeveling.core.data.local.entity.ActiveWorkoutSessionEntity
import com.hunterxdk.gymsololeveling.core.data.local.entity.ActiveWorkoutSetEntity
import com.hunterxdk.gymsololeveling.core.data.local.entity.ExerciseEntity
import com.hunterxdk.gymsololeveling.core.data.local.entity.ExerciseFtsEntity
import com.hunterxdk.gymsololeveling.core.data.local.entity.MuscleRankEntity
import com.hunterxdk.gymsololeveling.core.data.local.entity.WeightEntryEntity
import com.hunterxdk.gymsololeveling.core.data.local.entity.WorkoutTemplateEntity

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
    exportSchema = false,
)
abstract class GymLevelsDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun activeWorkoutDao(): ActiveWorkoutDao
    abstract fun muscleRankDao(): MuscleRankDao
    abstract fun weightEntryDao(): WeightEntryDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
}
