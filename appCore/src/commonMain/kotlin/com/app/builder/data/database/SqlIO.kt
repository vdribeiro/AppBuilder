package com.app.builder.data.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.Query
import app.cash.sqldelight.SuspendingTransacter
import app.cash.sqldelight.SuspendingTransactionWithReturn
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.flow.runSuspendCatching

/**
 * Safely executes a SQLDelight transaction, catching any exceptions and returning a [Result].
 * If the transaction completes successfully, it returns [Result.success] with the result of [body].
 * If an exception is thrown inside [body], SQLDelight will automatically roll back the transaction, and this function will catch the bubbled-up exception, returning it as a [Result.failure].
 *
 * @param R The return type of the transaction.
 * @param body The suspending transaction block to execute.
 * @return A [Result] encapsulating either the successful return value or the caught exception.
 */
suspend fun <R> SuspendingTransacter.safeTransaction(
    body: suspend SuspendingTransactionWithReturn<R>.() -> R
): Result<R> = runSuspendCatching {
    transactionWithResult(bodyWithReturn = body)
}

/**
 * Transforms a SQLDelight [Query] into a reactive [Flow] of domain models.
 * Automates table observation, background cursor reading via [Dispatcher.IO], domain mapping, and emission optimization to prevent redundant downstream updates.
 *
 * @param Entity The auto-generated database schema class representing a table row.
 * @param Domain The target domain model class.
 * @param transform The mapping function converting an [Entity] to a [Domain] model.
 * @return A [Flow] emitting a mapped list of domain objects that updates automatically upon database changes.
 */
fun <Entity: Any, Domain> Query<Entity>.asFlow(transform: (Entity) -> Domain): Flow<List<Domain>> =
    asFlow()
        .mapToList(context = Dispatcher.IO)
        .map { list -> list.map(transform = transform) }
        .distinctUntilChanged()
        .flowOn(context = Dispatcher.Default)

/**
 * Lists all tables in the database, except the internal ones.
 *
 * @return The table names.
 */
suspend fun SqlDriver.getTableNames(): List<String> = object: Query<String>(mapper = { it.getString(index = 0).orEmpty() }) {
    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> = executeQuery(
        identifier = null,
        sql = "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'",
        mapper = mapper,
        parameters = 0
    )

    override fun addListener(listener: Listener) = Unit
    override fun removeListener(listener: Listener) = Unit
}.awaitAsList()
