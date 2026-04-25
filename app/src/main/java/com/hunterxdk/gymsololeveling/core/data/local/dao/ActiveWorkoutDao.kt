package com.hunterxdk.gymsololeveling.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hunterxdk.gymsololeveling.core.data.local.entity.ActiveWorkoutExerciseEntity
import com.hunterxdk.gymsololeveling.core.data.local.entity.ActiveWorkoutSessionEntity
import com.hunterxdk.gymsololeveling.core.data.local.entity.ActiveWorkoutSessionWithExercises
import com.hunterxdk.gymsololeveling.core.data.local.entity.ActiveWorkoutSetEntity

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
}
