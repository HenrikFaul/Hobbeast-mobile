package com.hobbeast.app.ui.createevent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hobbeast.app.data.model.*
import com.hobbeast.app.data.repository.EventRepository
import com.hobbeast.app.data.repository.PlacesRepository
import com.hobbeast.app.data.repository.LocationSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val placesRepository: PlacesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val editEventId: String? = savedStateHandle["eventId"]

    private val _form = MutableStateFlow(EventForm())
    val form: StateFlow<EventForm> = _form.asStateFlow()

    private val _uiState = MutableStateFlow<CreateEventUiState>(CreateEventUiState.Idle)
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

    private val _locationSuggestions = MutableStateFlow<List<LocationSuggestion>>(emptyList())
    val locationSuggestions: StateFlow<List<LocationSuggestion>> = _locationSuggestions.asStateFlow()

    init {
        editEventId?.let { loadEventForEdit(it) }
    }

    private fun loadEventForEdit(id: String) {
        viewModelScope.launch {
            eventRepository.getEvent(id).onSuccess { event ->
                _form.value = EventForm(
                    title = event.title,
                    description = event.description,
                    category = event.category,
                    location = event.location,
                    address = event.address ?: "",
                    startTime = event.startTime,
                    endTime = event.endTime ?: "",
                    maxCapacity = event.maxCapacity?.toString() ?: "",
                    isPrivate = event.isPrivate,
                    isFree = event.isFree,
                    price = event.price?.toString() ?: "",
                    tags = event.tags.joinToString(", "),
                )
            }
        }
    }

    fun updateTitle(v: String)       = _form.update { it.copy(title = v) }
    fun updateDescription(v: String) = _form.update { it.copy(description = v) }
    fun updateCategory(v: String)    = _form.update { it.copy(category = v) }
    fun updateLocation(v: String)    = _form.update { it.copy(location = v) }
    fun updateAddress(v: String)     = _form.update { it.copy(address = v) }
    fun updateStartTime(v: String)   = _form.update { it.copy(startTime = v) }
    fun updateEndTime(v: String)     = _form.update { it.copy(endTime = v) }
    fun updateCapacity(v: String)    = _form.update { it.copy(maxCapacity = v) }
    fun updateIsPrivate(v: Boolean)  = _form.update { it.copy(isPrivate = v) }
    fun updateIsFree(v: Boolean)     = _form.update { it.copy(isFree = v, price = if (v) "" else it.price) }
    fun updatePrice(v: String)       = _form.update { it.copy(price = v) }
    fun updateTags(v: String)        = _form.update { it.copy(tags = v) }

    fun searchLocations(query: String) {
        viewModelScope.launch {
            if (query.length >= 2) {
                placesRepository.autocomplete(query)
                    .onSuccess { _locationSuggestions.value = it }
            } else {
                _locationSuggestions.value = emptyList()
            }
        }
    }

    fun selectLocation(suggestion: LocationSuggestion) {
        _form.update { it.copy(location = suggestion.label, address = suggestion.address ?: suggestion.label,
            latitude = suggestion.latitude, longitude = suggestion.longitude) }
        _locationSuggestions.value = emptyList()
    }

    fun saveEvent() {
        val f = _form.value
        val errors = validate(f)
        if (errors.isNotEmpty()) {
            _uiState.value = CreateEventUiState.ValidationError(errors)
            return
        }
        viewModelScope.launch {
            _uiState.value = CreateEventUiState.Loading
            val event = Event(
                id = editEventId ?: UUID.randomUUID().toString(),
                title = f.title,
                description = f.description,
                category = f.category,
                location = f.location,
                address = f.address,
                latitude = f.latitude,
                longitude = f.longitude,
                startTime = f.startTime,
                endTime = f.endTime.ifBlank { null },
                maxCapacity = f.maxCapacity.toIntOrNull(),
                isPrivate = f.isPrivate,
                isFree = f.isFree,
                price = f.price.toDoubleOrNull(),
                tags = f.tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
            )
            val result = if (editEventId != null) eventRepository.updateEvent(event) else eventRepository.createEvent(event)
            result
                .onSuccess { saved -> _uiState.value = CreateEventUiState.Success(saved.id) }
                .onFailure { _uiState.value = CreateEventUiState.Error(it.message ?: "Mentés sikertelen") }
        }
    }

    private fun validate(f: EventForm): Map<String, String> = buildMap {
        if (f.title.isBlank()) put("title", "A cím kötelező")
        if (f.category.isBlank()) put("category", "Válassz kategóriát")
        if (f.location.isBlank()) put("location", "A helyszín kötelező")
        if (f.startTime.isBlank()) put("startTime", "A kezdési időpont kötelező")
        if (!f.isFree && f.price.isBlank()) put("price", "Add meg az árat")
    }
}

data class EventForm(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val location: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val startTime: String = "",
    val endTime: String = "",
    val maxCapacity: String = "",
    val isPrivate: Boolean = false,
    val isFree: Boolean = true,
    val price: String = "",
    val tags: String = "",
)

sealed interface CreateEventUiState {
    data object Idle    : CreateEventUiState
    data object Loading : CreateEventUiState
    data class  Success(val eventId: String) : CreateEventUiState
    data class  Error(val message: String)   : CreateEventUiState
    data class  ValidationError(val errors: Map<String, String>) : CreateEventUiState
}
