package com.app.builder.data.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import com.app.builder.test.ExcludeFromTesting

/** A no-op implementation of [SqlDriver]. */
@ExcludeFromTesting
object NoOpSqlDriver: SqlDriver {

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): QueryResult<R> = mapper(NoOpCursor)

    override fun execute(identifier: Int?, sql: String, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): QueryResult<Long> = QueryResult.Value(value = 0L)
    override fun newTransaction(): QueryResult<Transacter.Transaction> = QueryResult.Value(value = NoOpTransaction)
    override fun currentTransaction(): Transacter.Transaction? = null
    override fun addListener(vararg queryKeys: String, listener: Query.Listener) {}
    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {}
    override fun notifyListeners(vararg queryKeys: String) {}
    override fun close() {}

    /** A no-op implementation of [SqlCursor]. */
    private object NoOpCursor: SqlCursor {
        override fun next(): QueryResult<Boolean> = QueryResult.Value(value = false)
        override fun getString(index: Int): String? = null
        override fun getLong(index: Int): Long? = null
        override fun getBytes(index: Int): ByteArray? = null
        override fun getDouble(index: Int): Double? = null
        override fun getBoolean(index: Int): Boolean? = null
    }

    /** A no-op implementation of [Transacter.Transaction]. */
    private object NoOpTransaction: Transacter.Transaction() {
        override val enclosingTransaction: Transacter.Transaction? = null
        override fun endTransaction(successful: Boolean): QueryResult<Unit> = QueryResult.Value(value = Unit)
    }
}
