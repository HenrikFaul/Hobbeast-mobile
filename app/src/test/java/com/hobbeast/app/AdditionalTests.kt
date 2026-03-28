package com.hobbeast.app

import com.hobbeast.app.data.model.*

import com.hobbeast.app.domain.usecase.*
import io.mockk.*
import org.junit.*
import org.junit.Assert.*

// Re-export for convenience


// ─── RankForMeUseCase tests ───────────────────────────────────────────────────
// (Pure scoring logic, no coroutines needed for score function tests)

class RankingTest {

    @Test
    fun `interest match boosts score`() {
        val profile = UserProfile(id = "u1", interests = listOf("zene", "futás"))
        val musicEvent = Event(id = "1", title = "Koncert", category = "Zene",
            startTime = "2026-07-01T20:00:00", latitude = 47.50, longitude = 19.04)
        val sportsEvent = Event(id = "2", title = "Futóverseny", category = "Sport",
            startTime = "2026-07-02T09:00:00", latitude = 47.51, longitude = 19.05)

        // Music matches "zene" interest → higher score
        val musicScore = scoreEvent(musicEvent, profile.interests, 47.50, 19.04)
        val sportsScore = scoreEvent(sportsEvent, profile.interests, 47.50, 19.04)

        // Both match an interest ("futás" is in sport), but music is exact category match
        assertTrue("Music event should score ≥ sport event", musicScore >= sportsScore)
    }

    @Test
    fun `proximity within 2km gives max distance bonus`() {
        val nearScore = distanceScore(lat1 = 47.50, lon1 = 19.04, lat2 = 47.501, lon2 = 19.041)
        val farScore  = distanceScore(lat1 = 47.50, lon1 = 19.04, lat2 = 47.60, lon2 = 19.14)
        assertTrue("Near event should score higher than far", nearScore > farScore)
    }

    @Test
    fun `trending adds score`() {
        val base    = Event(id = "1", title = "A", category = "X", startTime = "2026-01-01T10:00:00")
        val trending = base.copy(id = "2", isTrending = true)
        assertTrue(scoreEvent(trending, emptyList(), 47.5, 19.0) > scoreEvent(base, emptyList(), 47.5, 19.0))
    }

    // Scoring helpers (mirror RankForMeUseCase logic without DI)
    private fun scoreEvent(event: Event, interests: List<String>, userLat: Double, userLon: Double): Double {
        var score = 0.0
        if (interests.any { event.category.lowercase().contains(it) }) score += 3.0
        val tagMatches = event.tags.count { tag -> interests.any { tag.lowercase().contains(it) } }
        score += tagMatches * 1.5
        score += (event.communityPulseScore ?: 0.0) * 2.0
        if (event.latitude != null && event.longitude != null)
            score += distanceScore(userLat, userLon, event.latitude, event.longitude)
        if (event.isTrending) score += 1.0
        if (event.isFeatured) score += 0.5
        return score
    }

    private fun distanceScore(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dist = haversine(lat1, lon1, lat2, lon2)
        return when {
            dist < 2  -> 2.0
            dist < 5  -> 1.5
            dist < 10 -> 1.0
            dist < 25 -> 0.5
            else      -> 0.0
        }
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

// ─── GetDiscoverySurfacesUseCase tests ────────────────────────────────────────

class DiscoverySurfacesTest {

    private val useCase = GetDiscoverySurfacesUseCase()

    private val events = listOf(
        Event(id = "1", title = "T1", category = "X", startTime = "2026-01-01T10:00:00", isTrending = true),
        Event(id = "2", title = "T2", category = "X", startTime = "2026-01-02T10:00:00", isTrending = true),
        Event(id = "3", title = "F1", category = "X", startTime = "2026-01-03T10:00:00", isFeatured = true),
        Event(id = "4", title = "E1", category = "X", startTime = "2026-01-04T10:00:00", isEarlyAccess = true),
        Event(id = "5", title = "Plain", category = "X", startTime = "2026-01-05T10:00:00"),
    )

    @Test
    fun `trending surface contains only trending events`() {
        val surfaces = useCase(events)
        assertTrue(surfaces.trending.all { it.isTrending })
        assertEquals(2, surfaces.trending.size)
    }

    @Test
    fun `featured surface contains only featured events`() {
        val surfaces = useCase(events)
        assertTrue(surfaces.featured.all { it.isFeatured })
    }

    @Test
    fun `earlyAccess surface contains only early access events`() {
        val surfaces = useCase(events)
        assertTrue(surfaces.earlyAccess.all { it.isEarlyAccess })
    }

    @Test
    fun `plain event does not appear in any surface`() {
        val surfaces = useCase(events)
        val allSurface = surfaces.trending + surfaces.featured + surfaces.earlyAccess
        assertFalse(allSurface.any { it.id == "5" })
    }
}

// ─── UpdateParticipationUseCase state machine tests ───────────────────────────

class ParticipationStateMachineTest {

    private val repo = mockk<com.hobbeast.app.data.repository.EventRepository>()
    private val useCase = UpdateParticipationUseCase(repo)

    @Before
    fun setup() {
        coEvery { repo.setParticipation(any(), any()) } returns Result.success(Unit)
    }

    @Test
    fun `NONE can transition to GOING`() = kotlinx.coroutines.runBlocking {
        val result = useCase("e1", ParticipationState.NONE, ParticipationState.GOING)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `NONE can transition to INTERESTED`() = kotlinx.coroutines.runBlocking {
        val result = useCase("e1", ParticipationState.NONE, ParticipationState.INTERESTED)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `CHECKED_IN cannot transition anywhere`() = kotlinx.coroutines.runBlocking {
        val result = useCase("e1", ParticipationState.CHECKED_IN, ParticipationState.GOING)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `GOING can transition to CHECKED_IN`() = kotlinx.coroutines.runBlocking {
        val result = useCase("e1", ParticipationState.GOING, ParticipationState.CHECKED_IN)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `WAITLISTED can transition to GOING when capacity opens`() = kotlinx.coroutines.runBlocking {
        val result = useCase("e1", ParticipationState.WAITLISTED, ParticipationState.GOING)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `GOING cannot transition to WAITLISTED`() = kotlinx.coroutines.runBlocking {
        val result = useCase("e1", ParticipationState.GOING, ParticipationState.WAITLISTED)
        assertTrue(result.isFailure)
    }
}

// ─── Hub quality edge cases ───────────────────────────────────────────────────

class HubQualityEdgeCasesTest {

    private val useCase = ScoreHubQualityUseCase()

    @Test
    fun `zero participants returns zero activity`() {
        val result = useCase(0, 0, 0.0, 0.0, 999)
        assertEquals(0.0, result.activityScore, 0.001)
        assertFalse(result.shouldSurface)
    }

    @Test
    fun `perfect scenario returns high activity and confidence`() {
        val result = useCase(
            participantCount = 100,
            eventCount = 20,
            recurringRatio = 1.0,
            avgAttendance = 30.0,
            recentActivityDays = 1,
        )
        assertTrue("Expected activityScore > 0.9, got ${result.activityScore}", result.activityScore > 0.9)
        assertTrue("Expected confidence > 0.8, got ${result.signalConfidence}", result.signalConfidence > 0.8)
        assertTrue(result.shouldSurface)
    }

    @Test
    fun `exactly 5 participants crosses confidence threshold`() {
        val below = useCase(4, 2, 0.5, 10.0, 5)
        val above = useCase(5, 2, 0.5, 10.0, 5)
        assertTrue("5 participants should have higher confidence than 4", above.signalConfidence > below.signalConfidence)
    }
}
