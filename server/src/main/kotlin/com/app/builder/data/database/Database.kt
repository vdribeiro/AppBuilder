package com.app.builder.data.database

import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.transactionManager
import com.app.builder.data.database.table.ConfigTable
import com.app.builder.data.database.table.ConnectionTable
import com.app.builder.data.database.table.DeviceLocationTable
import com.app.builder.data.database.table.DeviceTokenTable
import com.app.builder.data.database.table.NotificationTable
import com.app.builder.data.database.table.RegistryTable
import com.app.builder.data.database.table.TaskTable
import com.app.builder.data.database.table.TaskUserJoinTable
import com.app.builder.data.database.table.TicketTable
import com.app.builder.data.database.table.UserSecretTable
import com.app.builder.data.database.table.UserSessionTable
import com.app.builder.data.database.table.UserTable
import io.r2dbc.spi.IsolationLevel

/** A list of all the tables in the database. */
private val tables = listOf(
    ConfigTable,
    UserTable,
    UserSecretTable,
    UserSessionTable,
    RegistryTable,
    DeviceTokenTable,
    DeviceLocationTable,
    TicketTable,
    ConnectionTable,
    NotificationTable,
    TaskTable,
    TaskUserJoinTable,
).toTypedArray()

/** Creates all the tables in the database. */
suspend fun R2dbcDatabase.create() {
    safeTransaction { SchemaUtils.create(tables = tables) }
}

/** Drops all the tables in the database. */
suspend fun R2dbcDatabase.reset() {
    safeTransaction { SchemaUtils.drop(tables = tables) }
}

/**
 * Safely executes a database transaction, catching any exceptions and returning a [Result].
 * If the transaction completes successfully, it returns [Result.success] with the result of [statement].
 * If an exception is thrown inside [statement], the database will automatically roll back the transaction, and this function will catch the bubbled-up exception, returning it as a [Result.failure].
 *
 * @param T The return type of the database operation.
 * @param transactionIsolation The isolation level for the transaction. Defaults to the transaction manager's default level.
 * @param readOnly Whether the transaction should be optimized for read-only access. Defaults to the transaction manager's default setting.
 * @param statement A suspendable lambda executed within the context of an [R2dbcTransaction].
 * @return A [Result] encapsulating either the successful result of the [statement], or the caught exception.
 * @see R2dbcTransaction
 * @see suspendTransaction
 */
suspend fun <T> R2dbcDatabase.safeTransaction(
    transactionIsolation: IsolationLevel? = transactionManager.defaultIsolationLevel,
    readOnly: Boolean? = transactionManager.defaultReadOnly,
    statement: suspend R2dbcTransaction.() -> T
): Result<T> = runCatching {
    suspendTransaction(
        db = this@safeTransaction,
        transactionIsolation = transactionIsolation,
        readOnly = readOnly,
        statement = statement
    )
}
