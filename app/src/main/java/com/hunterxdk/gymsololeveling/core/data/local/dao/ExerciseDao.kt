package com.hunterxdk.gymsololeveling.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hunterxdk.gymsololeveling.core.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Query("""
        SELECT exercises.* FROM exercises
        JOIN exercises_fts ON exercises.rowid = exercises_fts.rowid
        WHERE exercises_fts MATCH :query
    """)
    fun searchExercises(query: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE primaryMuscles LIKE '%' || :muscle || '%'")
    fun getByMuscle(muscle: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE mainEquipment = :equipment OR otherEquipment LIKE '%' || :equipment || '%'")
    fun getByEquipment(equipment: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE splitCategories LIKE '%' || :category || '%'")
    fun getByCategory(category: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE isCustom = 1 ORDER BY name ASC")
    fun getCustom(): Flow<List<ExerciseEntity>>

    @Query("DELETE FROM exercises WHERE isCustom = 0")
    suspend fun clearNonCustom()

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Delete
    suspend fun delete(exercise: ExerciseEntity)
}
