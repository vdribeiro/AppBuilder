package com.app.builder.data.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import com.app.builder.test.TestCase

class NoOpSqlDriverTest: TestCase() {

    /** Verifies that [NoOpSqlDriver.executeQuery] hands the mapper a cursor with no rows. */
    @Test
    fun executeQueryYieldsAnEmptyCursor() = runUnitTest {
        val result = NoOpSqlDriver.executeQuery(
            identifier = null,
            sql = "SELECT 1",
            mapper = { cursor -> QueryResult.Value(value = cursor.next().value) },
            parameters = 0,
            binders = null
        )

        assertFalse(actual = result.value)
    }

    /** Verifies that [NoOpSqlDriver.execute] reports no affected rows. */
    @Test
    fun executeAffectsNoRows() = runUnitTest {
        assertEquals(expected = 0L, actual = NoOpSqlDriver.execute(identifier = null, sql = "DELETE", parameters = 0, binders = null).value)
    }

    /** Verifies that [NoOpSqlDriver] has no current transaction and runs a transaction body to completion. */
    @Test
    fun transactionsAreNoOps() = runUnitTest {
        assertNull(actual = NoOpSqlDriver.currentTransaction())

        assertEquals(expected = 1, actual = object: TransacterImpl(driver = NoOpSqlDriver) {}.transactionWithResult { 1 })
    }

    /** Verifies that listener registration, notification and closing are accepted without effect. */
    @Test
    fun listenersAndCloseAreNoOps() = runUnitTest {
        val listener = Query.Listener {}

        NoOpSqlDriver.addListener("table", listener = listener)
        NoOpSqlDriver.notifyListeners("table")
        NoOpSqlDriver.removeListener("table", listener = listener)
        NoOpSqlDriver.close()
    }
}
