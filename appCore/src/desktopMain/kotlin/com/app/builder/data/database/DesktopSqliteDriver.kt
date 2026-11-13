package com.app.builder.data.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.util.Properties
import kotlin.concurrent.getOrSet
import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.ConnectionManager.Transaction
import app.cash.sqldelight.driver.jdbc.JdbcDriver

/**
 * A SQLite JDBC driver that opens one connection per thread and begins every transaction as `IMMEDIATE`.
 * A transaction acquires the database write lock when it begins, waiting for any other writer to finish first.
 *
 * Adapted from `JdbcSqliteDriver` in SQLDelight 2.4.1 (Apache License 2.0), which begins transactions as deferred.
 *
 * > **NOTE:** Compare against the source when upgrading SQL libraries.
 *
 * @property url The database connection URL in the form of `jdbc:sqlite:path`.
 * @property properties The properties applied to every connection the driver opens.
 */
internal class DesktopSqliteDriver(
    private val url: String,
    private val properties: Properties = Properties()
): JdbcDriver() {
    /** The listeners registered for each query key. */
    private val listeners = linkedMapOf<String, MutableSet<Query.Listener>>()

    /** The transaction in progress on each thread. */
    private val transactions = ThreadLocal<Transaction>()

    /** The connection opened by each thread. */
    private val connections = ThreadLocal<Connection>()

    /** The transaction in progress on the current thread. Clearing it closes the connection of the finished transaction. */
    override var transaction: Transaction?
        get() = transactions.get()
        set(value) {
            val currentTransaction = transactions.get()
            transactions.set(value)
            if (value == null && currentTransaction != null) closeConnection(connection = currentTransaction.connection)
        }

    /**
     * Resolves the connection of the current thread, opening it if the thread has none.
     *
     * @return The connection bound to the current thread.
     */
    override fun getConnection(): Connection = connections.getOrSet { DriverManager.getConnection(url, properties) }

    /**
     * Closes the given [connection] unless a transaction is in progress on the current thread.
     *
     * @param connection The connection to close.
     * @throws IllegalStateException If the connection was opened by another thread.
     */
    override fun closeConnection(connection: Connection) {
        check(value = connections.get() == connection) { "Connections must be closed on the thread that opened them" }
        if (transaction == null) {
            connection.close()
            connections.remove()
        }
    }

    /** Begins a transaction that acquires the database write lock immediately. */
    override fun Connection.beginTransaction() {
        prepareStatement("BEGIN IMMEDIATE TRANSACTION").use(block = PreparedStatement::execute)
    }

    /** Commits the transaction in progress. */
    override fun Connection.endTransaction() {
        prepareStatement("END TRANSACTION").use(block = PreparedStatement::execute)
    }

    /** Rolls back the transaction in progress. */
    override fun Connection.rollbackTransaction() {
        prepareStatement("ROLLBACK TRANSACTION").use(block = PreparedStatement::execute)
    }

    override fun addListener(vararg queryKeys: String, listener: Query.Listener) {
        synchronized(lock = listeners) {
            queryKeys.forEach { listeners.getOrPut(key = it) { linkedSetOf() }.add(element = listener) }
        }
    }

    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {
        synchronized(lock = listeners) {
            queryKeys.forEach { listeners[it]?.remove(element = listener) }
        }
    }

    override fun notifyListeners(vararg queryKeys: String) {
        val listenersToNotify = linkedSetOf<Query.Listener>()
        synchronized(lock = listeners) {
            queryKeys.forEach { key -> listeners[key]?.let { listenersToNotify.addAll(elements = it) } }
        }
        listenersToNotify.forEach(action = Query.Listener::queryResultsChanged)
    }
}

/**
 * Creates a [DesktopSqliteDriver] and creates or migrates the given [schema] based on the `user_version` of the database.
 *
 * @param url The database connection URL in the form of `jdbc:sqlite:path`.
 * @param properties The properties applied to every connection the driver opens.
 * @param schema The SQLDelight schema used to create or migrate the database.
 * @return The driver, with the database at the version of [schema].
 */
internal fun DesktopSqliteDriver(
    url: String,
    properties: Properties = Properties(),
    schema: SqlSchema<QueryResult.Value<Unit>>
): DesktopSqliteDriver {
    val driver = DesktopSqliteDriver(url = url, properties = properties)
    val transacter = object: TransacterImpl(driver = driver) {}

    transacter.transaction {
        val version = driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version",
            mapper = { cursor: SqlCursor -> QueryResult.Value(value = if (cursor.next().value) cursor.getLong(index = 0) else null) },
            parameters = 0,
            binders = null
        ).value ?: 0L

        if (version == 0L) {
            schema.create(driver = driver).value
            driver.execute(identifier = null, sql = "PRAGMA user_version = ${schema.version}", parameters = 0, binders = null).value
        } else if (version < schema.version) {
            schema.migrate(driver = driver, oldVersion = version, newVersion = schema.version).value
            driver.execute(identifier = null, sql = "PRAGMA user_version = ${schema.version}", parameters = 0, binders = null).value
        }
    }

    return driver
}
