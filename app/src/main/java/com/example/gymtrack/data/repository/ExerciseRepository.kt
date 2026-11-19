package com.example.gymtrack.data.repository

import com.example.gymtrack.data.dao.ExerciseDao
import com.example.gymtrack.data.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val dao: ExerciseDao) {

    fun observeExercises(dayId: Long): Flow<List<ExerciseEntity>> = dao.observeForDay(dayId)

    suspend fun getExercises(dayId: Long): List<ExerciseEntity> = dao.getForDay(dayId)

    suspend fun getExercise(exerciseId: Long): ExerciseEntity? = dao.getById(exerciseId)

    suspend fun addExercise(
        dayId: Long,
        name: String,
        sets: Int,
        reps: Int,
        weightKg: Double? = null,
        notes: String? = null
    ): Long = dao.insert(
        ExerciseEntity(
            workoutDayId = dayId,
            name = name.trim(),
            sets = sets,
            reps = reps,
            weightKg = weightKg,
            orderIndex = dao.maxOrderIndex(dayId) + 1,
            notes = notes
        )
    )

    suspend fun updateExercise(exercise: ExerciseEntity) = dao.update(exercise)

    /** Past sessions keep their own copy of the name, so deleting an exercise never alters history. */
    suspend fun deleteExercise(exerciseId: Long) = dao.deleteById(exerciseId)
}
