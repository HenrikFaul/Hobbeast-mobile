package com.hobbeast.app.ui.retention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hobbeast.app.data.model.*
import com.hobbeast.app.data.remote.SupabaseDataSource
import com.hobbeast.app.data.repository.EventRepository
import com.hobbeast.app.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPlansViewModel @Inject constructor(
    private val supabase: SupabaseDataSource,
    private val eventRepository: EventRepository,
    private val reminderRepository: ReminderRepository,
) : ViewModel() {

    private val _goingEvents  = MutableStateFlow<List<Event>>(emptyList())
    val goingEvents: StateFlow<List<Event>> = _goingEvents.asStateFlow()

    private val _savedEvents  = MutableStateFlow<List<Event>>(emptyList())
    val savedEvents: StateFlow<List<Event>> = _savedEvents.asStateFlow()

    private val _reminderMap  = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val reminderMap: StateFlow<Map<String, Boolean>> = _reminderMap.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            // Load all events, filter by participation state
            eventRepository.getDiscoveryEvents(DiscoveryFilter()).collect { result ->
                result.onSuccess { events ->
                    _goingEvents.value  = events.filter { it.participationState == ParticipationState.GOING }
                    _savedEvents.value  = events.filter { it.participationState == ParticipationState.INTERESTED }
                }
            }
        }
        viewModelScope.launch {
            reminderRepository.getActiveReminders().collect { reminders ->
                _reminderMap.value = reminders.associate { it.eventId to true }
            }
        }
    }

    fun toggleReminder(event: Event) {
        viewModelScope.launch {
            if (_reminderMap.value[event.id] == true) {
                reminderRepository.cancelReminder(event.id)
            } else {
                reminderRepository.scheduleReminder(
                    eventId = event.id,
                    eventTitle = event.title,
                    eventStartTimeIso = event.startTime,
                    hoursBeforeEvent = 24,
                )
            }
        }
    }
}
