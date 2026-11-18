package com.example.gymtrack.workout.domain.repository

import com.example.gymtrack.workout.domain.model.Weekday
import com.example.gymtrack.workout.domain.model.WorkoutDayDetails
import com.example.gymtrack.workout.domain.model.WorkoutDayInput
import com.example.gymtrack.workout.domain.model.WorkoutPlan
import kotlinx.coroutines.flow.Flow

/**
 * Public contract of the Workout Schedule module (Person 2).
 * Other modules (Home, Active Workout, Statistics) should depend on this interface only.
 *
 * Deletion rule: a plan/day that is already referenced by exercises or workout sessions is
 * archived (hidden from every list here) instead of being physically deleted, so history survives.
 */
interface WorkoutRepository {
    /** All active plans, newest first, each with its active days. Emits again on every change. */
    fun observePlans(): Flow<List<WorkoutPlan>>

    /** One active day plus its plan name, or null if it does not exist / was removed. */
    fun observeDay(dayId: Long): Flow<WorkoutDayDetails?>

    suspend fun getPlan(planId: Long): WorkoutPlan?

    /** Creates a plan and all its days in one transaction. Returns the new plan id. */
    suspend fun createPlan(name: String, days: List<WorkoutDayInput>): Long

    /**
     * Saves an edited plan in one transaction. Days with a non-zero id are updated, days with
     * id 0 are inserted, and existing days missing from [days] are removed (or archived if in use).
     */
    suspend fun updatePlan(planId: Long, name: String, days: List<WorkoutDayInput>)

    suspend fun deletePlan(planId: Long)

    suspend fun addDay(planId: Long, day: WorkoutDayInput): Long

    suspend fun updateDay(dayId: Long, name: String, weekday: Weekday)

    suspend fun deleteDay(dayId: Long)
}
