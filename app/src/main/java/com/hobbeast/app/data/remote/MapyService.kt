package com.hobbeast.app.data.remote

import com.google.gson.annotations.SerializedName
import com.hobbeast.app.data.model.*
import retrofit2.http.*

// ─── Mapy.com API ─────────────────────────────────────────────────────────────

interface MapyApi {

    @GET("suggest")
    suspend fun suggest(
        @Query("apikey") apiKey: String,
        @Query("query") query: String,
        @Query("lang") lang: String = "hu",
        @Query("limit") limit: Int = 8,
        @Query("proximity") proximity: String? = null,
    ): MapySuggestResponse

    @GET("geocode")
    suspend fun geocode(
        @Query("apikey") apiKey: String,
        @Query("query") query: String,
        @Query("lang") lang: String = "hu",
        @Query("limit") limit: Int = 1,
    ): MapyGeocodeResponse

    @GET("rgeocode")
    suspend fun reverseGeocode(
        @Query("apikey") apiKey: String,
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("lang") lang: String = "hu",
    ): MapyGeocodeResponse

    @POST("route")
    suspend fun route(
        @Query("apikey") apiKey: String,
        @Body request: MapyRouteRequest,
    ): MapyRouteResponse
}

data class MapySuggestResponse(
    val items: List<MapySuggestItem>?,
)

data class MapySuggestItem(
    val name: String,
    val label: String?,
    val location: MapyLocation?,
    val type: String?,
)

data class MapyGeocodeResponse(
    val items: List<MapyGeocodeItem>?,
)

data class MapyGeocodeItem(
    val name: String?,
    val label: String?,
    val location: MapyLocation?,
    val regionalStructure: List<MapyRegionalItem>?,
)

data class MapyLocation(
    val lat: Double,
    val lon: Double,
)

data class MapyRegionalItem(
    val name: String,
    val type: String,
)

data class MapyRouteRequest(
    val origin: MapyRoutePoint,
    val destination: MapyRoutePoint,
    val waypoints: List<MapyRoutePoint> = emptyList(),
    val routeType: String = "car_fast",  // car_fast, car_short, foot, bike, mtb, hike
    val lang: String = "hu",
)

data class MapyRoutePoint(
    val coords: String,  // "lon,lat"
)

data class MapyRouteResponse(
    val route: List<MapyRouteSegment>?,
    val summary: MapyRouteSummary?,
    val geometry: String?,  // Encoded polyline
)

data class MapyRouteSegment(
    val distance: Double,
    val duration: Int,
    val name: String?,
)

data class MapyRouteSummary(
    val distance: Double,
    val duration: Int,
    val elevationGain: Double?,
    val elevationLoss: Double?,
)

// ─── Mapy trip planning service (HOB-98) ─────────────────────────────────────

class MapyTripPlanningService(
    private val mapy: MapyApi,
    private val apiKey: String,
) {
    suspend fun planRoute(
        start: LocationRef,
        end: LocationRef,
        waypoints: List<LocationRef> = emptyList(),
        routeType: RouteType = RouteType.CAR,
    ): Result<TripPlanningResult> = runCatching {
        val request = MapyRouteRequest(
            origin = MapyRoutePoint("${start.longitude},${start.latitude}"),
            destination = MapyRoutePoint("${end.longitude},${end.latitude}"),
            waypoints = waypoints.map { MapyRoutePoint("${it.longitude},${it.latitude}") },
            routeType = routeType.toMapyType(),
        )
        val response = mapy.route(apiKey, request)
        val summary = response.summary ?: throw Exception("No route summary")
        TripPlanningResult(
            distanceKm = summary.distance / 1000.0,
            durationMin = summary.duration / 60,
            geometry = response.geometry,
            elevationGain = summary.elevationGain,
            elevationLoss = summary.elevationLoss,
        )
    }

    suspend fun suggest(
        query: String,
        lat: Double? = null,
        lon: Double? = null,
    ): Result<List<LocationRef>> = runCatching {
        val proximity = if (lat != null && lon != null) "$lon,$lat" else null
        val response = mapy.suggest(apiKey, query, proximity = proximity)
        response.items?.mapNotNull { item ->
            val loc = item.location ?: return@mapNotNull null
            LocationRef(
                label = item.label ?: item.name,
                latitude = loc.lat,
                longitude = loc.lon,
            )
        } ?: emptyList()
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): Result<LocationRef> = runCatching {
        val response = mapy.reverseGeocode(apiKey, lat, lon)
        val item = response.items?.firstOrNull() ?: throw Exception("No result")
        LocationRef(
            label = item.label ?: item.name ?: "$lat, $lon",
            latitude = lat,
            longitude = lon,
            address = item.label,
        )
    }
}

data class TripPlanningResult(
    val distanceKm: Double,
    val durationMin: Int,
    val geometry: String?,
    val elevationGain: Double?,
    val elevationLoss: Double?,
)

fun RouteType.toMapyType() = when (this) {
    RouteType.CAR     -> "car_fast"
    RouteType.FOOT    -> "foot"
    RouteType.BIKE    -> "bike"
    RouteType.TRANSIT -> "car_fast"  // Mapy doesn't have transit; fallback to car
}
