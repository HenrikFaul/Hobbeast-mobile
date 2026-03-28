package com.hobbeast.app.data.remote

import com.hobbeast.app.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {

    // ─── Auth ─────────────────────────────────────────────────────────────────

    suspend fun signIn(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signUp(email: String, password: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() = supabase.auth.signOut()
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id
    fun isAuthenticated(): Boolean = supabase.auth.currentUserOrNull() != null

    // ─── Events ───────────────────────────────────────────────────────────────

    suspend fun getEvents(filter: DiscoveryFilter): List<Event> =
        supabase.from("events").select {
            if (filter.searchQuery.isNotBlank())
                filter { ilike("title", "%${filter.searchQuery}%") }
            order("start_time", Order.ASCENDING)
            limit(50)
        }.decodeList()

    suspend fun getEventById(id: String): Event =
        supabase.from("events").select { filter { eq("id", id) } }.decodeSingle()

    suspend fun createEvent(event: Event): Event =
        supabase.from("events").insert(event) { select() }.decodeSingle()

    suspend fun updateEvent(event: Event): Event =
        supabase.from("events").update(event) {
            filter { eq("id", event.id) }; select()
        }.decodeSingle()

    suspend fun deleteEvent(eventId: String) =
        supabase.from("events").delete { filter { eq("id", eventId) } }

    // ─── Participation ────────────────────────────────────────────────────────

    suspend fun setParticipation(eventId: String, state: ParticipationState) {
        val userId = currentUserId() ?: return
        val existing = supabase.from("attendees")
            .select { filter { eq("event_id", eventId); eq("user_id", userId) } }
            .decodeList<Attendee>()
        if (existing.isEmpty()) {
            supabase.from("attendees").insert(
                mapOf("event_id" to eventId, "user_id" to userId, "state" to state.name.lowercase())
            )
        } else {
            supabase.from("attendees").update(mapOf("state" to state.name.lowercase())) {
                filter { eq("event_id", eventId); eq("user_id", userId) }
            }
        }
    }

    // ─── Attendees ────────────────────────────────────────────────────────────

    suspend fun getAttendees(eventId: String): List<Attendee> =
        supabase.from("attendees").select { filter { eq("event_id", eventId) } }.decodeList()

    suspend fun checkInAttendee(eventId: String, inviteCode: String): Attendee =
        supabase.from("attendees").update(mapOf("state" to "checked_in")) {
            filter { eq("event_id", eventId); eq("invite_code", inviteCode) }; select()
        }.decodeSingle()

    // ─── Profile ──────────────────────────────────────────────────────────────

    suspend fun getUserProfile(userId: String): UserProfile =
        supabase.from("profiles").select { filter { eq("id", userId) } }.decodeSingle()

    suspend fun updateProfile(profile: UserProfile): UserProfile =
        supabase.from("profiles").upsert(profile) { select() }.decodeSingle()

    // ─── Analytics ────────────────────────────────────────────────────────────

    suspend fun getEventAnalytics(eventId: String): EventAnalytics =
        supabase.from("event_analytics").select { filter { eq("event_id", eventId) } }.decodeSingle()

    // ─── Messages ─────────────────────────────────────────────────────────────

    suspend fun sendMessage(message: OrganizerMessage): OrganizerMessage =
        supabase.from("organizer_messages").insert(message) { select() }.decodeSingle()

    suspend fun getMessages(eventId: String): List<OrganizerMessage> =
        supabase.from("organizer_messages").select { filter { eq("event_id", eventId) } }.decodeList()

    // ─── Trip Plans ───────────────────────────────────────────────────────────

    suspend fun saveTripPlan(plan: TripPlan): TripPlan =
        supabase.from("trip_plans").upsert(plan) { select() }.decodeSingle()

    suspend fun getTripPlan(id: String): TripPlan =
        supabase.from("trip_plans").select { filter { eq("id", id) } }.decodeSingle()

    // ─── Community Pulse ──────────────────────────────────────────────────────

    suspend fun getCommunityPulse(category: String): List<CommunityPulse> =
        supabase.from("community_pulse").select {
            if (category.isNotBlank()) filter { eq("category", category) }
        }.decodeList()

    // ─── Venues ───────────────────────────────────────────────────────────────

    suspend fun getVenueById(venueId: String): Venue =
        supabase.from("venues").select { filter { eq("id", venueId) } }.decodeSingle()

    suspend fun getEventsByVenue(venueId: String): List<Event> =
        supabase.from("events").select { filter { eq("venue_id", venueId) } }.decodeList()
}
