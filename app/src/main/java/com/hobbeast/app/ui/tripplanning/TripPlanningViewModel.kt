package com.hobbeast.app.ui.tripplanning

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hobbeast.app.data.model.*
import com.hobbeast.app.data.remote.MapyTripPlanningService
import com.hobbeast.app.data.remote.TripPlanningResult
import com.hobbeast.app.data.repository.LocationSuggestion
import com.hobbeast.app.data.repository.PlacesRepository
import com.hobbeast.app.data.remote.SupabaseDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripPlanningViewModel @Inject constructor(
    private val mapy: MapyTripPlanningService,
    private val placesRepository: PlacesRepository,
    private val supabase: SupabaseDataSource,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val eventId: String = checkNotNull(savedStateHandle["eventId"])

    private val _planState = MutableStateFlow(TripPlanState())
    val planState: StateFlow<TripPlanState> = _planState.asStateFlow()

    private val _suggestions = MutableStateFlow<List<LocationSuggestion>>(emptyList())
    val suggestions: StateFlow<List<LocationSuggestion>> = _suggestions.asStateFlow()

    private val _activeField = MutableStateFlow<TripField?>(null)
    val activeField: StateFlow<TripField?> = _activeField.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    fun setActiveField(field: TripField?) { _activeField.value = field }

    fun searchLocation(query: String) {
        viewModelScope.launch {
            if (query.length < 2) { _suggestions.value = emptyList(); return@launch }

            // Try Mapy first for trip planning context, fallback to Geoapify
            mapy.suggest(query)
                .onSuccess { refs ->
                    _suggestions.value = refs.map { ref ->
                        LocationSuggestion(
                            label = ref.label,
                            latitude = ref.latitude,
                            longitude = ref.longitude,
                            address = ref.address,
                        )
                    }
                }
                .onFailure {
                    placesRepository.autocomplete(query)
                        .onSuccess { _suggestions.value = it }
                }
        }
    }

    fun selectSuggestion(suggestion: LocationSuggestion) {
        val ref = LocationRef(
            label = suggestion.label,
            latitude = suggestion.latitude,
            longitude = suggestion.longitude,
            address = suggestion.address,
        )
        when (_activeField.value) {
            TripField.START    -> _planState.update { it.copy(start = ref) }
            TripField.END      -> _planState.update { it.copy(end = ref) }
            TripField.WAYPOINT -> _planState.update { it.copy(waypoints = it.waypoints + ref) }
            null -> {}
        }
        _suggestions.value = emptyList()
        _activeField.value = null
    }

    fun removeWaypoint(index: Int) {
        _planState.update { s ->
            s.copy(waypoints = s.waypoints.toMutableList().also { it.removeAt(index) })
        }
    }

    fun setRouteType(type: RouteType) {
        _planState.update { it.copy(routeType = type) }
    }

    fun planRoute() {
        val state = _planState.value
        val start = state.start ?: return
        val end   = state.end   ?: return

        viewModelScope.launch {
            _planState.update { it.copy(isCalculating = true, error = null, routeReady = false) }

            mapy.planRoute(
                start = start,
                end = end,
                waypoints = state.waypoints,
                routeType = state.routeType,
            ).onSuccess { result ->
                _planState.update {
                    it.copy(
                        isCalculating = false,
                        routeReady = true,
                        calculatedDistance = result.distanceKm,
                        calculatedDurationMin = result.durationMin,
                        routeGeometry = result.geometry,
                        elevationGain = result.elevationGain,
                        elevationLoss = result.elevationLoss,
                    )
                }
            }.onFailure { e ->
                _planState.update { it.copy(isCalculating = false, error = e.message) }
            }
        }
    }

    fun savePlan() {
        val state = _planState.value
        if (!state.routeReady) return
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            val plan = TripPlan(
                eventId = eventId,
                title = "Útvonal – ${state.start?.label} → ${state.end?.label}",
                start = state.start!!,
                end = state.end!!,
                waypoints = state.waypoints,
                routeType = state.routeType,
                distance = state.calculatedDistance,
                duration = state.calculatedDurationMin,
                geometry = state.routeGeometry,
            )
            runCatching { supabase.saveTripPlan(plan) }
                .onSuccess { _saveState.value = SaveState.Saved }
                .onFailure { _saveState.value = SaveState.Error(it.message ?: "Mentés sikertelen") }
        }
    }
}

data class TripPlanState(
    val start: LocationRef? = null,
    val end: LocationRef? = null,
    val waypoints: List<LocationRef> = emptyList(),
    val routeType: RouteType = RouteType.CAR,
    val isCalculating: Boolean = false,
    val routeReady: Boolean = false,
    val calculatedDistance: Double? = null,
    val calculatedDurationMin: Int? = null,
    val routeGeometry: String? = null,
    val elevationGain: Double? = null,
    val elevationLoss: Double? = null,
    val error: String? = null,
)

enum class TripField { START, END, WAYPOINT }

sealed interface SaveState {
    data object Idle   : SaveState
    data object Saving : SaveState
    data object Saved  : SaveState
    data class Error(val message: String) : SaveState
}
