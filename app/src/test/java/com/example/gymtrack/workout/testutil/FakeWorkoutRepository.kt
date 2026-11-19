package com.example.gymtrack.workout.testutil

import com.example.gymtrack.workout.Weekday
import com.example.gymtrack.workout.WorkoutDay
import com.example.gymtrack.workout.WorkoutDayDetails
import com.example.gymtrack.workout.WorkoutDayInput
import com.example.gymtrack.workout.WorkoutPlan
import com.example.gymtrack.workout.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Simple in-memory repository for ViewModel tests. Records calls and can be told to fail. */
class FakeWorkoutRepository(initialPlans: List<WorkoutPlan> = emptyList()) : WorkoutRepository {

    val plans = MutableStateFlow(initialPlans)
    var shouldFail = false

    var createdPlans = mutableListOf<Pair<String, List<WorkoutDayInput>>>()
    var updatedPlans = mutableListOf<Triple<Long, String, List<WorkoutDayInput>>>()
    var deletedPlanIds = mutableListOf<Long>()
    var deletedDayIds = mutableListOf<Long>()

    private fun failIfNeeded() {
        if (shouldFail) throw IllegalStateException("boom")
    }

    override fun observePlans() = plans

    override fun observeDay(dayId: Long) = plans.map { list ->
        list.firstNotNullOfOrNull { plan ->
            plan.days.firstOrNull { it.id == dayId }?.let { WorkoutDayDetails(it, plan.name) }
        }
    }

    override suspend fun getPlan(planId: Long): WorkoutPlan? {
        failIfNeeded()
        return plans.value.firstOrNull { it.id == planId }
    }

    override suspend fun createPlan(name: String, days: List<WorkoutDayInput>): Long {
        failIfNeeded()
        createdPlans += name to days
        return 1L
    }

    override suspend fun updatePlan(planId: Long, name: String, days: List<WorkoutDayInput>) {
        failIfNeeded()
        updatedPlans += Triple(planId, name, days)
    }

    override suspend fun deletePlan(planId: Long) {
        failIfNeeded()
        deletedPlanIds += planId
    }

    override suspend fun addDay(planId: Long, day: WorkoutDayInput): Long = 0

    override suspend fun updateDay(dayId: Long, name: String, weekday: Weekday) = Unit

    override suspend fun deleteDay(dayId: Long) {
        failIfNeeded()
        deletedDayIds += dayId
    }

    companion object {
        fun plan(id: Long, name: String, vararg days: WorkoutDay) = WorkoutPlan(id, name, 0L, days.toList())
        fun day(id: Long, planId: Long, name: String, weekday: Weekday, order: Int, exercises: Int = 0) =
            WorkoutDay(id, planId, name, weekday, order, exercises)
    }
}
