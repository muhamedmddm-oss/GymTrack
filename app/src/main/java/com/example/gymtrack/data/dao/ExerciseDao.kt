package com.example.gymtrack.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.gymtrack.data.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

/** Result row of [ExerciseDao.observeCountsPerDay]. */
data class DayExerciseCount(val workoutDayId: Long, val count: Int)

@Dao
interface ExerciseDao {
    @Insert
    suspend fun insert(exercise: ExerciseEntity): Long

    @Insert
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Delete
    suspend fun delete(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :exerciseId")
    suspend fun deleteById(exerciseId: Long)

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun getById(exerciseId: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises ORDER BY workoutDayId, orderIndex, id")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE workoutDayId = :dayId ORDER BY orderIndex, id")
    suspend fun getForDay(dayId: Long): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE workoutDayId = :dayId ORDER BY orderIndex, id")
    fun observeForDay(dayId: Long): Flow<List<ExerciseEntity>>

    @Query("SELECT COUNT(*) FROM exercises WHERE workoutDayId = :dayId")
    suspend fun countForDay(dayId: Long): Int

    @Query("SELECT workoutDayId, COUNT(*) AS count FROM exercises GROUP BY workoutDayId")
    fun observeCountsPerDay(): Flow<List<DayExerciseCount>>

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM exercises WHERE workoutDayId = :dayId")
    suspend fun maxOrderIndex(dayId: Long): Int
}
