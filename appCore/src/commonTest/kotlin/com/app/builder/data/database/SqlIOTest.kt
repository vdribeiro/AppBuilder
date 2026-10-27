package com.app.builder.data.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import app.cash.sqldelight.Query
import app.cash.sqldelight.SuspendingTransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import com.app.builder.test.TestCase

class SqlIOTest: TestCase() {

    /** A [Query] serving a fixed list of string rows. */
    private class ListQuery(private val rows: List<String>): Query<String>(mapper = { cursor -> cursor.getString(index = 0).orEmpty() }) {
        override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> = mapper(ListCursor(rows = rows))
        override fun addListener(listener: Listener) {}
        override fun removeListener(listener: Listener) {}
    }

    /** A [SqlCursor] walking a fixed list of string rows in a single column. */
    private class ListCursor(private val rows: List<String>): SqlCursor {
        /** The index of the current row. */
        private var index = -1

        override fun next(): QueryResult<Boolean> = QueryResult.Value(value = ++index < rows.size)
        override fun getString(index: Int): String? = rows[this.index]
        override fun getLong(index: Int): Long? = null
        override fun getBytes(index: Int): ByteArray? = null
        override fun getDouble(index: Int): Double? = null
        override fun getBoolean(index: Int): Boolean? = null
    }

    /** A transacter over the [NoOpSqlDriver]. */
    private val transacter = object: SuspendingTransacterImpl(driver = NoOpSqlDriver) {}

    /** Verifies that [asFlow] emits the query rows mapped through the transform. */
    @Test
    fun asFlowMapsTheQueryRows() = runUnitTest {
        assertEquals(
            expected = listOf("A", "B"),
            actual = ListQuery(rows = listOf("a", "b")).asFlow { it.uppercase() }.first()
        )
    }

    /** Verifies that [safeTransaction] returns the body result as a success. */
    @Test
    fun safeTransactionReturnsTheBodyResult() = runUnitTest {
        assertEquals(expected = Result.success(value = 1), actual = transacter.safeTransaction { 1 })
    }

    /** Verifies that [safeTransaction] returns a failure when the body throws. */
    @Test
    fun safeTransactionCatchesTheBodyException() = runUnitTest {
        val result = transacter.safeTransaction<Int> { error(message = "boom") }

        assertTrue(actual = result.isFailure)
        assertEquals(expected = "boom", actual = result.exceptionOrNull()?.message)
    }
}
