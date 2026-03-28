package com.hobbeast.app.ui.organizer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hobbeast.app.data.model.*
import com.hobbeast.app.data.remote.SupabaseDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val supabase: SupabaseDataSource,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"])

    private val _uiState = MutableStateFlow<CheckInUiState>(CheckInUiState.Idle)
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()

    // Track names of recently checked-in attendees this session
    private val _recentCheckIns = MutableStateFlow<List<String>>(emptyList())
    val recentCheckIns: StateFlow<List<String>> = _recentCheckIns.asStateFlow()

    fun checkIn(inviteCodeOrQr: String) {
        if (inviteCodeOrQr.isBlank()) return
        viewModelScope.launch {
            _uiState.value = CheckInUiState.Loading
            runCatching { supabase.checkInAttendee(eventId, inviteCodeOrQr) }
                .onSuccess { attendee ->
                    _uiState.value = CheckInUiState.Success(attendee.userName)
                    _recentCheckIns.update { listOf(attendee.userName) + it }
                    // Auto-clear success after 3 seconds
                    kotlinx.coroutines.delay(3_000)
                    if (_uiState.value is CheckInUiState.Success) {
                        _uiState.value = CheckInUiState.Idle
                    }
                }
                .onFailure { e ->
                    _uiState.value = CheckInUiState.Error(
                        when {
                            e.message?.contains("not found", ignoreCase = true) == true ->
                                "Érvénytelen kód – nem található résztvevő"
                            e.message?.contains("already", ignoreCase = true) == true ->
                                "Ez a résztvevő már be van jelentkezve"
                            else -> "Hiba: ${e.message}"
                        }
                    )
                    kotlinx.coroutines.delay(4_000)
                    if (_uiState.value is CheckInUiState.Error) {
                        _uiState.value = CheckInUiState.Idle
                    }
                }
        }
    }
}

sealed interface CheckInUiState {
    data object Idle    : CheckInUiState
    data object Loading : CheckInUiState
    data class  Success(val attendeeName: String) : CheckInUiState
    data class  Error(val message: String)        : CheckInUiState
}
