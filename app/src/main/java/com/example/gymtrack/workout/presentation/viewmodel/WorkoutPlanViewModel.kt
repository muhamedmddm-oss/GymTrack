package com.example.gymtrack.workout.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtrack.workout.domain.model.WorkoutDay
import com.example.gymtrack.workout.domain.model.WorkoutPlan
import com.example.gymtrack.workout.domain.repository.WorkoutRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutPlansUiState(
    val isLoading: Boolean = true,
    val plans: List<WorkoutPlan> = emptyList(),
    val planPendingDelete: WorkoutPlan? = null,
    val dayPendingDelete: WorkoutDay? = null,
    val errorMessage: String? = null
)

/** State holder for the "My Workout Plans" screen (list + delete). */
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

    // ---- Plan deletion ----

    fun onDeletePlanClick(plan: WorkoutPlan) = _uiState.update { it.copy(planPendingDelete = plan) }

    fun onDeletePlanDismiss() = _uiState.update { it.copy(planPendingDelete = null) }

    fun onDeletePlanConfirm() {
        val plan = _uiState.value.planPendingDelete ?: return
        _uiState.update { it.copy(planPendingDelete = null) }
        runSafely("Could not delete the plan. Please try again.") { repository.deletePlan(plan.id) }
    }

    // ---- Day deletion ----

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
