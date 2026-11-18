package com.example.gymtrack.workout.presentation

import com.example.gymtrack.workout.domain.model.Weekday
import com.example.gymtrack.workout.presentation.viewmodel.PlanEditorEvent
import com.example.gymtrack.workout.presentation.viewmodel.WorkoutPlanEditorViewModel
import com.example.gymtrack.workout.testutil.FakeWorkoutRepository
import com.example.gymtrack.workout.testutil.FakeWorkoutRepository.Companion.day
import com.example.gymtrack.workout.testutil.FakeWorkoutRepository.Companion.plan
import com.example.gymtrack.workout.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkoutPlanEditorViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun createVm(repo: FakeWorkoutRepository = FakeWorkoutRepository()) =
        WorkoutPlanEditorViewModel(repo, planId = null) to repo

    private fun WorkoutPlanEditorViewModel.fillValidPpl() {
        onPlanNameChange("My PPL Program")
        val keys = uiState.value.days.map { it.key }
        onDayNameChange(keys[0], "Push"); onWeekdaySelected(keys[0], Weekday.SATURDAY)
        onDayNameChange(keys[1], "Pull"); onWeekdaySelected(keys[1], Weekday.MONDAY)
        onDayNameChange(keys[2], "Legs"); onWeekdaySelected(keys[2], Weekday.WEDNESDAY)
    }

    @Test
    fun createMode_startsWithThreeBlankDays_andNoChanges() {
        val (vm, _) = createVm()
        assertEquals(3, vm.uiState.value.days.size)
        assertFalse(vm.uiState.value.hasChanges)
        assertFalse(vm.uiState.value.isEditMode)
    }

    @Test
    fun selectingFiveDays_generatesFiveEntries_andSevenIsTheMax() {
        val (vm, _) = createVm()
        vm.onTrainingDaysSelected(5)
        assertEquals(5, vm.uiState.value.days.size)
        vm.onTrainingDaysSelected(12)
        assertEquals(7, vm.uiState.value.days.size)
        vm.onAddDay()
        assertEquals(7, vm.uiState.value.days.size)
    }

    @Test
    fun reducingDayCount_removesFromTheEnd_andKeepsEarlierInput() {
        val (vm, _) = createVm()
        vm.onTrainingDaysSelected(5)
        val first = vm.uiState.value.days.first().key
        vm.onDayNameChange(first, "Push")
        vm.onTrainingDaysSelected(2)
        assertEquals(2, vm.uiState.value.days.size)
        assertEquals("Push", vm.uiState.value.days.first().name)
    }

    @Test
    fun duplicateWeekday_isIgnored() {
        val (vm, _) = createVm()
        val keys = vm.uiState.value.days.map { it.key }
        vm.onWeekdaySelected(keys[0], Weekday.MONDAY)
        vm.onWeekdaySelected(keys[1], Weekday.MONDAY)
        assertEquals(Weekday.MONDAY, vm.uiState.value.days[0].weekday)
        assertNull(vm.uiState.value.days[1].weekday)
    }

    @Test
    fun saveWithIncompleteData_showsErrors_andDoesNotCallRepository() {
        val (vm, repo) = createVm()
        vm.onSave()
        assertTrue(vm.uiState.value.showErrors)
        assertFalse(vm.uiState.value.validation.isValid)
        assertTrue(repo.createdPlans.isEmpty())
    }

    @Test
    fun saveValidThreeDaySplit_createsPlan_andEmitsSaved() = runTest {
        val (vm, repo) = createVm()
        vm.fillValidPpl()
        vm.onSave()

        assertEquals(1, repo.createdPlans.size)
        val (name, days) = repo.createdPlans.single()
        assertEquals("My PPL Program", name)
        assertEquals(listOf("Push", "Pull", "Legs"), days.map { it.name })
        assertEquals(listOf(Weekday.SATURDAY, Weekday.MONDAY, Weekday.WEDNESDAY), days.map { it.weekday })
        assertEquals(PlanEditorEvent.Saved, vm.events.first())
    }

    @Test
    fun saveFiveDaySplit_createsFiveDays() {
        val (vm, repo) = createVm()
        vm.onPlanNameChange("Bro Split")
        vm.onTrainingDaysSelected(5)
        vm.uiState.value.days.forEachIndexed { i, d ->
            vm.onDayNameChange(d.key, "Day ${i + 1}")
            vm.onWeekdaySelected(d.key, Weekday.entries[i])
        }
        vm.onSave()
        assertEquals(5, repo.createdPlans.single().second.size)
    }

    @Test
    fun saveFailure_keepsData_andReportsError() {
        val repo = FakeWorkoutRepository().apply { shouldFail = true }
        val (vm, _) = createVm(repo)
        vm.fillValidPpl()
        vm.onSave()
        assertNotNull(vm.uiState.value.saveError)
        assertFalse(vm.uiState.value.isSaving)
        assertEquals("My PPL Program", vm.uiState.value.planName)
    }

    @Test
    fun backWithoutChanges_closesImmediately() = runTest {
        val (vm, _) = createVm()
        vm.onBackRequested()
        assertEquals(PlanEditorEvent.Close, vm.events.first())
    }

    @Test
    fun backWithChanges_asksBeforeDiscarding() = runTest {
        val (vm, _) = createVm()
        vm.onPlanNameChange("Something")
        assertTrue(vm.uiState.value.hasChanges)
        vm.onBackRequested()
        assertTrue(vm.uiState.value.showDiscardDialog)

        vm.onDiscardConfirm()
        assertFalse(vm.uiState.value.showDiscardDialog)
        assertEquals(PlanEditorEvent.Close, vm.events.first())
    }

    // ---- Edit mode ----

    private val existing = plan(
        7, "PPL",
        day(1, 7, "Push", Weekday.SATURDAY, 0, exercises = 4),
        day(2, 7, "Pull", Weekday.MONDAY, 1),
        day(3, 7, "Legs", Weekday.WEDNESDAY, 2)
    )

    @Test
    fun editMode_loadsExistingPlan_preservingIds() {
        val vm = WorkoutPlanEditorViewModel(FakeWorkoutRepository(listOf(existing)), planId = 7)
        val state = vm.uiState.value
        assertTrue(state.isEditMode)
        assertFalse(state.isLoading)
        assertEquals("PPL", state.planName)
        assertEquals(listOf(1L, 2L, 3L), state.days.map { it.id })
        assertFalse(state.hasChanges)
    }

    @Test
    fun editMode_unknownPlan_showsLoadError() {
        val vm = WorkoutPlanEditorViewModel(FakeWorkoutRepository(), planId = 99)
        assertNotNull(vm.uiState.value.loadError)
    }

    @Test
    fun editMode_removingDayWithExercises_requiresConfirmation() {
        val repo = FakeWorkoutRepository(listOf(existing))
        val vm = WorkoutPlanEditorViewModel(repo, planId = 7)
        val pushKey = vm.uiState.value.days.first { it.name == "Push" }.key

        vm.onRemoveDay(pushKey)
        assertEquals(3, vm.uiState.value.days.size)          // nothing removed yet
        assertNotNull(vm.uiState.value.pendingRemovalKeys)
        assertEquals(4, vm.uiState.value.pendingRemovalExerciseCount)

        vm.onConfirmRemoval()
        assertEquals(2, vm.uiState.value.days.size)
        assertNull(vm.uiState.value.pendingRemovalKeys)
    }

    @Test
    fun editMode_cancellingRemoval_keepsTheDay() {
        val vm = WorkoutPlanEditorViewModel(FakeWorkoutRepository(listOf(existing)), planId = 7)
        val pushKey = vm.uiState.value.days.first { it.name == "Push" }.key
        vm.onRemoveDay(pushKey)
        vm.onDismissRemoval()
        assertEquals(3, vm.uiState.value.days.size)
    }

    @Test
    fun editMode_removingEmptyDay_happensImmediately() {
        val vm = WorkoutPlanEditorViewModel(FakeWorkoutRepository(listOf(existing)), planId = 7)
        val pullKey = vm.uiState.value.days.first { it.name == "Pull" }.key
        vm.onRemoveDay(pullKey)
        assertEquals(2, vm.uiState.value.days.size)
        assertNull(vm.uiState.value.pendingRemovalKeys)
    }

    @Test
    fun editMode_save_updatesPlanKeepingIdsAndAddingNewDay() {
        val repo = FakeWorkoutRepository(listOf(existing))
        val vm = WorkoutPlanEditorViewModel(repo, planId = 7)
        vm.onPlanNameChange("PPL v2")
        val pullKey = vm.uiState.value.days.first { it.name == "Pull" }.key
        vm.onDayNameChange(pullKey, "Pull Heavy")
        vm.onAddDay()
        val newKey = vm.uiState.value.days.last().key
        vm.onDayNameChange(newKey, "Arms")
        vm.onWeekdaySelected(newKey, Weekday.FRIDAY)
        vm.onSave()

        val (planId, name, days) = repo.updatedPlans.single()
        assertEquals(7L, planId)
        assertEquals("PPL v2", name)
        assertEquals(listOf(1L, 2L, 3L, 0L), days.map { it.id })
        assertEquals("Pull Heavy", days[1].name)
        assertEquals("Arms", days[3].name)
    }
}
