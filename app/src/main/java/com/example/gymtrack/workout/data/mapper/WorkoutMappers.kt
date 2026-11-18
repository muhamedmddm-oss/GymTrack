package com.example.gymtrack.workout.data.mapper

import com.example.gymtrack.workout.data.entity.WorkoutDayEntity
import com.example.gymtrack.workout.data.entity.WorkoutPlanEntity
import com.example.gymtrack.workout.domain.model.Weekday
import com.example.gymtrack.workout.domain.model.WorkoutDay
import com.example.gymtrack.workout.domain.model.WorkoutPlan

internal fun WorkoutDayEntity.toDomain(exerciseCount: Int = 0): WorkoutDay = WorkoutDay(
    id = id,
    planId = planId,
    name = name,
    // Unknown stored values fall back to Monday instead of crashing the whole screen.
    weekday = Weekday.fromName(weekday) ?: Weekday.MONDAY,
    orderIndex = orderIndex,
    exerciseCount = exerciseCount
)

internal fun WorkoutPlanEntity.toDomain(days: List<WorkoutDay>): WorkoutPlan = WorkoutPlan(
    id = id,
    name = name,
    createdAt = createdAt,
    days = days
)
