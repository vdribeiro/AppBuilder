package com.app.builder.domain.usecase.ticket

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.jetbrains.exposed.v1.r2dbc.selectAll
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.security.uuid
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.TicketTable
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TicketUseCasesTest: TestCase() {

    /** Verifies that a created ticket can be consumed exactly once and rejects unknown tickets. */
    @Test
    fun createAndConsumeSingleUseTicket() = runServerTest {
        val ticketUseCases = dependency.get()
            .useCases
            .ticketUseCases

        val ticketUuid = assertNotNull(actual = ticketUseCases.createTicket(userUuid = FakeData.adminUser.uuid))

        assertNull(actual = ticketUseCases.consumeTicket(ticketUuid = uuid()))
        assertEquals(expected = FakeData.adminUser.uuid, actual = ticketUseCases.consumeTicket(ticketUuid = ticketUuid))
        assertNull(actual = ticketUseCases.consumeTicket(ticketUuid = ticketUuid))
    }

    /** Verifies that an expired ticket cannot be consumed. */
    @Test
    fun expiredTicketIsRejected() = runServerTest {
        ServerConfigs.set { it.copy(ticketTtl = -1_000L) }
        val ticketUseCases = dependency.get()
            .useCases
            .ticketUseCases

        val ticketUuid = assertNotNull(actual = ticketUseCases.createTicket(userUuid = FakeData.adminUser.uuid))
        assertNull(actual = ticketUseCases.consumeTicket(ticketUuid = ticketUuid))
    }

    /** Verifies that creating a new ticket purges any previously expired tickets. */
    @Test
    fun createTicketPurgesExpiredTickets() = runServerTest {
        val ticketUseCases = dependency.get()
            .useCases
            .ticketUseCases
        val database = dependency.get().database

        ServerConfigs.set { it.copy(ticketTtl = -1_000L) }
        val expiredTicketUuid = assertNotNull(actual = ticketUseCases.createTicket(userUuid = FakeData.adminUser.uuid))
        assertEquals(expected = 1L, actual = database.safeTransaction { TicketTable.selectAll().count() }.getOrThrow())

        ServerConfigs.set { it.copy(ticketTtl = 30_000L) }
        assertNotNull(actual = ticketUseCases.createTicket(userUuid = FakeData.adminUser.uuid))

        assertEquals(expected = 1L, actual = database.safeTransaction { TicketTable.selectAll().count() }.getOrThrow())
        assertNull(actual = ticketUseCases.consumeTicket(ticketUuid = expiredTicketUuid))
    }
}
