package com.example.gymtrack.workout.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtrack.workout.domain.model.WorkoutDayDetails
import com.example.gymtrack.workout.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
