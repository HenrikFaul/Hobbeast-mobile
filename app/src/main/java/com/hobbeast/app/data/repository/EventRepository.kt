package com.hobbeast.app.data.repository

import com.hobbeast.app.BuildConfig
import com.hobbeast.app.data.local.*
import com.hobbeast.app.data.model.*
import com.hobbeast.app.data.remote.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

private const val CACHE_TTL_MS = 5 * 60 * 1_000L  // 5 minutes

@Singleton
class EventRepository @Inject constructor(
    private val supabase: SupabaseDataSource,
    private val ticketmasterApi: TicketmasterApi,
    private val seatGeekApi: SeatGeekApi,
    private val eventDao: EventDao,
) {

    // â”€â”€â”€ Discovery (offline-first) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    fun getDiscoveryEvents(filter: DiscoveryFilter): Flow<Result<List<Event>>> = flow {
        // 1. Emit cached data immediately
        val cached = eventDao.getAllEvents().first().map { it.toModel() }
        if (cached.isNotEmpty()) {
            emit(Result.success(applyFilter(cached, filter)))
        }

        // 2. Fetch fresh data
        try {
            coroutineScope {
                val hobbeastDeferred = async { supabase.getEvents(filter) }
                val ticketmasterDeferred = async {
                    if (EventSource.TICKETMASTER in filter.sources || filter.sources.isEmpty())
                        fetchTicketmasterEvents(filter)
                    else emptyList()
                }
                val seatgeekDeferred = async {
                    if (EventSource.SEATGEEK in filter.sources || filter.sources.isEmpty())
                        fetchSeatGeekEvents(filter)
                    else emptyList()
                }

                val fresh = hobbeastDeferred.await() +
                        ticketmasterDeferred.await() +
                        seatgeekDeferred.await()

                // Persist to cache
                eventDao.deleteStaleCache(System.currentTimeMillis() - CACHE_TTL_MS)
                eventDao.upsertEvents(fresh.map { it.toEntity() })

                val ranked = rankEvents(fresh, filter)
                emit(Result.success(ranked))
            }
        } catch (e: Exception) {
            // If we already emitted cache, just silently fail; otherwise emit error
            if (cached.isEmpty()) emit(Result.failure(e))
        }
    }

    private fun applyFilter(events: List<Event>, filter: DiscoveryFilter): List<Event> {
        var result = events
        if (filter.searchQuery.isNotBlank()) {
            result = result.filter {
                it.title.contains(filter.searchQuery, ignoreCase = true) ||
                        it.description.contains(filter.searchQuery, ignoreCase = true)
            }
        }
        if (filter.categories.isNotEmpty()) {
            result = result.filter { it.category in filter.categories }
        }
        if (filter.showFreeOnly) {
            result = result.filter { it.isFree }
        }
        return rankEvents(result, filter)
    }

    private fun rankEvents(events: List<Event>, filter: DiscoveryFilter): List<Event> =
        when (filter.mode) {
            FilterMode.FOR_ME -> events.sortedByDescending { it.communityPulseScore ?: 0.0 }
            else -> events.sortedBy { it.startTime }
        }

    private suspend fun fetchTicketmasterEvents(filter: DiscoveryFilter): List<Event> = emptyList()

    private suspend fun fetchSeatGeekEvents(filter: DiscoveryFilter): List<Event> = emptyList()

    // â”€â”€â”€ Single event â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun getEvent(id: String): Result<Event> {
        // Try cache first
        eventDao.getEventById(id)?.let { return Result.success(it.toModel()) }
        return runCatching {
            val event = supabase.getEventById(id)
            eventDao.upsertEvent(event.toEntity())
            event
        }
    }

    // â”€â”€â”€ CRUD â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun createEvent(event: Event): Result<Event> = runCatching {
        val saved = supabase.createEvent(event)
        eventDao.upsertEvent(saved.toEntity())
        saved
    }

    suspend fun updateEvent(event: Event): Result<Event> = runCatching {
        val updated = supabase.updateEvent(event)
        eventDao.upsertEvent(updated.toEntity())
        updated
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> = runCatching {
        supabase.deleteEvent(eventId)
        eventDao.deleteEvent(eventId)
    }

    // â”€â”€â”€ Participation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun setParticipation(eventId: String, state: ParticipationState): Result<Unit> = runCatching {
        supabase.setParticipation(eventId, state)
        // Update local cache
        eventDao.getEventById(eventId)?.let { entity ->
            eventDao.upsertEvent(entity.copy(participationState = state.name.lowercase()))
        }
    }

    // â”€â”€â”€ Trending / Featured â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    fun getTrendingEvents(): Flow<Result<List<Event>>> =
        eventDao.getTrendingEvents().map { list -> Result.success(list.map { it.toModel() }) }

    fun getFeaturedEvents(): Flow<Result<List<Event>>> =
        eventDao.getFeaturedEvents().map { list -> Result.success(list.map { it.toModel() }) }

    fun getOrganizerEvents(userId: String): Flow<List<Event>> =
        eventDao.getOrganizerEvents(userId).map { list -> list.map { it.toModel() } }
}

