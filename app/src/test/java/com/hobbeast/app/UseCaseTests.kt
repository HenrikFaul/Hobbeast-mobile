package com.hobbeast.app

import app.cash.turbine.test
import com.hobbeast.app.data.local.UserPreferencesStore
import com.hobbeast.app.data.model.*
import com.hobbeast.app.domain.usecase.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

// ─── FilterStateMachineUseCase tests ─────────────────────────────────────────

class FilterStateMachineUseCaseTest {

    private val useCase = FilterStateMachineUseCase()
    private val base = DiscoveryFilter(mode = FilterMode.ALL, categories = setOf("Zene", "Sport"))

    @Test
    fun `switching to ALL clears search query`() {
        val result = useCase.applyModeSwitch(base.copy(searchQuery = "test"), FilterMode.ALL)
        assertEquals(FilterMode.ALL, result.mode)
        assertEquals("", result.searchQuery)
    }

    @Test
    fun `switching to SEARCH preserves categories`() {
        val result = useCase.applyModeSwitch(base, FilterMode.SEARCH)
        assertEquals(FilterMode.SEARCH, result.mode)
        assertEquals(setOf("Zene", "Sport"), result.categories)
    }

    @Test
    fun `switching to FOR_ME clears search query`() {
        val result = useCase.applyModeSwitch(base.copy(searchQuery = "futás"), FilterMode.FOR_ME)
        assertEquals(FilterMode.FOR_ME, result.mode)
        assertEquals("", result.searchQuery)
    }

    @Test
    fun `toggling new category adds it and sets CATEGORIES mode`() {
        val result = useCase.toggleCategory(DiscoveryFilter(), "Zene")
        assertTrue("Zene" in result.categories)
        assertEquals(FilterMode.CATEGORIES, result.mode)
    }

    @Test
    fun `toggling existing category removes it`() {
        val withZene = base.copy(mode = FilterMode.CATEGORIES)
        val result = useCase.toggleCategory(withZene, "Zene")
        assertFalse("Zene" in result.categories)
    }

    @Test
    fun `removing all categories reverts to ALL mode`() {
        val oneCategory = DiscoveryFilter(mode = FilterMode.CATEGORIES, categories = setOf("Zene"))
        val result = useCase.toggleCategory(oneCategory, "Zene")
        assertEquals(FilterMode.ALL, result.mode)
        assertTrue(result.categories.isEmpty())
    }
}

// ─── ScoreHubQualityUseCase tests ─────────────────────────────────────────────

class ScoreHubQualityUseCaseTest {

    private val useCase = ScoreHubQualityUseCase()

    @Test
    fun `small thin community has low confidence`() {
        val result = useCase(participantCount = 3, eventCount = 1, recurringRatio = 0.0, avgAttendance = 5.0, recentActivityDays = 10)
        assertTrue("Confidence should be low for 3 participants", result.signalConfidence < 0.4)
        assertFalse(result.shouldSurface)
    }

    @Test
    fun `active community with no events is underserved`() {
        val result = useCase(participantCount = 15, eventCount = 1, recurringRatio = 0.1, avgAttendance = 12.0, recentActivityDays = 5)
        assertTrue(result.isUnderserved)
        assertTrue(result.shouldSurface)
    }

    @Test
    fun `stale community reduces confidence`() {
        val result = useCase(participantCount = 20, eventCount = 5, recurringRatio = 0.5, avgAttendance = 15.0, recentActivityDays = 65)
        assertTrue("Stale community should have lower confidence", result.signalConfidence < 0.6)
    }

    @Test
    fun `healthy active community has high activity score`() {
        val result = useCase(participantCount = 80, eventCount = 15, recurringRatio = 0.7, avgAttendance = 25.0, recentActivityDays = 3)
        assertTrue("Active community should score high", result.activityScore > 0.5)
        assertTrue(result.shouldSurface)
    }
}

// ─── ValidateTicketTiersUseCase tests ─────────────────────────────────────────

class ValidateTicketTiersUseCaseTest {

    private val useCase = ValidateTicketTiersUseCase()

    @Test
    fun `empty tiers is valid (free event)`() {
        val result = useCase(emptyList())
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `tier with blank name fails`() {
        val result = useCase(listOf(TicketTier(id = "1", name = "", price = 1000.0)))
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("neve") })
    }

    @Test
    fun `tier with negative price fails`() {
        val result = useCase(listOf(TicketTier(id = "1", name = "Alap", price = -100.0)))
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("negatív") })
    }

    @Test
    fun `valid tier passes`() {
        val result = useCase(listOf(TicketTier(id = "1", name = "Alap", price = 2500.0, available = 50)))
        assertTrue(result.isValid)
    }
}

// ─── ExportAttendeesUseCase tests ─────────────────────────────────────────────

class ExportAttendeesUseCaseTest {

    private val useCase = ExportAttendeesUseCase()

    @Test
    fun `CSV has header row`() {
        val csv = useCase.toCsv(emptyList())
        assertTrue(csv.startsWith("Név,"))
    }

    @Test
    fun `each attendee becomes one row`() {
        val attendees = listOf(
            Attendee(id = "1", eventId = "e1", userId = "u1", userName = "Kis Péter",
                state = ParticipationState.GOING, joinedAt = "2026-04-01T10:00:00"),
            Attendee(id = "2", eventId = "e1", userId = "u2", userName = "Nagy Kata",
                state = ParticipationState.CHECKED_IN, joinedAt = "2026-04-01T11:00:00",
                checkedInAt = "2026-04-05T18:30:00"),
        )
        val csv = useCase.toCsv(attendees)
        val lines = csv.lines()
        assertEquals(3, lines.size) // header + 2 rows
        assertTrue(csv.contains("Kis Péter"))
        assertTrue(csv.contains("Nagy Kata"))
    }

    @Test
    fun `checked-in attendee has checkin timestamp`() {
        val attendees = listOf(
            Attendee(id = "1", eventId = "e1", userId = "u1", userName = "Test",
                state = ParticipationState.CHECKED_IN, joinedAt = "2026-04-01T10:00:00",
                checkedInAt = "2026-04-05T18:30:00"),
        )
        val csv = useCase.toCsv(attendees)
        assertTrue(csv.contains("2026-04-05"))
    }
}

// ─── JoinEventUseCase tests ────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class JoinEventUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()

    @MockK lateinit var eventRepository: com.hobbeast.app.data.repository.EventRepository
    @MockK lateinit var supabase: com.hobbeast.app.data.remote.SupabaseDataSource
    private lateinit var useCase: JoinEventUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        useCase = JoinEventUseCase(eventRepository, supabase)
    }

    @After
    fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `already going returns AlreadyGoing`() = runTest {
        val event = Event(id = "1", participationState = ParticipationState.GOING)
        val result = useCase(event)
        assertEquals(JoinEventUseCase.JoinResult.AlreadyGoing, result)
    }

    @Test
    fun `full event returns Waitlisted`() = runTest {
        val event = Event(id = "1", maxCapacity = 10, attendeeCount = 10)
        coEvery { eventRepository.setParticipation("1", ParticipationState.WAITLISTED) } returns Result.success(Unit)
        val result = useCase(event)
        assertEquals(JoinEventUseCase.JoinResult.Waitlisted, result)
    }

    @Test
    fun `open event returns Joined`() = runTest {
        val event = Event(id = "1", maxCapacity = 50, attendeeCount = 10)
        coEvery { eventRepository.setParticipation("1", ParticipationState.GOING) } returns Result.success(Unit)
        val result = useCase(event)
        assertEquals(JoinEventUseCase.JoinResult.Joined, result)
    }
}
