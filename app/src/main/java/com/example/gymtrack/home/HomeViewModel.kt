package com.example.gymtrack.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtrack.data.entity.UserProfileEntity
import com.example.gymtrack.data.entity.WorkoutSessionEntity
import com.example.gymtrack.data.repository.UserProfileRepository
import com.example.gymtrack.data.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map

class HomeViewModel(
    profiles: UserProfileRepository,
    sessions: WorkoutSessionRepository
) : ViewModel() {
    val profile: StateFlow<UserProfileEntity?> = profiles.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val completedSessions: StateFlow<Int> = sessions.observeFinishedSessionCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val lastWorkout: StateFlow<WorkoutSessionEntity?> = sessions.observeLastFinishedSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val weekStart = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L
    val weeklySessions: StateFlow<Int> = sessions.observeFinishedSessionsBetween(
        weekStart, System.currentTimeMillis() + 1L
    ).let { flow ->
        flow.map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    }
}
