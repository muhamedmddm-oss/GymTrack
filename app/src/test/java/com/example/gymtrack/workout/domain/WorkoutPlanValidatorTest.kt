package com.example.gymtrack.workout.domain

import com.example.gymtrack.workout.Weekday
import com.example.gymtrack.workout.DayFormInput
import com.example.gymtrack.workout.WorkoutPlanValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutPlanValidatorTest {

    private val pplDays = listOf(
        DayFormInput("Push", Weekday.SATURDAY),
        DayFormInput("Pull", Weekday.MONDAY),
        DayFormInput("Legs", Weekday.WEDNESDAY)
    )

    @Test
    fun validThreeDaySplit_passes() {
        val result = WorkoutPlanValidator.validate("My PPL Program", pplDays)
        assertTrue(result.isValid)
    }

    @Test
    fun blankPlanName_isRejected() {
        val result = WorkoutPlanValidator.validate("   ", pplDays)
        assertNotNull(result.planNameError)
        assertFalse(result.isValid)
    }

    @Test
    fun zeroDays_isRejected() {
        val result = WorkoutPlanValidator.validate("Plan", emptyList())
        assertNotNull(result.daysError)
        assertFalse(result.isValid)
    }

    @Test
    fun moreThanSevenDays_isRejected() {
        val eight = List(8) { DayFormInput("Day $it", Weekday.entries[it % 7]) }
        val result = WorkoutPlanValidator.validate("Plan", eight)
        assertNotNull(result.daysError)
    }

    @Test
    fun sevenDays_isAllowed() {
        val seven = Weekday.entries.map { DayFormInput("Day ${it.shortName}", it) }
        assertTrue(WorkoutPlanValidator.validate("Plan", seven).isValid)
    }

    @Test
    fun blankDayName_isRejected() {
        val days = listOf(DayFormInput("", Weekday.MONDAY))
        val result = WorkoutPlanValidator.validate("Plan", days)
        assertNotNull(result.dayErrors[0].nameError)
    }

    @Test
    fun missingWeekday_isRejected() {
        val days = listOf(DayFormInput("Push", null))
        val result = WorkoutPlanValidator.validate("Plan", days)
        assertNotNull(result.dayErrors[0].weekdayError)
    }

    @Test
    fun duplicateWeekday_isFlaggedOnTheLaterDay() {
        val days = listOf(
            DayFormInput("Push", Weekday.MONDAY),
            DayFormInput("Pull", Weekday.MONDAY)
        )
        val result = WorkoutPlanValidator.validate("Plan", days)
        assertNull(result.dayErrors[0].weekdayError)
        assertNotNull(result.dayErrors[1].weekdayError)
        assertFalse(result.isValid)
    }

    @Test
    fun overlongNames_areRejected() {
        val result = WorkoutPlanValidator.validate(
            "x".repeat(WorkoutPlanValidator.MAX_PLAN_NAME_LENGTH + 1),
            listOf(DayFormInput("y".repeat(WorkoutPlanValidator.MAX_DAY_NAME_LENGTH + 1), Weekday.FRIDAY))
        )
        assertNotNull(result.planNameError)
        assertNotNull(result.dayErrors[0].nameError)
        assertEquals(1, result.dayErrors.size)
    }
}
