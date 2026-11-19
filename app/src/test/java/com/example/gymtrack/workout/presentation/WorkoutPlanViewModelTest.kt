package com.example.gymtrack.workout.presentation

import com.example.gymtrack.workout.Weekday
import com.example.gymtrack.workout.WorkoutPlanViewModel
import com.example.gymtrack.workout.testutil.FakeWorkoutRepository
import com.example.gymtrack.workout.testutil.FakeWorkoutRepository.Companion.day
import com.example.gymtrack.workout.testutil.FakeWorkoutRepository.Companion.plan
import com.example.gymtrack.workout.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkoutPlanViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val ppl = plan(
        1, "PPL",
        day(10, 1, "Push", Weekday.SATURDAY, 0, exercises = 3),
        day(11, 1, "Pull", Weekday.MONDAY, 1)
    )
    private val single = plan(2, "Solo", day(20, 2, "Full body", Weekday.FRIDAY, 0))

    @Test
    fun noPlans_showsEmptyList() {
        val vm = WorkoutPlanViewModel(FakeWorkoutRepository())
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.plans.isEmpty())
    }

    @Test
    fun plans_areExposed_andUpdateImmediately() {
        val repo = FakeWorkoutRepository(listOf(ppl))
        val vm = WorkoutPlanViewModel(repo)
        assertEquals(1, vm.uiState.value.plans.size)

        repo.plans.value = listOf(ppl, single)
        assertEquals(2, vm.uiState.value.plans.size)
    }

    @Test
    fun deletePlan_requiresConfirmation() {
        val repo = FakeWorkoutRepository(listOf(ppl))
        val vm = WorkoutPlanViewModel(repo)

        vm.onDeletePlanClick(ppl)
        assertEquals(ppl, vm.uiState.value.planPendingDelete)
        assertTrue(repo.deletedPlanIds.isEmpty())          // not deleted yet

        vm.onDeletePlanConfirm()
        assertEquals(listOf(1L), repo.deletedPlanIds)
        assertNull(vm.uiState.value.planPendingDelete)
    }

    @Test
    fun cancelDeletePlan_deletesNothing() {
        val repo = FakeWorkoutRepository(listOf(ppl))
        val vm = WorkoutPlanViewModel(repo)
        vm.onDeletePlanClick(ppl)
        vm.onDeletePlanDismiss()
        assertTrue(repo.deletedPlanIds.isEmpty())
        assertNull(vm.uiState.value.planPendingDelete)
    }

    @Test
    fun deleteDay_requiresConfirmation() {
        val repo = FakeWorkoutRepository(listOf(ppl))
        val vm = WorkoutPlanViewModel(repo)
        val push = ppl.days.first()

        vm.onDeleteDayClick(push)
        assertEquals(push, vm.uiState.value.dayPendingDelete)
        vm.onDeleteDayConfirm()
        assertEquals(listOf(10L), repo.deletedDayIds)
    }

    @Test
    fun deletingTheLastDayOfAPlan_isBlockedWithAMessage() {
        val repo = FakeWorkoutRepository(listOf(single))
        val vm = WorkoutPlanViewModel(repo)
        vm.onDeleteDayClick(single.days.first())
        assertNull(vm.uiState.value.dayPendingDelete)
        assertNotNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun deleteFailure_isReportedWithoutCrashing() {
        val repo = FakeWorkoutRepository(listOf(ppl))
        val vm = WorkoutPlanViewModel(repo)
        repo.shouldFail = true
        vm.onDeletePlanClick(ppl)
        vm.onDeletePlanConfirm()
        assertNotNull(vm.uiState.value.errorMessage)
        vm.onErrorShown()
        assertNull(vm.uiState.value.errorMessage)
    }
}
