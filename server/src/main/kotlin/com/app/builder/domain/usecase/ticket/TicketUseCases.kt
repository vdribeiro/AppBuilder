package com.app.builder.domain.usecase.ticket

import kotlin.uuid.Uuid

/** Defines the business workflows for single-use, short-lived push connection tickets. */
interface TicketUseCases {

    /**
     * Creates a single-use ticket for a user, set to expire after the configured TTL.
     *
     * @param userUuid The UUID of the user requesting the ticket.
     * @return The ticket UUID, or null if the ticket could not be persisted.
     */
    suspend fun createTicket(userUuid: Uuid): Uuid?

    /**
     * Atomically consumes a ticket, guaranteeing it cannot be redeemed twice, even by concurrent consumers on different server instances.
     *
     * @param ticketUuid The ticket to consume.
     * @return The UUID of the user the ticket was created for, or null if the ticket is unknown, expired, or already consumed.
     */
    suspend fun consumeTicket(ticketUuid: Uuid): Uuid?
}
