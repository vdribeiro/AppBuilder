package com.app.builder.domain.usecase.ticket

import kotlin.uuid.Uuid
import com.app.builder.domain.usecase.Outcome

/** Defines the business workflows for single-use, short-lived push connection tickets. */
interface TicketUseCases {

    /**
     * Creates a single-use ticket for a user, set to expire after the configured TTL.
     *
     * @param userUuid The UUID of the user requesting the ticket.
     * @return [Outcome.Success] with the ticket UUID, or [Outcome.Error] when the ticket could not be persisted.
     */
    suspend fun createTicket(userUuid: Uuid): Outcome<Uuid>

    /**
     * Atomically consumes a ticket, guaranteeing it cannot be redeemed twice, even by concurrent consumers on different server instances.
     *
     * @param ticketUuid The ticket to consume.
     * @return [Outcome.Success] with the UUID of the user the ticket was created for, [Outcome.Rejected] when the ticket is unknown, expired or already consumed, or [Outcome.Error] when the operation failed.
     */
    suspend fun consumeTicket(ticketUuid: Uuid): Outcome<Uuid>
}
