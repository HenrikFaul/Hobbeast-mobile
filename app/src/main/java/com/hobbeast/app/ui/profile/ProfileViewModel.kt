package com.hobbeast.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hobbeast.app.data.model.*
import com.hobbeast.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _saveState = MutableStateFlow<ProfileSaveState>(ProfileSaveState.Idle)
    val saveState: StateFlow<ProfileSaveState> = _saveState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            userRepository.getCurrentProfile()
                .onSuccess { _profile.value = it }
        }
    }

    fun updateDisplayName(v: String) = _profile.update { it?.copy(displayName = v) }
    fun updateBio(v: String) = _profile.update { it?.copy(bio = v) }
    fun updateLocation(v: String) = _profile.update { it?.copy(location = v) }
    fun toggleLocationSharing() = _profile.update { it?.copy(locationSharing = !(it.locationSharing)) }
    fun setDistanceKm(km: Int) = _profile.update { it?.copy(distanceKm = km) }
    fun updateVisibility(v: ProfileVisibility) = _profile.update { it?.copy(profileVisibility = v) }
    fun toggleInterest(interest: String) = _profile.update { p ->
        p?.copy(interests = if (interest in p.interests) p.interests - interest else p.interests + interest)
    }

    fun save() {
        val p = _profile.value ?: return
        viewModelScope.launch {
            _saveState.value = ProfileSaveState.Saving
            userRepository.updateProfile(p)
                .onSuccess { _saveState.value = ProfileSaveState.Saved }
                .onFailure { _saveState.value = ProfileSaveState.Error(it.message ?: "Hiba") }
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            userRepository.signOut()
            onDone()
        }
    }
}

sealed interface ProfileSaveState {
    data object Idle   : ProfileSaveState
    data object Saving : ProfileSaveState
    data object Saved  : ProfileSaveState
    data class Error(val message: String) : ProfileSaveState
}
