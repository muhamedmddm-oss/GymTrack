package com.example.gymtrack.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow


data class WorkoutDayDetailsUiState(
    val isLoading: Boolean = true,
    val details: WorkoutDayDetails? = null,
    val errorMessage: String? = null
) {
    val notFound: Boolean get() = !isLoading && details == null && errorMessage == null
}

class WorkoutDayDetailsViewModel(
    private val repository: WorkoutRepository,
    dayId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutDayDetailsUiState())
    val uiState: StateFlow<WorkoutDayDetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeDay(dayId)
                .catch { _uiState.update { s -> s.copy(isLoading = false, errorMessage = "Could not load this workout day") } }
                .collect { details -> _uiState.update { it.copy(isLoading = false, details = details) } }
        }
    }
}


data class DayDraft(
    val key: Long,
    val id: Long = 0,
    val name: String = "",
    val weekday: Weekday? = null,
    val exerciseCount: Int = 0
)

data class PlanEditorUiState(
    val isEditMode: Boolean,
    val isLoading: Boolean,
    val loadError: String? = null,
    val planName: String = "",
    val days: List<DayDraft> = emptyList(),
    val validation: PlanValidation = PlanValidation(),

    val showErrors: Boolean = false,
    val hasChanges: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,

    val pendingRemovalKeys: List<Long>? = null,
    val pendingRemovalExerciseCount: Int = 0,
    val showDiscardDialog: Boolean = false
)

sealed interface PlanEditorEvent {
    data object Saved : PlanEditorEvent
    data object Close : PlanEditorEvent
}


class WorkoutPlanEditorViewModel(
    private val repository: WorkoutRepository,
    private val planId: Long?
) : ViewModel() {

    private data class Snapshot(val name: String, val days: List<DayDraft>)

    private var nextKey = 1L
    private var initialSnapshot = Snapshot("", emptyList())

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<PlanEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<PlanEditorEvent>(Channel.BUFFERED)
    val events: Flow<PlanEditorEvent> = _events.receiveAsFlow()

    init {
        if (planId != null) loadPlan(planId)
    }


    fun onPlanNameChange(value: String) = mutate { it.copy(planName = value) }

    /** Called by the "1..7" selector. Adds blank days or removes days from the end. */
    fun onTrainingDaysSelected(count: Int) {
        val target = count.coerceIn(WorkoutPlanValidator.MIN_DAYS, WorkoutPlanValidator.MAX_DAYS)
        val current = _uiState.value.days
        when {
            target == current.size -> Unit
            target > current.size -> mutate { it.copy(days = it.days + List(target - it.days.size) { newBlankDay() }) }
            else -> requestRemoval(current.drop(target).map { it.key })
        }
    }

    fun onAddDay() {
        val size = _uiState.value.days.size
        if (size < WorkoutPlanValidator.MAX_DAYS) onTrainingDaysSelected(size + 1)
    }

    fun onRemoveDay(key: Long) {
        if (_uiState.value.days.size > WorkoutPlanValidator.MIN_DAYS) requestRemoval(listOf(key))
    }

    fun onDayNameChange(key: Long, name: String) = updateDay(key) { it.copy(name = name) }

    fun onWeekdaySelected(key: Long, weekday: Weekday) {
        // Weekdays already used by another day are disabled in the UI; guard here as well.
        val usedElsewhere = _uiState.value.days.any { it.key != key && it.weekday == weekday }
        if (!usedElsewhere) updateDay(key) { it.copy(weekday = weekday) }
    }


    fun onConfirmRemoval() {
        val keys = _uiState.value.pendingRemovalKeys ?: return
        _uiState.update { it.copy(pendingRemovalKeys = null, pendingRemovalExerciseCount = 0) }
        applyRemoval(keys)
    }

    fun onDismissRemoval() =
        _uiState.update { it.copy(pendingRemovalKeys = null, pendingRemovalExerciseCount = 0) }


    fun onBackRequested() {
        val state = _uiState.value
        if (state.hasChanges && !state.isSaving) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            _events.trySend(PlanEditorEvent.Close)
        }
    }

    fun onDiscardDismiss() = _uiState.update { it.copy(showDiscardDialog = false) }

    fun onDiscardConfirm() {
        _uiState.update { it.copy(showDiscardDialog = false) }
        _events.trySend(PlanEditorEvent.Close)
    }


    fun onSave() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading || state.loadError != null) return
        if (!state.validation.isValid) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        _uiState.update { it.copy(isSaving = true, showErrors = true, saveError = null) }

        val name = state.planName.trim()
        val inputs = state.days.mapNotNull { day ->
            day.weekday?.let { WorkoutDayInput(id = day.id, name = day.name.trim(), weekday = it) }
        }
        viewModelScope.launch {
            try {
                if (planId == null) repository.createPlan(name, inputs) else repository.updatePlan(planId, name, inputs)
                _uiState.update { it.copy(isSaving = false, hasChanges = false) }
                _events.send(PlanEditorEvent.Saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = "Could not save the plan. Please try again.") }
            }
        }
    }

    fun onSaveErrorShown() = _uiState.update { it.copy(saveError = null) }


    private fun createInitialState(): PlanEditorUiState {
        if (planId != null) return PlanEditorUiState(isEditMode = true, isLoading = true)
        val days = List(DEFAULT_DAY_COUNT) { newBlankDay() }
        initialSnapshot = snapshot("", days)
        return PlanEditorUiState(isEditMode = false, isLoading = false, days = days).withValidation()
    }

    private fun loadPlan(id: Long) {
        viewModelScope.launch {
            try {
                val plan = repository.getPlan(id)
                if (plan == null) {
                    _uiState.update { it.copy(isLoading = false, loadError = "This workout plan no longer exists.") }
                    return@launch
                }
                val drafts = plan.days.map { day ->
                    DayDraft(
                        key = nextKey++,
                        id = day.id,
                        name = day.name,
                        weekday = day.weekday,
                        exerciseCount = day.exerciseCount
                    )
                }
                initialSnapshot = snapshot(plan.name, drafts)
                _uiState.update {
                    it.copy(isLoading = false, planName = plan.name, days = drafts, hasChanges = false).withValidation()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadError = "Could not load this workout plan.") }
            }
        }
    }

    private fun requestRemoval(keys: List<Long>) {
        val removed = _uiState.value.days.filter { it.key in keys }
        val savedDaysWithExercises = removed.filter { it.id != 0L && it.exerciseCount > 0 }
        if (savedDaysWithExercises.isEmpty()) {
            applyRemoval(keys)
        } else {
            _uiState.update {
                it.copy(
                    pendingRemovalKeys = keys,
                    pendingRemovalExerciseCount = savedDaysWithExercises.sumOf { d -> d.exerciseCount }
                )
            }
        }
    }

    private fun applyRemoval(keys: List<Long>) = mutate { state ->
        state.copy(days = state.days.filterNot { it.key in keys })
    }

    private fun updateDay(key: Long, change: (DayDraft) -> DayDraft) = mutate { state ->
        state.copy(days = state.days.map { if (it.key == key) change(it) else it })
    }

    private fun newBlankDay() = DayDraft(key = nextKey++)

    private fun mutate(transform: (PlanEditorUiState) -> PlanEditorUiState) {
        _uiState.update { old ->
            transform(old)
                .withValidation()
                .let { it.copy(hasChanges = snapshot(it.planName, it.days) != initialSnapshot) }
        }
    }

    private fun PlanEditorUiState.withValidation(): PlanEditorUiState = copy(
        validation = WorkoutPlanValidator.validate(
            planName = planName,
            days = days.map { DayFormInput(it.name, it.weekday) }
        )
    )

    private fun snapshot(name: String, days: List<DayDraft>) =
        Snapshot(name, days.map { it.copy(key = 0, exerciseCount = 0) })

    private companion object {
        const val DEFAULT_DAY_COUNT = 3
    }
}


data class WorkoutPlansUiState(
    val isLoading: Boolean = true,
    val plans: List<WorkoutPlan> = emptyList(),
    val planPendingDelete: WorkoutPlan? = null,
    val dayPendingDelete: WorkoutDay? = null,
    val errorMessage: String? = null
)

class WorkoutPlanViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutPlansUiState())
    val uiState: StateFlow<WorkoutPlansUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observePlans()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Could not load your plans") }
                }
                .collect { plans -> _uiState.update { it.copy(isLoading = false, plans = plans) } }
        }
    }


    fun onDeletePlanClick(plan: WorkoutPlan) = _uiState.update { it.copy(planPendingDelete = plan) }

    fun onDeletePlanDismiss() = _uiState.update { it.copy(planPendingDelete = null) }

    fun onDeletePlanConfirm() {
        val plan = _uiState.value.planPendingDelete ?: return
        _uiState.update { it.copy(planPendingDelete = null) }
        runSafely("Could not delete the plan. Please try again.") { repository.deletePlan(plan.id) }
    }


    fun onDeleteDayClick(day: WorkoutDay) {
        val plan = _uiState.value.plans.firstOrNull { it.id == day.planId }
        if (plan != null && plan.days.size <= 1) {
            _uiState.update {
                it.copy(errorMessage = "A plan needs at least one day. Delete the whole plan instead.")
            }
        } else {
            _uiState.update { it.copy(dayPendingDelete = day) }
        }
    }

    fun onDeleteDayDismiss() = _uiState.update { it.copy(dayPendingDelete = null) }

    fun onDeleteDayConfirm() {
        val day = _uiState.value.dayPendingDelete ?: return
        _uiState.update { it.copy(dayPendingDelete = null) }
        runSafely("Could not delete the workout day. Please try again.") { repository.deleteDay(day.id) }
    }

    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }

    private fun runSafely(errorMessage: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = errorMessage) }
            }
        }
    }
}
