package com.hobbeast.app.ui.organizer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hobbeast.app.data.model.*
import com.hobbeast.app.data.remote.SupabaseDataSource
import com.hobbeast.app.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── Organizer Dashboard VM ───────────────────────────────────────────────────

@HiltViewModel
class OrganizerViewModel @Inject constructor(
    private val supabase: SupabaseDataSource,
    private val eventRepository: EventRepository,
) : ViewModel() {

    private val _myEvents = MutableStateFlow<List<Event>>(emptyList())
    val myEvents: StateFlow<List<Event>> = _myEvents.asStateFlow()

    init { loadMyEvents() }

    private fun loadMyEvents() {
        viewModelScope.launch {
            eventRepository.getDiscoveryEvents(DiscoveryFilter()).collect { result ->
                result.onSuccess { events ->
                    val uid = supabase.currentUserId()
                    _myEvents.value = events.filter { it.organizerId == uid }
                }
            }
        }
    }
}

// ─── Attendee VM ──────────────────────────────────────────────────────────────

@HiltViewModel
class AttendeeViewModel @Inject constructor(
    private val supabase: SupabaseDataSource,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"])

    private val _attendees = MutableStateFlow<List<Attendee>>(emptyList())
    val attendees: StateFlow<List<Attendee>> = _attendees.asStateFlow()

    private val _filterState = MutableStateFlow<ParticipationState?>(null)
    val filterState: StateFlow<ParticipationState?> = _filterState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            runCatching { supabase.getAttendees(eventId) }
                .onSuccess { _attendees.value = it }
        }
    }

    fun setFilter(state: ParticipationState?) { _filterState.value = state }

    fun removeAttendee(attendeeId: String) {
        _attendees.update { it.filter { a -> a.id != attendeeId } }
        // TODO: call Supabase delete
    }

    fun exportAttendees() {
        // TODO: generate CSV and share via Android share sheet
    }
}

// ─── Messaging VM ─────────────────────────────────────────────────────────────

@HiltViewModel
class MessagingViewModel @Inject constructor(
    private val supabase: SupabaseDataSource,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"])

    private val _messages = MutableStateFlow<List<OrganizerMessage>>(emptyList())
    val messages: StateFlow<List<OrganizerMessage>> = _messages.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            runCatching { supabase.getMessages(eventId) }
                .onSuccess { _messages.value = it }
        }
    }

    fun sendMessage(subject: String, body: String, type: MessageType) {
        viewModelScope.launch {
            val msg = OrganizerMessage(
                eventId = eventId, type = type, subject = subject, body = body,
                status = MessageStatus.SENT,
            )
            runCatching { supabase.sendMessage(msg) }
                .onSuccess { sent -> _messages.update { listOf(sent) + it } }
        }
    }
}

// ─── Analytics VM ─────────────────────────────────────────────────────────────

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val supabase: SupabaseDataSource,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"])

    private val _analytics = MutableStateFlow<EventAnalytics?>(null)
    val analytics: StateFlow<EventAnalytics?> = _analytics.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            runCatching { supabase.getEventAnalytics(eventId) }
                .onSuccess { _analytics.value = it }
        }
    }
}
