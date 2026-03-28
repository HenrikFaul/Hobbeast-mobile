package com.hobbeast.app.domain.usecase

import com.hobbeast.app.data.model.*
import com.hobbeast.app.data.remote.SupabaseDataSource
import com.hobbeast.app.data.repository.EventRepository
import javax.inject.Inject

/**
 * HOB-15 – Capacity and waitlist handling.
 * Determines the correct participation action given current event capacity.
 */
class JoinEventUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val supabase: SupabaseDataSource,
) {
    sealed interface JoinResult {
        data object Joined      : JoinResult
        data object Waitlisted  : JoinResult
        data object AlreadyGoing: JoinResult
        data class  Error(val message: String) : JoinResult
    }

    suspend operator fun invoke(event: Event): JoinResult {
        return when (event.participationState) {
            ParticipationState.GOING      -> JoinResult.AlreadyGoing
            ParticipationState.WAITLISTED -> JoinResult.Waitlisted
            else -> {
                val isFull = event.maxCapacity != null && event.attendeeCount >= event.maxCapacity
                val targetState = if (isFull) ParticipationState.WAITLISTED else ParticipationState.GOING

                eventRepository.setParticipation(event.id, targetState)
                    .fold(
                        onSuccess = {
                            if (isFull) JoinResult.Waitlisted else JoinResult.Joined
                        },
                        onFailure = { JoinResult.Error(it.message ?: "Hiba") },
                    )
            }
        }
    }
}

/**
 * HOB-16 – Rich RSVP state machine.
 * Validates allowed state transitions.
 */
class UpdateParticipationUseCase @Inject constructor(
    private val eventRepository: EventRepository,
) {
    /** Allowed transitions: from -> setOf(allowed next states) */
    private val allowedTransitions = mapOf(
        ParticipationState.NONE       to setOf(ParticipationState.INTERESTED, ParticipationState.GOING, ParticipationState.WAITLISTED),
        ParticipationState.INTERESTED to setOf(ParticipationState.GOING, ParticipationState.WAITLISTED, ParticipationState.NONE),
        ParticipationState.GOING      to setOf(ParticipationState.NONE, ParticipationState.CHECKED_IN, ParticipationState.INTERESTED),
        ParticipationState.WAITLISTED to setOf(ParticipationState.NONE, ParticipationState.GOING),
        ParticipationState.CHECKED_IN to emptySet(),      // Terminal state
        ParticipationState.DECLINED   to setOf(ParticipationState.INTERESTED, ParticipationState.GOING),
    )

    suspend operator fun invoke(
        eventId: String,
        currentState: ParticipationState,
        targetState: ParticipationState,
    ): Result<Unit> {
        val allowed = allowedTransitions[currentState] ?: emptySet()
        if (targetState !in allowed) {
            return Result.failure(
                IllegalStateException("Transition $currentState → $targetState nem engedélyezett")
            )
        }
        return eventRepository.setParticipation(eventId, targetState)
    }
}

/**
 * HOB-42 – Payment provider integration contract.
 * Defines the interface for future payment provider implementations.
 */
interface PaymentProvider {
    suspend fun initiateSession(
        eventId: String,
        ticketTierId: String,
        userId: String,
        amount: Double,
        currency: String,
    ): PaymentSession

    suspend fun confirmSession(sessionId: String): PaymentResult
    suspend fun cancelSession(sessionId: String)
}

data class PaymentSession(
    val sessionId: String,
    val checkoutUrl: String,
    val expiresAt: String,
)

sealed interface PaymentResult {
    data class  Success(val transactionId: String) : PaymentResult
    data class  Failure(val reason: String)        : PaymentResult
    data object Cancelled                          : PaymentResult
}

/**
 * HOB-40 – Paid event pricing model.
 * Validates ticket tier configuration on event creation.
 */
class ValidateTicketTiersUseCase @Inject constructor() {
    data class ValidationResult(val isValid: Boolean, val errors: List<String>)

    operator fun invoke(tiers: List<TicketTier>): ValidationResult {
        val errors = mutableListOf<String>()

        if (tiers.isEmpty()) return ValidationResult(true, emptyList()) // Free event OK

        tiers.forEachIndexed { index, tier ->
            if (tier.name.isBlank())  errors += "A ${index + 1}. jegytípusnak van neve"
            if (tier.price < 0)       errors += "A ${index + 1}. jegytípus ára nem lehet negatív"
            if (tier.available != null && tier.available <= 0)
                errors += "A ${index + 1}. jegytípus darabszáma pozitív kell legyen"
        }

        val totalCapacity = tiers.mapNotNull { it.available }.sum()
        if (tiers.any { it.available != null } && totalCapacity == 0) {
            errors += "Legalább 1 jegy elérhetőnek kell lennie"
        }

        return ValidationResult(errors.isEmpty(), errors)
    }
}

/**
 * HOB-43 – Attendee export use case.
 * Produces a CSV string from the attendee list.
 */
class ExportAttendeesUseCase @Inject constructor() {
    fun toCsv(attendees: List<Attendee>): String {
        val header = "Név,Állapot,Csatlakozás dátuma,Meghívókód,Check-in időpont"
        val rows = attendees.map { a ->
            listOf(
                a.userName,
                a.state.name.lowercase(),
                a.joinedAt.take(10),
                a.inviteCode ?: "",
                a.checkedInAt?.take(16) ?: "",
            ).joinToString(",") { "\"$it\"" }
        }
        return (listOf(header) + rows).joinToString("\n")
    }
}
