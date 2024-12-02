package com.app.builder.domain.usecase.ticket

import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.TicketTable

/**
 * Gateway implementation for ticket use cases.
 *
 * @property database The database instance used to execute transactions.
 */
class TicketGateway(
    private val database: R2dbcDatabase
): TicketUseCases {

    override suspend fun createTicket(userUuid: Uuid): Uuid? = withContext(context = Dispatcher.IO) {
        val ticketUuid = uuid()
        val now = now()
        val ticketTtl = ServerConfigs.configs.ticketTtl.milliseconds

        database.safeTransaction {
            TicketTable.deleteWhere { TicketTable.expiresAt less now }
            TicketTable.insert {
                it[TicketTable.uuid] = ticketUuid
                it[TicketTable.userUuid] = userUuid
                it[TicketTable.expiresAt] = now + ticketTtl
            }.insertedCount > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to create ticket for user $userUuid", throwable = it)
        }.getOrDefault(defaultValue = false).let { created ->
            if (created) ticketUuid else null
        }
    }

    override suspend fun consumeTicket(ticketUuid: Uuid): Uuid? = withContext(context = Dispatcher.IO) {
        val now = now()

        database.safeTransaction {
            val row = TicketTable
                .selectAll()
                .where { TicketTable.uuid eq ticketUuid }
                .firstOrNull() ?: return@safeTransaction null
            if (TicketTable.deleteWhere { TicketTable.uuid eq ticketUuid } == 0) return@safeTransaction null
            if (row[TicketTable.expiresAt] < now) return@safeTransaction null
            row[TicketTable.userUuid]
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to consume ticket $ticketUuid", throwable = it)
        }.getOrNull()
    }

    companion object {
        private const val TAG = "TicketGateway"
    }
}
