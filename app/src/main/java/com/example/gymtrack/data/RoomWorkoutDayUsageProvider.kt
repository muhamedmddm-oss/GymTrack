package com.example.gymtrack.data

import com.example.gymtrack.data.dao.ExerciseDao
import com.example.gymtrack.data.dao.WorkoutSessionDao
import com.example.gymtrack.workout.WorkoutDayUsageProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The real implementation of Person 2's [WorkoutDayUsageProvider], backed by Room.
 * A day is "in use" when it has planned exercises OR recorded workout sessions, so deleting such
 * a day (or its plan) archives it instead of destroying the history.
 */
class RoomWorkoutDayUsageProvider(
    private val exerciseDao: ExerciseDao,
    private val sessionDao: WorkoutSessionDao
) : WorkoutDayUsageProvider {

    override fun observeExerciseCounts(): Flow<Map<Long, Int>> =
        exerciseDao.observeCountsPerDay().map { rows -> rows.associate { it.workoutDayId to it.count } }

    override suspend fun isDayInUse(dayId: Long): Boolean =
        exerciseDao.countForDay(dayId) > 0 || sessionDao.countForDay(dayId) > 0
}
