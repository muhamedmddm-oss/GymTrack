package com.example.gymtrack.workout.domain.validation

import com.example.gymtrack.workout.domain.model.Weekday

/** What the user typed for one day (weekday is null until chosen). */
data class DayFormInput(val name: String, val weekday: Weekday?)

data class DayValidation(
    val nameError: String? = null,
    val weekdayError: String? = null
) {
    val isValid: Boolean get() = nameError == null && weekdayError == null
}

data class PlanValidation(
    val planNameError: String? = null,
    val daysError: String? = null,
    val dayErrors: List<DayValidation> = emptyList()
) {
    val isValid: Boolean
        get() = planNameError == null && daysError == null && dayErrors.all { it.isValid }
}

/** Pure validation rules for creating/editing a plan. No Android dependencies, easy to unit test. */
object WorkoutPlanValidator {
    const val MIN_DAYS = 1
    const val MAX_DAYS = 7
    const val MAX_PLAN_NAME_LENGTH = 40
    const val MAX_DAY_NAME_LENGTH = 30

    fun validate(planName: String, days: List<DayFormInput>): PlanValidation {
        val planNameError = when {
            planName.isBlank() -> "Enter a workout plan name"
            planName.trim().length > MAX_PLAN_NAME_LENGTH ->
                "Plan name must be $MAX_PLAN_NAME_LENGTH characters or fewer"
            else -> null
        }

        val daysError = when {
            days.size < MIN_DAYS -> "Select at least $MIN_DAYS training day"
            days.size > MAX_DAYS -> "A plan can have at most $MAX_DAYS training days"
            else -> null
        }

        val seen = mutableSetOf<Weekday>()
        val dayErrors = days.map { day ->
            val nameError = when {
                day.name.isBlank() -> "Enter a name for this day"
                day.name.trim().length > MAX_DAY_NAME_LENGTH ->
                    "Day name must be $MAX_DAY_NAME_LENGTH characters or fewer"
                else -> null
            }
            val weekday = day.weekday
            val weekdayError = when {
                weekday == null -> "Choose a weekday"
                !seen.add(weekday) -> "${weekday.displayName} is already used by another day"
                else -> null
            }
            DayValidation(nameError, weekdayError)
        }

        return PlanValidation(planNameError, daysError, dayErrors)
    }
}
