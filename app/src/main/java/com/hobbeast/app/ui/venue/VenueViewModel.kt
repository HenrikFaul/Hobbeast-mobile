package com.hobbeast.app.ui.venue

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hobbeast.app.data.model.*
import com.hobbeast.app.data.remote.SupabaseDataSource
import com.hobbeast.app.data.repository.PlacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VenueViewModel @Inject constructor(
    private val supabase: SupabaseDataSource,
    private val placesRepository: PlacesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val venueId: String? = savedStateHandle["venueId"]

    private val _venue = MutableStateFlow<Venue?>(null)
    val venue: StateFlow<Venue?> = _venue.asStateFlow()

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _nearbyVenues = MutableStateFlow<List<Venue>>(emptyList())
    val nearbyVenues: StateFlow<List<Venue>> = _nearbyVenues.asStateFlow()

    init {
        venueId?.let { loadVenue(it) }
    }

    private fun loadVenue(id: String) {
        viewModelScope.launch {
            runCatching { supabase.getVenueById(id) }.onSuccess { _venue.value = it }
            runCatching { supabase.getEventsByVenue(id) }.onSuccess { _events.value = it }
        }
    }

    fun loadNearbyVenues(lat: Double, lon: Double) {
        viewModelScope.launch {
            placesRepository.searchVenues("", lat, lon, 5).onSuccess { _nearbyVenues.value = it }
        }
    }
}
