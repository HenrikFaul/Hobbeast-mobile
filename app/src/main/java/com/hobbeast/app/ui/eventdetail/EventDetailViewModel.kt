package com.hobbeast.app.ui.eventdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hobbeast.app.data.model.*
import com.hobbeast.app.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"])

    private val _uiState = MutableStateFlow<EventDetailUiState>(EventDetailUiState.Loading)
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    private val _participationLoading = MutableStateFlow(false)
    val participationLoading: StateFlow<Boolean> = _participationLoading.asStateFlow()

    init { loadEvent() }

    fun loadEvent() {
        viewModelScope.launch {
            _uiState.value = EventDetailUiState.Loading
            eventRepository.getEvent(eventId)
                .onSuccess { _uiState.value = EventDetailUiState.Success(it) }
                .onFailure { _uiState.value = EventDetailUiState.Error(it.message ?: "Hiba történt") }
        }
    }

    fun setParticipation(state: ParticipationState) {
        viewModelScope.launch {
            _participationLoading.value = true
            eventRepository.setParticipation(eventId, state)
                .onSuccess { loadEvent() }
            _participationLoading.value = false
        }
    }
}

sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState
    data class Success(val event: Event) : EventDetailUiState
    data class Error(val message: String) : EventDetailUiState
}
