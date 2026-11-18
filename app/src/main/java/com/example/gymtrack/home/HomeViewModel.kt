package com.example.gymtrack.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtrack.data.entity.UserProfileEntity
import com.example.gymtrack.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** State holder for the Home screen. For now it exposes the saved user profile (null until it exists). */
class HomeViewModel(profiles: UserProfileRepository) : ViewModel() {

    val profile: StateFlow<UserProfileEntity?> = profiles.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
