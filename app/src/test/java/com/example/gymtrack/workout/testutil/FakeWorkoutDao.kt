package com.example.gymtrack.workout.testutil

import com.example.gymtrack.workout.DayWithPlanName
import com.example.gymtrack.workout.WorkoutDao
import com.example.gymtrack.workout.WorkoutDayEntity
import com.example.gymtrack.workout.WorkoutPlanEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory stand-in for the Room DAO so the repository logic can be tested on the JVM.
 * The real @Transaction methods of [WorkoutDao] are inherited and run on top of these fakes.
 */
class FakeWorkoutDao : WorkoutDao() {

    val plans = MutableStateFlow<List<WorkoutPlanEntity>>(emptyList())
    val days = MutableStateFlow<List<WorkoutDayEntity>>(emptyList())
    private var nextPlanId = 1L
    private var nextDayId = 1L

    override fun observePlans(): Flow<List<WorkoutPlanEntity>> =
        plans.map { list -> list.filter { !it.isArchived }.sortedByDescending { it.createdAt } }

    override fun observeAllDays(): Flow<List<WorkoutDayEntity>> =
        days.map { list -> list.filter { !it.isArchived }.sortedWith(compareBy({ it.planId }, { it.orderIndex })) }

    override fun observeDayWithPlanName(dayId: Long): Flow<DayWithPlanName?> =
        combineLatest { p, d ->
            val day = d.firstOrNull { it.id == dayId && !it.isArchived } ?: return@combineLatest null
            val plan = p.firstOrNull { it.id == day.planId && !it.isArchived } ?: return@combineLatest null
            DayWithPlanName(day, plan.name)
        }

    private fun <T> combineLatest(transform: (List<WorkoutPlanEntity>, List<WorkoutDayEntity>) -> T): Flow<T> =
        kotlinx.coroutines.flow.combine(plans, days, transform)

    override suspend fun getPlan(planId: Long) = plans.value.firstOrNull { it.id == planId && !it.isArchived }

    override suspend fun getDays(planId: Long) =
        days.value.filter { it.planId == planId && !it.isArchived }.sortedBy { it.orderIndex }

    override suspend fun getAllDaysIncludingArchived(planId: Long) = days.value.filter { it.planId == planId }

    override suspend fun getDay(dayId: Long) = days.value.firstOrNull { it.id == dayId && !it.isArchived }

    override suspend fun maxOrderIndex(planId: Long) =
        days.value.filter { it.planId == planId }.maxOfOrNull { it.orderIndex } ?: -1

    override suspend fun insertPlan(plan: WorkoutPlanEntity): Long {
        val id = nextPlanId++
        plans.value = plans.value + plan.copy(id = id)
        return id
    }

    override suspend fun insertDay(day: WorkoutDayEntity): Long {
        val id = nextDayId++
        days.value = days.value + day.copy(id = id)
        return id
    }

    override suspend fun insertDays(days: List<WorkoutDayEntity>) {
        days.forEach { insertDay(it) }
    }

    override suspend fun updateDay(day: WorkoutDayEntity) {
        days.value = days.value.map { if (it.id == day.id) day else it }
    }

    override suspend fun renamePlan(planId: Long, name: String) {
        plans.value = plans.value.map { if (it.id == planId) it.copy(name = name) else it }
    }

    override suspend fun deletePlanById(planId: Long) {
        plans.value = plans.value.filterNot { it.id == planId }
        days.value = days.value.filterNot { it.planId == planId } // ON DELETE CASCADE
    }

    override suspend fun deleteDayById(dayId: Long) {
        days.value = days.value.filterNot { it.id == dayId }
    }

    override suspend fun archiveDay(dayId: Long) {
        days.value = days.value.map { if (it.id == dayId) it.copy(isArchived = true) else it }
    }

    override suspend fun archivePlan(planId: Long) {
        plans.value = plans.value.map { if (it.id == planId) it.copy(isArchived = true) else it }
    }

    override suspend fun archiveDaysOfPlan(planId: Long) {
        days.value = days.value.map { if (it.planId == planId) it.copy(isArchived = true) else it }
    }
}
