package com.example.gymtrack.workout.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Integration point for Person 3 (exercises) and Person 4 (sessions).
 *
 * The schedule module does not know about exercises or sessions, so it asks this provider:
 *  - how many exercises each day has (shown on the cards), and
 *  - whether a day is "in use" (has exercises or recorded sessions). In-use days are archived
 *    instead of deleted so historical data is never lost.
 *
 * Until their modules exist, [NoWorkoutDayUsage] is used (0 exercises, nothing in use).
 */
interface WorkoutDayUsageProvider {
    /** Map of workoutDayId -> number of exercises. Days without exercises may be omitted. */
    fun observeExerciseCounts(): Flow<Map<Long, Int>>

    /** True if the day has exercises or workout sessions that must be preserved. */
    suspend fun isDayInUse(dayId: Long): Boolean
}

object NoWorkoutDayUsage : WorkoutDayUsageProvider {
    override fun observeExerciseCounts(): Flow<Map<Long, Int>> = flowOf(emptyMap())
    override suspend fun isDayInUse(dayId: Long): Boolean = false
}
