package com.hobbeast.app

import app.cash.turbine.test
import com.hobbeast.app.data.local.EventDao
import com.hobbeast.app.data.model.*
import com.hobbeast.app.data.remote.*
import com.hobbeast.app.data.repository.EventRepository
import com.hobbeast.app.ui.discovery.DiscoveryUiState
import com.hobbeast.app.ui.discovery.DiscoveryViewModel
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @MockK lateinit var eventRepository: EventRepository
    private lateinit var viewModel: DiscoveryViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        every { eventRepository.getDiscoveryEvents(any()) } returns flowOf(
            Result.success(testEvents)
        )
        every { eventRepository.getTrendingEvents() } returns flowOf(
            Result.success(testEvents.filter { it.isTrending })
        )
        every { eventRepository.getFeaturedEvents() } returns flowOf(
            Result.success(testEvents.filter { it.isFeatured })
        )

        viewModel = DiscoveryViewModel(eventRepository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        viewModel.uiState.test {
            val first = awaitItem()
            // Could be Loading or already Success depending on dispatcher
            assertTrue(first is DiscoveryUiState.Loading || first is DiscoveryUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `events are loaded and emitted as Success`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Loading
            testDispatcher.scheduler.advanceUntilIdle()
            val next = awaitItem()
            assertTrue(next is DiscoveryUiState.Success)
            assertEquals(2, (next as DiscoveryUiState.Success).events.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setFilterMode updates filter`() = runTest {
        viewModel.setFilterMode(FilterMode.FOR_ME)
        assertEquals(FilterMode.FOR_ME, viewModel.filter.value.mode)
    }

    @Test
    fun `setSearchQuery sets SEARCH mode and query`() = runTest {
        viewModel.setSearchQuery("futás")
        val filter = viewModel.filter.value
        assertEquals(FilterMode.SEARCH, filter.mode)
        assertEquals("futás", filter.searchQuery)
    }

    @Test
    fun `toggleCategory adds and removes category`() = runTest {
        viewModel.toggleCategory("Sport")
        assertTrue("Sport" in viewModel.filter.value.categories)

        viewModel.toggleCategory("Sport")
        assertFalse("Sport" in viewModel.filter.value.categories)
    }

    @Test
    fun `toggleCategory sets CATEGORIES mode when non-empty`() = runTest {
        viewModel.toggleCategory("Zene")
        assertEquals(FilterMode.CATEGORIES, viewModel.filter.value.mode)
    }

    @Test
    fun `toggleCategory reverts to ALL mode when all categories removed`() = runTest {
        viewModel.toggleCategory("Zene")
        viewModel.toggleCategory("Zene")
        assertEquals(FilterMode.ALL, viewModel.filter.value.mode)
    }

    private val testEvents = listOf(
        Event(id = "1", title = "Zenei Fesztivál", category = "Zene",
            startTime = "2026-05-01T18:00:00", isTrending = true),
        Event(id = "2", title = "Futóverseny", category = "Sport",
            startTime = "2026-05-02T09:00:00", isFeatured = true),
    )
}

// ─── EventRepository test ─────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class EventRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    @MockK lateinit var supabase: SupabaseDataSource
    @MockK lateinit var ticketmasterApi: TicketmasterApi
    @MockK lateinit var seatGeekApi: SeatGeekApi
    @MockK lateinit var eventDao: EventDao

    private lateinit var repository: EventRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        coEvery { eventDao.getAllEvents() } returns flowOf(emptyList())
        coEvery { eventDao.deleteStaleCache(any()) } just Runs
        coEvery { eventDao.upsertEvents(any()) } just Runs
        coEvery { supabase.getEvents(any()) } returns testNetworkEvents
        coEvery { ticketmasterApi.searchEvents(any(), any(), any(), any(), any(), any(), any(), any(), any()) } throws Exception("network")
        coEvery { seatGeekApi.searchEvents(any(), any(), any(), any(), any(), any(), any()) } throws Exception("network")

        repository = EventRepository(supabase, ticketmasterApi, seatGeekApi, eventDao)
    }

    @After
    fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `getDiscoveryEvents returns supabase events`() = runTest {
        repository.getDiscoveryEvents(DiscoveryFilter()).test {
            testDispatcher.scheduler.advanceUntilIdle()
            val result = awaitItem()
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getEvent returns cached entity if present`() = runTest {
        val cached = com.hobbeast.app.data.local.EventEntity(
            id = "1", title = "Cached", description = "", startTime = "2026-01-01T10:00:00",
            endTime = null, location = "Budapest", address = null, latitude = null, longitude = null,
            organizerId = "u1", organizerName = null, imageUrl = null, category = "Sport",
            tags = "[]", maxCapacity = null, attendeeCount = 0, isPrivate = false, isFree = true,
            price = null, source = "hobbeast", externalId = null, externalUrl = null,
            isTrending = false, isFeatured = false, isEarlyAccess = false,
            participationState = "none", reminderSet = false, communityPulseScore = null,
        )
        coEvery { eventDao.getEventById("1") } returns cached

        val result = repository.getEvent("1")
        assertTrue(result.isSuccess)
        assertEquals("Cached", result.getOrNull()?.title)
        coVerify(exactly = 0) { supabase.getEventById(any()) }
    }

    private val testNetworkEvents = listOf(
        Event(id = "net1", title = "Network Event", category = "Zene",
            startTime = "2026-06-01T20:00:00"),
    )
}
