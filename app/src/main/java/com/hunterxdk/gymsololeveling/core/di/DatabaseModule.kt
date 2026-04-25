package com.hunterxdk.gymsololeveling.core.di

import android.content.Context
import androidx.room.Room
import com.hunterxdk.gymsololeveling.core.data.local.GymLevelsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GymLevelsDatabase =
        Room.databaseBuilder(context, GymLevelsDatabase::class.java, "gymlevels.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideExerciseDao(db: GymLevelsDatabase) = db.exerciseDao()
    @Provides fun provideActiveWorkoutDao(db: GymLevelsDatabase) = db.activeWorkoutDao()
    @Provides fun provideMuscleRankDao(db: GymLevelsDatabase) = db.muscleRankDao()
    @Provides fun provideWeightEntryDao(db: GymLevelsDatabase) = db.weightEntryDao()
    @Provides fun provideWorkoutTemplateDao(db: GymLevelsDatabase) = db.workoutTemplateDao()
}
