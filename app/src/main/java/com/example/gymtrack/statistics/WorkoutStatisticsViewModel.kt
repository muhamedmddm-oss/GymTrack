package com.example.gymtrack.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtrack.data.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class WorkoutStatisticsViewModel(repository: WorkoutSessionRepository) : ViewModel() {
    private val range = MutableStateFlow(StatisticsRange.DAYS_30)
    val selectedRange = range
    val uiState = combine(repository.observeAllSessionDetails(), range) { sessions, selected ->
        calculateWorkoutStatistics(sessions, selected) to false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), calculateWorkoutStatistics(emptyList(), StatisticsRange.DAYS_30) to true)

    fun selectRange(value: StatisticsRange) { range.value = value }
}
