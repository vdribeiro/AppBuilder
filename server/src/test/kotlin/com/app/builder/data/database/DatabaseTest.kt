package com.app.builder.data.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.database.table.TicketTable
import com.app.builder.data.database.table.UserTable
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class DatabaseTest: TestCase() {

    /** Verifies that reset() drops the schema so subsequent queries fail, and create() recreates it so queries succeed again. */
    @Test
    fun resetAndCreate() = runServerTest {
        val database = dependency.get().database

        assertTrue(actual = database.safeTransaction { UserTable.selectAll().count() }.isSuccess)

        database.reset()
        assertTrue(actual = database.safeTransaction { UserTable.selectAll().count() }.isFailure)

        database.create()
        assertTrue(actual = database.safeTransaction { UserTable.selectAll().count() }.isSuccess)
    }

    /** Verifies that safeTransaction returns the block's result wrapped in a successful [Result]. */
    @Test
    fun safeTransactionReturnsSuccessResult() = runServerTest {
        val database = dependency.get().database

        val result = database.safeTransaction { UserTable.selectAll().count() }

        assertTrue(actual = result.isSuccess)
        assertEquals(expected = 2L, actual = result.getOrThrow())
    }

    /** Verifies that safeTransaction catches an exception thrown inside the block, returning a failed [Result], and rolls back any writes made before the throw. */
    @Test
    fun safeTransactionRollsBackOnFailure() = runServerTest {
        val database = dependency.get().database
        val ticketUuid = uuid()

        val result = database.safeTransaction {
            TicketTable.insert {
                it[TicketTable.uuid] = ticketUuid
                it[TicketTable.userUuid] = FakeData.adminUser.uuid
                it[TicketTable.expiresAt] = now()
            }
            error(message = "boom")
        }

        assertTrue(actual = result.isFailure)

        val persistedCount = database.safeTransaction {
            TicketTable.selectAll().where { TicketTable.uuid eq ticketUuid }.count()
        }.getOrThrow()
        assertEquals(expected = 0L, actual = persistedCount)
    }
}
