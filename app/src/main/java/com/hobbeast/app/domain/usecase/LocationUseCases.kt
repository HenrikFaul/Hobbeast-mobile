package com.hobbeast.app.domain.usecase

import com.hobbeast.app.data.local.UserPreferencesStore
import com.hobbeast.app.data.model.*
import com.hobbeast.app.data.repository.LocationSuggestion
import com.hobbeast.app.data.repository.PlacesRepository
import com.hobbeast.app.service.LocationService
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * HOB-34 – Shared address autocomplete and location normalization.
 * Single entry-point for all location input fields across the app.
 */
class AddressAutocompleteUseCase @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val prefs: UserPreferencesStore,
) {
    suspend operator fun invoke(query: String): Result<List<LocationSuggestion>> {
        if (query.length < 2) return Result.success(emptyList())

        val (lat, lon) = prefs.lastLocation.first() ?: Pair(null, null)
        return placesRepository.autocomplete(query, lat, lon)
    }

    /** Normalize a raw suggestion to a canonical address form for storage. */
    fun normalize(suggestion: LocationSuggestion): NormalizedLocation = NormalizedLocation(
        displayLabel = suggestion.label,
        cityName = suggestion.city ?: extractCity(suggestion.label),
        fullAddress = suggestion.address ?: suggestion.label,
        latitude = suggestion.latitude,
        longitude = suggestion.longitude,
        placeId = suggestion.placeId,
    )

    private fun extractCity(label: String): String? {
        // "Budapest, Deák Ferenc tér 1" → "Budapest"
        return label.split(",").firstOrNull()?.trim()
    }
}

data class NormalizedLocation(
    val displayLabel: String,
    val cityName: String?,
    val fullAddress: String,
    val latitude: Double,
    val longitude: Double,
    val placeId: String?,
)

/**
 * HOB-35 – Profile-based distance filtering.
 * Filters a list of events by the user's configured max distance.
 */
class DistanceFilterUseCase @Inject constructor(
    private val locationService: LocationService,
    private val prefs: UserPreferencesStore,
) {
    suspend operator fun invoke(
        events: List<Event>,
        profile: UserProfile?,
    ): List<Event> {
        val maxDistKm = profile?.distanceKm ?: return events
        val (lat, lon) = prefs.lastLocation.first() ?: return events

        return events.filter { event ->
            if (event.latitude == null || event.longitude == null) return@filter true
            val dist = locationService.distanceKm(lat, lon, event.latitude, event.longitude)
            dist <= maxDistKm
        }
    }
}

/**
 * HOB-29 – Location privacy behaviour.
 * Determines whether location should be shared / used based on profile settings.
 */
class LocationPrivacyUseCase @Inject constructor(
    private val prefs: UserPreferencesStore,
) {
    suspend fun shouldShareLocation(profile: UserProfile?): Boolean {
        if (profile?.locationSharing == false) return false
        return true
    }

    suspend fun getEffectiveLocation(profile: UserProfile?): Pair<Double, Double>? {
        if (!shouldShareLocation(profile)) return null
        if (profile?.latitude != null && profile.longitude != null) {
            return Pair(profile.latitude, profile.longitude)
        }
        return prefs.lastLocation.first()
    }
}
