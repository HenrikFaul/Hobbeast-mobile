package com.hobbeast.app.domain.usecase

import com.hobbeast.app.data.model.*
import com.hobbeast.app.data.remote.SupabaseDataSource
import javax.inject.Inject

/**
 * HOB-176 / HOB-182 – Hidden hub domain model and quality scoring.
 *
 * A "hub" is an INTERNAL clustering concept – never exposed to users by name.
 * The product surface speaks in terms of "scenes" and "community pulse" only.
 */
data class HiddenHub(
    val id: String,
    val sceneLabel: String,           // e.g. "Futók – XIII. kerület"
    val category: String,
    val location: Pair<Double, Double>?,
    val participantCount: Int,        // Aggregated, never individual-linked
    val recurringFormats: List<String>,
    val activityScore: Double,        // 0.0–1.0
    val signalConfidence: Double,     // 0.0–1.0, below 0.6 → suppress
    val isUnderserved: Boolean,
    val reviewStatus: HubReviewStatus,
)

enum class HubReviewStatus { PENDING, APPROVED, SUPPRESSED }

/**
 * HOB-182 – Hub quality scoring and underserved-scene detection.
 */
class ScoreHubQualityUseCase @Inject constructor() {
    data class HubQuality(
        val activityScore: Double,
        val signalConfidence: Double,
        val isUnderserved: Boolean,
        val shouldSurface: Boolean,
    )

    operator fun invoke(
        participantCount: Int,
        eventCount: Int,
        recurringRatio: Double,     // fraction of events that are recurring
        avgAttendance: Double,
        recentActivityDays: Int,    // days since last event
    ): HubQuality {
        // Activity score: weighted composite
        val activity = (
            (participantCount.coerceAtMost(100) / 100.0) * 0.3 +
            (eventCount.coerceAtMost(20) / 20.0) * 0.3 +
            recurringRatio * 0.2 +
            (avgAttendance.coerceAtMost(30.0) / 30.0) * 0.2
        ).coerceIn(0.0, 1.0)

        // Signal confidence: drop if data is thin or stale
        val confidence = when {
            participantCount < 5   -> 0.2
            participantCount < 10  -> 0.5
            recentActivityDays > 60 -> 0.4
            recentActivityDays > 30 -> 0.7
            else                    -> 0.9
        }

        // Underserved: decent community but few events
        val underserved = participantCount >= 8 && eventCount < 2 && recurringRatio < 0.3

        // Only surface hubs that pass confidence threshold
        val shouldSurface = confidence >= 0.6 && activity >= 0.2

        return HubQuality(activity, confidence, underserved, shouldSurface)
    }
}

/**
 * HOB-183 – Hub-backed event opportunity recommendations for organizers.
 */
class HubEventOpportunityUseCase @Inject constructor(
    private val supabase: SupabaseDataSource,
) {
    data class EventOpportunity(
        val sceneLabel: String,
        val suggestedCategory: String,
        val suggestedFormats: List<String>,
        val estimatedInterest: Int,
        val reasoning: String,          // Human-readable, not "AI decided"
    )

    suspend fun getOpportunities(): List<EventOpportunity> {
        return try {
            supabase.getCommunityPulse("")
                .filter { it.isUnderserved && it.activityScore > 0.3 }
                .map { pulse ->
                    EventOpportunity(
                        sceneLabel = pulse.sceneLabel,
                        suggestedCategory = pulse.sceneLabel.split(" ").first(),
                        suggestedFormats = pulse.recurringFormats,
                        estimatedInterest = (pulse.activityScore * 50).toInt().coerceAtLeast(5),
                        reasoning = buildReasoning(pulse),
                    )
                }
        } catch (e: Exception) { emptyList() }
    }

    private fun buildReasoning(pulse: CommunityPulse): String {
        val parts = mutableListOf<String>()
        if (pulse.activityScore > 0.5) parts += "aktív közösség"
        if (pulse.recurringFormats.isNotEmpty()) parts += "korábbi: ${pulse.recurringFormats.first()}"
        if (pulse.isUnderserved) parts += "jelenleg nincs rendszeres esemény"
        return if (parts.isEmpty()) "Potenciális helyszín" else parts.joinToString(", ")
    }
}

/**
 * HOB-184 – Future opt-in community exposure model.
 * Defines the consent-aware path from hidden → visible community.
 */
enum class CommunityExposureLevel {
    HIDDEN,           // Default: internal analytics only
    AGGREGATE_PULSE,  // Community pulse surfaces, no individual data
    OPT_IN_SCENE,     // Users explicitly join a named scene
    PUBLIC_COMMUNITY, // Fully visible community (future)
}

data class CommunityExposurePolicy(
    val level: CommunityExposureLevel,
    val requiresUserConsent: Boolean,
    val dataRetentionDays: Int,
    val canShowIndividualProfiles: Boolean,
) {
    companion object {
        val DEFAULT = CommunityExposurePolicy(
            level = CommunityExposureLevel.AGGREGATE_PULSE,
            requiresUserConsent = false,
            dataRetentionDays = 90,
            canShowIndividualProfiles = false,
        )
    }
}
