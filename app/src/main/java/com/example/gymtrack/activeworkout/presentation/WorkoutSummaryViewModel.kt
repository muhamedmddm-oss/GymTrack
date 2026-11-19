package com.example.gymtrack.activeworkout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtrack.data.dao.SessionWithDetails
import com.example.gymtrack.data.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class WorkoutSummaryUiState(
    val isLoading: Boolean = true,
    val details: SessionWithDetails? = null
)

/** Numbers shown at the top of the Workout Summary. */
data class SummaryStats(
    val totalExercises: Int,
    val completedExercises: Int,
    val totalSets: Int,
    val completedSets: Int,
    /** Sum of reps x weight over the COMPLETED sets. */
    val totalVolumeKg: Double
)

fun buildSummaryStats(details: SessionWithDetails): SummaryStats {
    val allSets = details.exercises.flatMap { it.sets }
    val doneSets = allSets.filter { it.isCompleted }
    return SummaryStats(
        totalExercises = details.exercises.size,
        completedExercises = details.exercises.count { ex -> ex.sets.isNotEmpty() && ex.sets.all { it.isCompleted } },
        totalSets = allSets.size,
        completedSets = doneSets.size,
        totalVolumeKg = doneSets.sumOf { it.reps * (it.weightKg ?: 0.0) }
    )
}

/** Reads the saved workout from Room by its id (the id is the only thing passed through navigation). */
class WorkoutSummaryViewModel(
    sessions: WorkoutSessionRepository,
    sessionId: Long
) : ViewModel() {

    val uiState: StateFlow<WorkoutSummaryUiState> = sessions.observeSessionDetails(sessionId)
        .map { WorkoutSummaryUiState(isLoading = false, details = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutSummaryUiState())
}
