package com.hobbeast.app.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hobbeast.app.data.model.*
import com.hobbeast.app.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val eventRepository: EventRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(DiscoveryFilter())
    val filter: StateFlow<DiscoveryFilter> = _filter.asStateFlow()

    private val _uiState = MutableStateFlow<DiscoveryUiState>(DiscoveryUiState.Loading)
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    private val _trendingEvents = MutableStateFlow<List<Event>>(emptyList())
    val trendingEvents: StateFlow<List<Event>> = _trendingEvents.asStateFlow()

    private val _featuredEvents = MutableStateFlow<List<Event>>(emptyList())
    val featuredEvents: StateFlow<List<Event>> = _featuredEvents.asStateFlow()

    init {
        observeFilter()
        loadTrendingAndFeatured()
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun observeFilter() {
        _filter
            .debounce(300)
            .flatMapLatest { filter -> eventRepository.getDiscoveryEvents(filter) }
            .onEach { result ->
                result.fold(
                    onSuccess = { events -> _uiState.value = DiscoveryUiState.Success(events) },
                    onFailure = { e -> _uiState.value = DiscoveryUiState.Error(e.message ?: "Ismeretlen hiba") },
                )
            }
            .launchIn(viewModelScope)
    }

    private fun loadTrendingAndFeatured() {
        viewModelScope.launch {
            eventRepository.getTrendingEvents().collect { result ->
                result.onSuccess { _trendingEvents.value = it }
            }
        }
        viewModelScope.launch {
            eventRepository.getFeaturedEvents().collect { result ->
                result.onSuccess { _featuredEvents.value = it }
            }
        }
    }

    fun setFilterMode(mode: FilterMode) {
        _filter.update { it.copy(mode = mode, searchQuery = if (mode != FilterMode.SEARCH) "" else it.searchQuery) }
    }

    fun setSearchQuery(query: String) {
        _filter.update { it.copy(mode = FilterMode.SEARCH, searchQuery = query) }
    }

    fun toggleCategory(category: String) {
        _filter.update { current ->
            val updated = if (category in current.categories)
                current.categories - category
            else
                current.categories + category
            current.copy(categories = updated, mode = if (updated.isEmpty()) FilterMode.ALL else FilterMode.CATEGORIES)
        }
    }

    fun setDistanceKm(km: Int?) {
        _filter.update { it.copy(distanceKm = km) }
    }

    fun toggleFreeOnly() {
        _filter.update { it.copy(showFreeOnly = !it.showFreeOnly) }
    }

    fun refresh() {
        _filter.update { it.copy() }
    }
}

sealed interface DiscoveryUiState {
    data object Loading : DiscoveryUiState
    data class Success(val events: List<Event>) : DiscoveryUiState
    data class Error(val message: String) : DiscoveryUiState
}
