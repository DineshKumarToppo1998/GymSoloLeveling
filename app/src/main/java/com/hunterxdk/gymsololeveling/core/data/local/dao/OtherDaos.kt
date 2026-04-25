package com.hunterxdk.gymsololeveling.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hunterxdk.gymsololeveling.core.data.local.entity.MuscleRankEntity
import com.hunterxdk.gymsololeveling.core.data.local.entity.WeightEntryEntity
import com.hunterxdk.gymsololeveling.core.data.local.entity.WorkoutTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MuscleRankDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rank: MuscleRankEntity)

    @Query("SELECT * FROM muscle_ranks")
    suspend fun getAll(): List<MuscleRankEntity>

    @Query("SELECT * FROM muscle_ranks")
    fun observeAll(): Flow<List<MuscleRankEntity>>

    @Query("SELECT * FROM muscle_ranks WHERE muscleGroup = :muscle")
    suspend fun get(muscle: String): MuscleRankEntity?
}

@Dao
interface WeightEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WeightEntryEntity)

    @Query("SELECT * FROM weight_entries WHERE uid = :userId ORDER BY loggedAt DESC")
    fun getAll(userId: String): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries WHERE uid = :userId ORDER BY loggedAt ASC LIMIT 90")
    suspend fun getLast90(userId: String): List<WeightEntryEntity>

    @Delete
    suspend fun delete(entry: WeightEntryEntity)
}

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
