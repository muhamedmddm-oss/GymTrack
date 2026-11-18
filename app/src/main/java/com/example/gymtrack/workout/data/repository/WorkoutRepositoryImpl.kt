package com.example.gymtrack.workout.data.repository

import com.example.gymtrack.workout.data.dao.WorkoutDao
import com.example.gymtrack.workout.data.entity.WorkoutDayEntity
import com.example.gymtrack.workout.data.entity.WorkoutPlanEntity
import com.example.gymtrack.workout.data.mapper.toDomain
import com.example.gymtrack.workout.domain.model.Weekday
import com.example.gymtrack.workout.domain.model.WorkoutDayDetails
import com.example.gymtrack.workout.domain.model.WorkoutDayInput
import com.example.gymtrack.workout.domain.model.WorkoutPlan
import com.example.gymtrack.workout.domain.repository.NoWorkoutDayUsage
import com.example.gymtrack.workout.domain.repository.WorkoutDayUsageProvider
import com.example.gymtrack.workout.domain.repository.WorkoutRepository
import com.example.gymtrack.workout.domain.validation.WorkoutPlanValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull

class WorkoutRepositoryImpl(
    private val dao: WorkoutDao,
    private val usage: WorkoutDayUsageProvider = NoWorkoutDayUsage,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : WorkoutRepository {

    override fun observePlans(): Flow<List<WorkoutPlan>> =
        combine(dao.observePlans(), dao.observeAllDays(), usage.observeExerciseCounts()) { plans, days, counts ->
            val daysByPlan = days.groupBy { it.planId }
            plans.map { plan ->
                val planDays = daysByPlan[plan.id].orEmpty().map { it.toDomain(counts[it.id] ?: 0) }
                plan.toDomain(planDays)
            }
        }

    override fun observeDay(dayId: Long): Flow<WorkoutDayDetails?> =
        combine(dao.observeDayWithPlanName(dayId), usage.observeExerciseCounts()) { row, counts ->
            row?.let { WorkoutDayDetails(it.day.toDomain(counts[it.day.id] ?: 0), it.planName) }
        }

    override suspend fun getPlan(planId: Long): WorkoutPlan? {
        val plan = dao.getPlan(planId) ?: return null
        val counts = firstCounts()
        return plan.toDomain(dao.getDays(planId).map { it.toDomain(counts[it.id] ?: 0) })
    }

    override suspend fun createPlan(name: String, days: List<WorkoutDayInput>): Long {
        checkPlanInput(name, days)
        val plan = WorkoutPlanEntity(name = name.trim(), createdAt = clock())
        val dayEntities = days.mapIndexed { index, day ->
            WorkoutDayEntity(planId = 0, name = day.name.trim(), weekday = day.weekday.name, orderIndex = index)
        }
        return dao.createPlanWithDays(plan, dayEntities)
    }

    override suspend fun updatePlan(planId: Long, name: String, days: List<WorkoutDayInput>) {
        checkPlanInput(name, days)
        require(dao.getPlan(planId) != null) { "Workout plan $planId does not exist" }

        val existing = dao.getDays(planId).associateBy { it.id }
        val updates = mutableListOf<WorkoutDayEntity>()
        val inserts = mutableListOf<WorkoutDayEntity>()
        days.forEachIndexed { index, input ->
            val current = existing[input.id]
            if (input.id != 0L && current != null) {
                updates += current.copy(name = input.name.trim(), weekday = input.weekday.name, orderIndex = index)
            } else {
                inserts += WorkoutDayEntity(
                    planId = planId,
                    name = input.name.trim(),
                    weekday = input.weekday.name,
                    orderIndex = index
                )
            }
        }

        // Days that disappeared: delete them, unless exercises/sessions still reference them.
        val keptIds = updates.map { it.id }.toSet()
        val removed = existing.values.filter { it.id !in keptIds }
        val (toArchive, toDelete) = removed.partition { usage.isDayInUse(it.id) }

        dao.applyPlanEdit(
            planId = planId,
            name = name.trim(),
            updates = updates,
            inserts = inserts,
            deleteIds = toDelete.map { it.id },
            archiveIds = toArchive.map { it.id }
        )
    }

    override suspend fun deletePlan(planId: Long) {
        // Archived days count too: a hard delete would cascade to them and wipe their history.
        val anyInUse = dao.getAllDaysIncludingArchived(planId).any { usage.isDayInUse(it.id) }
        if (anyInUse) dao.archivePlanWithDays(planId) else dao.deletePlanById(planId)
    }

    override suspend fun addDay(planId: Long, day: WorkoutDayInput): Long {
        require(day.name.isNotBlank()) { "Day name must not be blank" }
        val siblings = dao.getDays(planId)
        require(siblings.size < WorkoutPlanValidator.MAX_DAYS) { "A plan can have at most 7 days" }
        require(siblings.none { it.weekday == day.weekday.name }) { "${day.weekday.displayName} is already used" }
        return dao.insertDay(
            WorkoutDayEntity(
                planId = planId,
                name = day.name.trim(),
                weekday = day.weekday.name,
                orderIndex = dao.maxOrderIndex(planId) + 1
            )
        )
    }

    override suspend fun updateDay(dayId: Long, name: String, weekday: Weekday) {
        require(name.isNotBlank()) { "Day name must not be blank" }
        val day = dao.getDay(dayId) ?: throw IllegalArgumentException("Workout day $dayId does not exist")
        val clash = dao.getDays(day.planId).any { it.id != dayId && it.weekday == weekday.name }
        require(!clash) { "${weekday.displayName} is already used" }
        dao.updateDay(day.copy(name = name.trim(), weekday = weekday.name))
    }

    override suspend fun deleteDay(dayId: Long) {
        dao.getDay(dayId) ?: return
        if (usage.isDayInUse(dayId)) dao.archiveDay(dayId) else dao.deleteDayById(dayId)
    }

    private suspend fun firstCounts(): Map<Long, Int> =
        usage.observeExerciseCounts().firstOrNull() ?: emptyMap()

    private fun checkPlanInput(name: String, days: List<WorkoutDayInput>) {
        require(name.isNotBlank()) { "Plan name must not be blank" }
        require(days.size in WorkoutPlanValidator.MIN_DAYS..WorkoutPlanValidator.MAX_DAYS) {
            "A plan needs between 1 and 7 days"
        }
        require(days.all { it.name.isNotBlank() }) { "Day names must not be blank" }
        require(days.map { it.weekday }.toSet().size == days.size) { "Weekdays must be unique within a plan" }
    }
}
