package com.example.gymtrack.workout.domain.model

/** A workout plan (training split) with its active workout days, ordered by [WorkoutDay.orderIndex]. */
data class WorkoutPlan(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val days: List<WorkoutDay>
)

/**
 * One training day inside a plan, e.g. "Push" on Saturday.
 * [exerciseCount] comes from Person 3's module (0 until it is connected).
 */
data class WorkoutDay(
    val id: Long,
    val planId: Long,
    val name: String,
    val weekday: Weekday,
    val orderIndex: Int,
    val exerciseCount: Int = 0
)

/** A day together with the name of the plan it belongs to (used by the details screen). */
data class WorkoutDayDetails(
    val day: WorkoutDay,
    val planName: String
)

/**
 * Input used to create or update a day. [id] is 0 for a brand-new day.
 * The list order of inputs defines the day order inside the plan.
 */
data class WorkoutDayInput(
    val id: Long = 0,
    val name: String,
    val weekday: Weekday
)
