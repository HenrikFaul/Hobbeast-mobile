package com.hobbeast.app.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

// ─── Ticketmaster ─────────────────────────────────────────────────────────────

interface TicketmasterApi {

    @GET("discovery/v2/events.json")
    suspend fun searchEvents(
        @Query("apikey") apiKey: String,
        @Query("keyword") keyword: String? = null,
        @Query("latlong") latlong: String? = null,
        @Query("radius") radius: Int? = null,
        @Query("unit") unit: String = "km",
        @Query("classificationName") classificationName: String? = null,
        @Query("size") size: Int = 20,
        @Query("page") page: Int = 0,
        @Query("locale") locale: String = "hu,en-us,*",
    ): TicketmasterResponse
}

data class TicketmasterResponse(
    @SerializedName("_embedded") val embedded: TmEmbedded?,
    val page: TmPage?,
)

data class TmEmbedded(
    val events: List<TmEvent>?,
)

data class TmEvent(
    val id: String,
    val name: String,
    val description: String?,
    val info: String?,
    @SerializedName("images") val images: List<TmImage>?,
    val dates: TmDates?,
    @SerializedName("_embedded") val embedded: TmEventEmbedded?,
    val url: String?,
    val priceRanges: List<TmPriceRange>?,
)

data class TmImage(val url: String, val ratio: String?, val width: Int?, val height: Int?)
data class TmDates(val start: TmStart?)
data class TmStart(@SerializedName("dateTime") val dateTime: String?, @SerializedName("localDate") val localDate: String?)
data class TmEventEmbedded(val venues: List<TmVenue>?)
data class TmVenue(val name: String?, val city: TmCity?, val state: TmState?, val address: TmAddress?, val location: TmLocation?)
data class TmCity(val name: String?)
data class TmState(val name: String?)
data class TmAddress(val line1: String?)
data class TmLocation(val longitude: String?, val latitude: String?)
data class TmPriceRange(val min: Double?, val max: Double?, val currency: String?)
data class TmPage(val totalElements: Int?, val totalPages: Int?)

// ─── SeatGeek ─────────────────────────────────────────────────────────────────

interface SeatGeekApi {

    @GET("events")
    suspend fun searchEvents(
        @Query("client_id") clientId: String,
        @Query("q") query: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("range") range: String? = null,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1,
    ): SeatGeekResponse
}

data class SeatGeekResponse(
    val events: List<SgEvent>?,
    val meta: SgMeta?,
)

data class SgEvent(
    val id: Int,
    val title: String,
    val description: String?,
    @SerializedName("datetime_local") val datetimeLocal: String?,
    val venue: SgVenue?,
    @SerializedName("short_title") val shortTitle: String?,
    val url: String?,
    val performers: List<SgPerformer>?,
    val stats: SgStats?,
)

data class SgVenue(
    val name: String?,
    val city: String?,
    val state: String?,
    val address: String?,
    val location: SgLocation?,
)

data class SgLocation(val lat: Double?, val lon: Double?)
data class SgPerformer(val name: String?, @SerializedName("image") val image: String?)
data class SgStats(@SerializedName("lowest_price") val lowestPrice: Double?)
data class SgMeta(val total: Int?, val page: Int?, @SerializedName("per_page") val perPage: Int?)

// ─── Geoapify Places ──────────────────────────────────────────────────────────

interface GeoapifyApi {

    @GET("v2/places")
    suspend fun searchPlaces(
        @Query("apiKey") apiKey: String,
        @Query("categories") categories: String,
        @Query("filter") filter: String,
        @Query("bias") bias: String? = null,
        @Query("limit") limit: Int = 20,
    ): GeoapifyPlacesResponse

    @GET("v1/geocode/autocomplete")
    suspend fun autocomplete(
        @Query("apiKey") apiKey: String,
        @Query("text") text: String,
        @Query("bias") bias: String? = null,
        @Query("lang") lang: String = "hu",
        @Query("limit") limit: Int = 8,
    ): GeoapifyAutocompleteResponse

    @GET("v1/geocode/reverse")
    suspend fun reverseGeocode(
        @Query("apiKey") apiKey: String,
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("lang") lang: String = "hu",
    ): GeoapifyReverseResponse
}

data class GeoapifyPlacesResponse(val features: List<GeoapifyFeature>?)
data class GeoapifyFeature(
    val properties: GeoapifyProperties?,
    val geometry: GeoapifyGeometry?,
)
data class GeoapifyProperties(
    @SerializedName("place_id") val placeId: String?,
    val name: String?,
    val categories: List<String>?,
    val address_line1: String?,
    val address_line2: String?,
    val city: String?,
    val country: String?,
    val formatted: String?,
    val website: String?,
    val phone: String?,
    val opening_hours: String?,
    val rate: Double?,
)
data class GeoapifyGeometry(val coordinates: List<Double>?)
data class GeoapifyAutocompleteResponse(val features: List<GeoapifyFeature>?)
data class GeoapifyReverseResponse(val features: List<GeoapifyFeature>?)
