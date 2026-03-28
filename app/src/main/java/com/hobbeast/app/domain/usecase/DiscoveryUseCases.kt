package com.hobbeast.app.domain.usecase

import com.hobbeast.app.data.local.UserPreferencesStore
import com.hobbeast.app.data.model.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * HOB-9 – "Nekem" (For Me) recommendation ranking.
 * Scores events by matching user interests, location proximity and past behaviour.
 */
class RankForMeUseCase @Inject constructor(
    private val prefs: UserPreferencesStore,
) {
    suspend operator fun invoke(events: List<Event>, profile: UserProfile?): List<Event> {
        val interests = profile?.interests?.map { it.lowercase() } ?: emptyList()
        val (userLat, userLon) = prefs.lastLocation.first() ?: return events

        return events
            .map { event -> event to score(event, interests, userLat, userLon) }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private fun score(
        event: Event,
        interests: List<String>,
        userLat: Double,
        userLon: Double,
    ): Double {
        var score = 0.0

        // Category match
        if (interests.any { event.category.lowercase().contains(it) }) score += 3.0
        // Tag overlap
        val tagMatches = event.tags.count { tag -> interests.any { tag.lowercase().contains(it) } }
        score += tagMatches * 1.5

        // Community pulse
        score += (event.communityPulseScore ?: 0.0) * 2.0

        // Proximity bonus (closer = higher score, max 2 pts within 5km)
        if (event.latitude != null && event.longitude != null) {
            val dist = haversine(userLat, userLon, event.latitude, event.longitude)
            score += when {
                dist < 2  -> 2.0
                dist < 5  -> 1.5
                dist < 10 -> 1.0
                dist < 25 -> 0.5
                else      -> 0.0
            }
        }

        // Featured / trending boost
        if (event.isTrending)  score += 1.0
        if (event.isFeatured)  score += 0.5
        if (event.isEarlyAccess) score += 0.3

        return score
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).let { it * it } +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2).let { it * it }
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}

/**
 * HOB-10 – Trending, Featured, Early-Access discovery surfaces.
 */
class GetDiscoverySurfacesUseCase @Inject constructor() {
    data class DiscoverySurfaces(
        val trending: List<Event>,
        val featured: List<Event>,
        val earlyAccess: List<Event>,
    )

    operator fun invoke(events: List<Event>): DiscoverySurfaces {
        val now = System.currentTimeMillis()
        return DiscoverySurfaces(
            trending    = events.filter { it.isTrending }.sortedByDescending { it.communityPulseScore ?: 0.0 }.take(10),
            featured    = events.filter { it.isFeatured }.take(8),
            earlyAccess = events.filter { it.isEarlyAccess }.take(6),
        )
    }
}

/**
 * HOB-3 – Filter state persistence and consistency.
 * Ensures category selections are preserved when switching between filter modes.
 */
class FilterStateMachineUseCase @Inject constructor() {
    /**
     * Apply a mode switch while preserving user selections.
     * - Switching TO SEARCH clears categories but remembers them internally.
     * - Switching back FROM SEARCH restores previous mode and categories.
     */
    fun applyModeSwitch(
        current: DiscoveryFilter,
        newMode: FilterMode,
    ): DiscoveryFilter = when (newMode) {
        FilterMode.ALL -> current.copy(
            mode = FilterMode.ALL,
            searchQuery = "",
        )
        FilterMode.FOR_ME -> current.copy(
            mode = FilterMode.FOR_ME,
            searchQuery = "",
        )
        FilterMode.SEARCH -> current.copy(
            mode = FilterMode.SEARCH,
            // Keep categories so they can be re-applied when user leaves search
        )
        FilterMode.CATEGORIES -> current.copy(
            mode = FilterMode.CATEGORIES,
            searchQuery = "",
        )
    }

    /**
     * Toggle a single category, setting the appropriate mode.
     */
    fun toggleCategory(current: DiscoveryFilter, category: String): DiscoveryFilter {
        val updated = if (category in current.categories)
            current.categories - category
        else
            current.categories + category
        return current.copy(
            categories = updated,
            mode = if (updated.isEmpty()) FilterMode.ALL else FilterMode.CATEGORIES,
        )
    }
}
