package com.hobbeast.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

// ─── Event ────────────────────────────────────────────────────────────────────

@Serializable
data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    @SerialName("start_time") val startTime: String = "",
    @SerialName("end_time") val endTime: String? = null,
    val location: String = "",
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("organizer_id") val organizerId: String = "",
    @SerialName("organizer_name") val organizerName: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val category: String = "",
    val tags: List<String> = emptyList(),
    @SerialName("max_capacity") val maxCapacity: Int? = null,
    @SerialName("attendee_count") val attendeeCount: Int = 0,
    @SerialName("is_private") val isPrivate: Boolean = false,
    @SerialName("is_free") val isFree: Boolean = true,
    val price: Double? = null,
    @SerialName("ticket_tiers") val ticketTiers: List<TicketTier> = emptyList(),
    @SerialName("participation_state") val participationState: ParticipationState = ParticipationState.NONE,
    val source: EventSource = EventSource.HOBBEAST,
    @SerialName("external_id") val externalId: String? = null,
    @SerialName("external_url") val externalUrl: String? = null,
    @SerialName("venue_id") val venueId: String? = null,
    @SerialName("trip_plan_id") val tripPlanId: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("is_trending") val isTrending: Boolean = false,
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("is_early_access") val isEarlyAccess: Boolean = false,
    @SerialName("reminder_set") val reminderSet: Boolean = false,
    @SerialName("community_pulse_score") val communityPulseScore: Double? = null,
)

@Serializable
enum class EventSource {
    @SerialName("hobbeast") HOBBEAST,
    @SerialName("ticketmaster") TICKETMASTER,
    @SerialName("seatgeek") SEATGEEK,
    @SerialName("universe") UNIVERSE,
    @SerialName("ticket_tailor") TICKET_TAILOR,
}

@Serializable
enum class ParticipationState {
    @SerialName("none") NONE,
    @SerialName("interested") INTERESTED,
    @SerialName("going") GOING,
    @SerialName("waitlisted") WAITLISTED,
    @SerialName("checked_in") CHECKED_IN,
    @SerialName("declined") DECLINED,
}

@Serializable
data class TicketTier(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val currency: String = "HUF",
    val available: Int? = null,
    val sold: Int = 0,
)

// ─── User / Profile ───────────────────────────────────────────────────────────

@Serializable
data class UserProfile(
    val id: String = "",
    val email: String = "",
    @SerialName("display_name") val displayName: String = "",
    val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val interests: List<String> = emptyList(),
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("distance_km") val distanceKm: Int = 25,
    @SerialName("location_sharing") val locationSharing: Boolean = true,
    @SerialName("is_organizer") val isOrganizer: Boolean = false,
    @SerialName("organizer_verified") val organizerVerified: Boolean = false,
    @SerialName("profile_visibility") val profileVisibility: ProfileVisibility = ProfileVisibility.PUBLIC,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
enum class ProfileVisibility {
    @SerialName("public") PUBLIC,
    @SerialName("friends") FRIENDS,
    @SerialName("private") PRIVATE,
}

// ─── Venue ────────────────────────────────────────────────────────────────────

@Serializable
data class Venue(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @SerialName("image_url") val imageUrl: String? = null,
    val category: String = "",
    val tags: List<String> = emptyList(),
    val phone: String? = null,
    val website: String? = null,
    @SerialName("opening_hours") val openingHours: String? = null,
    val rating: Double? = null,
    @SerialName("is_partner") val isPartner: Boolean = false,
    @SerialName("partner_capabilities") val partnerCapabilities: List<String> = emptyList(),
    @SerialName("provider_id") val providerId: String? = null,
    val source: PlaceSource = PlaceSource.HOBBEAST,
)

@Serializable
enum class PlaceSource {
    @SerialName("hobbeast") HOBBEAST,
    @SerialName("geoapify") GEOAPIFY,
    @SerialName("tomtom") TOMTOM,
}

// ─── Attendee ─────────────────────────────────────────────────────────────────

@Serializable
data class Attendee(
    val id: String = "",
    @SerialName("event_id") val eventId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("user_name") val userName: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val state: ParticipationState = ParticipationState.GOING,
    @SerialName("ticket_tier_id") val ticketTierId: String? = null,
    @SerialName("invite_code") val inviteCode: String? = null,
    @SerialName("checked_in_at") val checkedInAt: String? = null,
    @SerialName("joined_at") val joinedAt: String = "",
)

// ─── Trip Plan ────────────────────────────────────────────────────────────────

@Serializable
data class TripPlan(
    val id: String = "",
    @SerialName("event_id") val eventId: String? = null,
    val title: String = "",
    val start: LocationRef = LocationRef(),
    val end: LocationRef = LocationRef(),
    val waypoints: List<LocationRef> = emptyList(),
    @SerialName("route_type") val routeType: RouteType = RouteType.CAR,
    val distance: Double? = null,
    val duration: Int? = null,
    val geometry: String? = null,
    @SerialName("elevation_profile") val elevationProfile: List<ElevationPoint> = emptyList(),
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class LocationRef(
    val label: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String? = null,
)

@Serializable
data class ElevationPoint(
    val distance: Double = 0.0,
    val elevation: Double = 0.0,
)

@Serializable
enum class RouteType {
    @SerialName("car") CAR,
    @SerialName("foot") FOOT,
    @SerialName("bike") BIKE,
    @SerialName("transit") TRANSIT,
}

// ─── Organizer Message ────────────────────────────────────────────────────────

@Serializable
data class OrganizerMessage(
    val id: String = "",
    @SerialName("event_id") val eventId: String = "",
    val type: MessageType = MessageType.REMINDER,
    val subject: String = "",
    val body: String = "",
    @SerialName("target_states") val targetStates: List<ParticipationState> = emptyList(),
    @SerialName("scheduled_at") val scheduledAt: String? = null,
    @SerialName("sent_at") val sentAt: String? = null,
    val status: MessageStatus = MessageStatus.DRAFT,
)

@Serializable
enum class MessageType {
    @SerialName("reminder") REMINDER,
    @SerialName("logistics_update") LOGISTICS_UPDATE,
    @SerialName("event_update") EVENT_UPDATE,
    @SerialName("cancellation") CANCELLATION,
    @SerialName("general") GENERAL,
}

@Serializable
enum class MessageStatus {
    @SerialName("draft") DRAFT,
    @SerialName("scheduled") SCHEDULED,
    @SerialName("sent") SENT,
    @SerialName("failed") FAILED,
}

// ─── Analytics ───────────────────────────────────────────────────────────────

@Serializable
data class EventAnalytics(
    @SerialName("event_id") val eventId: String = "",
    @SerialName("total_views") val totalViews: Int = 0,
    @SerialName("unique_viewers") val uniqueViewers: Int = 0,
    @SerialName("detail_opens") val detailOpens: Int = 0,
    @SerialName("going_count") val goingCount: Int = 0,
    @SerialName("interested_count") val interestedCount: Int = 0,
    @SerialName("waitlisted_count") val waitlistedCount: Int = 0,
    @SerialName("checked_in_count") val checkedInCount: Int = 0,
    @SerialName("conversion_rate") val conversionRate: Double = 0.0,
    val sources: List<SourceAttribution> = emptyList(),
)

@Serializable
data class SourceAttribution(
    val source: String = "",
    val count: Int = 0,
    val percentage: Double = 0.0,
)

// ─── Discovery Filter ─────────────────────────────────────────────────────────

data class DiscoveryFilter(
    val mode: FilterMode = FilterMode.ALL,
    val categories: Set<String> = emptySet(),
    val searchQuery: String = "",
    val distanceKm: Int? = null,
    val dateRange: DateRange? = null,
    val showFreeOnly: Boolean = false,
    val sources: Set<EventSource> = emptySet(),
)

enum class FilterMode { ALL, FOR_ME, SEARCH, CATEGORIES }

data class DateRange(val from: Instant, val to: Instant)

// ─── Hub Intelligence ────────────────────────────────────────────────────────

@Serializable
data class CommunityPulse(
    @SerialName("scene_label") val sceneLabel: String = "",
    @SerialName("activity_score") val activityScore: Double = 0.0,
    @SerialName("recurring_formats") val recurringFormats: List<String> = emptyList(),
    @SerialName("suggested_events") val suggestedEvents: List<String> = emptyList(),
    @SerialName("is_underserved") val isUnderserved: Boolean = false,
)
