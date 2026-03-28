package com.hobbeast.app.data.repository

import com.hobbeast.app.BuildConfig
import com.hobbeast.app.data.model.*
import com.hobbeast.app.data.remote.GeoapifyApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlacesRepository @Inject constructor(
    private val geoapifyApi: GeoapifyApi,
) {
    // ─── Autocomplete ─────────────────────────────────────────────────────────

    suspend fun autocomplete(query: String, lat: Double? = null, lon: Double? = null): Result<List<LocationSuggestion>> = runCatching {
        val bias = if (lat != null && lon != null) "proximity:$lon,$lat" else null
        val response = geoapifyApi.autocomplete(
            apiKey = BuildConfig.GEOAPIFY_API_KEY,
            text = query,
            bias = bias,
        )
        response.features?.mapNotNull { feature ->
            val props = feature.properties ?: return@mapNotNull null
            val coords = feature.geometry?.coordinates
            LocationSuggestion(
                label = props.formatted ?: props.name ?: return@mapNotNull null,
                latitude = coords?.getOrNull(1) ?: return@mapNotNull null,
                longitude = coords?.getOrNull(0) ?: return@mapNotNull null,
                address = props.formatted,
                placeId = props.placeId,
                city = props.city,
            )
        } ?: emptyList()
    }

    // ─── Reverse Geocode ──────────────────────────────────────────────────────

    suspend fun reverseGeocode(lat: Double, lon: Double): Result<LocationSuggestion> = runCatching {
        val response = geoapifyApi.reverseGeocode(
            apiKey = BuildConfig.GEOAPIFY_API_KEY,
            lat = lat,
            lon = lon,
        )
        val feature = response.features?.firstOrNull()
            ?: throw Exception("No result for reverse geocoding")
        val props = feature.properties ?: throw Exception("No properties")
        LocationSuggestion(
            label = props.formatted ?: "$lat, $lon",
            latitude = lat,
            longitude = lon,
            address = props.formatted,
            placeId = props.placeId,
            city = props.city,
        )
    }

    // ─── Venue search ─────────────────────────────────────────────────────────

    suspend fun searchVenues(
        query: String,
        lat: Double,
        lon: Double,
        radiusKm: Int = 10,
    ): Result<List<Venue>> = runCatching {
        val response = geoapifyApi.searchPlaces(
            apiKey = BuildConfig.GEOAPIFY_API_KEY,
            categories = "entertainment,sport,leisure,tourism",
            filter = "circle:$lon,$lat,${radiusKm * 1000}",
            bias = "proximity:$lon,$lat",
        )
        response.features?.mapNotNull { feature ->
            val props = feature.properties ?: return@mapNotNull null
            val coords = feature.geometry?.coordinates
            Venue(
                id = props.placeId ?: return@mapNotNull null,
                name = props.name ?: return@mapNotNull null,
                address = props.formatted ?: "",
                latitude = coords?.getOrNull(1) ?: return@mapNotNull null,
                longitude = coords?.getOrNull(0) ?: return@mapNotNull null,
                category = props.categories?.firstOrNull() ?: "",
                website = props.website,
                phone = props.phone,
                openingHours = props.opening_hours,
                rating = props.rate,
                source = PlaceSource.GEOAPIFY,
                providerId = props.placeId,
            )
        } ?: emptyList()
    }
}

data class LocationSuggestion(
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val placeId: String? = null,
    val city: String? = null,
)
