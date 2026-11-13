package com.app.builder.data.database

import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.app.builder.test.TestCase

/** Desktop-specific coverage for [DesktopSqliteDriver], which only exists on this target. */
class DesktopSqliteDriverTest: TestCase() {

    /** Verifies transactions that read and then write on several threads at once all commit. */
    @Test
    fun concurrentReadThenWriteTransactionsAllCommit() = runUnitTest {
        withDatabaseFile { url ->
            val driver = DesktopSqliteDriver(url = url, schema = TestSchema())
            driver.execute(identifier = null, sql = "PRAGMA journal_mode=WAL;", parameters = 0, binders = null)
            val transacter = object: TransacterImpl(driver = driver) {}
            val failures = CopyOnWriteArrayList<Throwable>()

            (1..THREADS).map { index ->
                thread {
                    repeat(times = TRANSACTIONS_PER_THREAD) {
                        runCatching {
                            transacter.transaction {
                                driver.queryLong(sql = "SELECT COUNT(*) FROM item")
                                Thread.sleep(1)
                                driver.execute(identifier = null, sql = "INSERT INTO item (value) VALUES ($index)", parameters = 0, binders = null)
                            }
                        }.onFailure { failures.add(it) }
                    }
                }
            }.forEach { it.join() }

            assertTrue(actual = failures.isEmpty(), message = failures.firstOrNull()?.message)
            assertEquals(expected = (THREADS * TRANSACTIONS_PER_THREAD).toLong(), actual = driver.queryLong(sql = "SELECT COUNT(*) FROM item"))
        }
    }

    /** Verifies a transaction that fails is rolled back and releases the write lock for a transaction on another thread. */
    @Test
    fun failedTransactionRollsBackAndReleasesWriteLock() = runUnitTest {
        withDatabaseFile { url ->
            val driver = DesktopSqliteDriver(url = url, schema = TestSchema())
            val transacter = object: TransacterImpl(driver = driver) {}

            assertFailsWith<IllegalStateException> {
                transacter.transaction {
                    driver.execute(identifier = null, sql = "INSERT INTO item (value) VALUES (1)", parameters = 0, binders = null)
                    error(message = "Failure inside the transaction")
                }
            }
            assertEquals(expected = 0L, actual = driver.queryLong(sql = "SELECT COUNT(*) FROM item"))

            thread {
                transacter.transaction {
                    driver.execute(identifier = null, sql = "INSERT INTO item (value) VALUES (2)", parameters = 0, binders = null)
                }
            }.join()
            assertEquals(expected = 1L, actual = driver.queryLong(sql = "SELECT COUNT(*) FROM item"))
        }
    }

    /** Verifies the schema is created once and the database is stamped with the schema version. */
    @Test
    fun createsSchemaOnceAndSetsUserVersion() = runUnitTest {
        withDatabaseFile { url ->
            val schema = TestSchema()

            val driver = DesktopSqliteDriver(url = url, schema = schema)
            assertEquals(expected = 1, actual = schema.created)
            assertEquals(expected = schema.version, actual = driver.queryLong(sql = "PRAGMA user_version"))

            DesktopSqliteDriver(url = url, schema = schema)
            assertEquals(expected = 1, actual = schema.created)
            assertEquals(expected = 0, actual = schema.migrated)
        }
    }

    /** Verifies a database at an older version is migrated and stamped with the new schema version. */
    @Test
    fun migratesSchemaWhenDatabaseVersionIsOlder() = runUnitTest {
        withDatabaseFile { url ->
            DesktopSqliteDriver(url = url, schema = TestSchema())
            val schema = TestSchema(version = 2)

            val driver = DesktopSqliteDriver(url = url, schema = schema)

            assertEquals(expected = 0, actual = schema.created)
            assertEquals(expected = 1, actual = schema.migrated)
            assertEquals(expected = 2L, actual = driver.queryLong(sql = "PRAGMA user_version"))
        }
    }

    /**
     * Runs the given [block] with the JDBC URL of a database file that is deleted afterwards.
     *
     * @param block The block receiving the JDBC URL of the database file.
     */
    private fun withDatabaseFile(block: (String) -> Unit) {
        val file = File.createTempFile("desktop-sqlite-driver", ".db").also { it.delete() }
        try {
            block("jdbc:sqlite:${file.absolutePath}")
        } finally {
            listOf("", "-wal", "-shm").forEach { File(file.absolutePath + it).delete() }
        }
    }

    /**
     * Executes the given [sql] and reads the first column of the first row.
     *
     * @param sql The query to execute.
     * @return The value read, or `null` if the query returns no rows.
     */
    private fun SqlDriver.queryLong(sql: String): Long? = executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor -> QueryResult.Value(value = if (cursor.next().value) cursor.getLong(index = 0) else null) },
        parameters = 0,
        binders = null
    ).value

    /**
     * A schema with a single table that counts how many times it is created and migrated.
     *
     * @property version The version of the schema.
     */
    private class TestSchema(override val version: Long = 1): SqlSchema<QueryResult.Value<Unit>> {
        /** The number of times the schema was created. */
        var created = 0

        /** The number of times the schema was migrated. */
        var migrated = 0

        override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
            created++
            driver.execute(identifier = null, sql = "CREATE TABLE item (id INTEGER PRIMARY KEY AUTOINCREMENT, value INTEGER NOT NULL)", parameters = 0, binders = null)
            return QueryResult.Unit
        }

        override fun migrate(driver: SqlDriver, oldVersion: Long, newVersion: Long, vararg callbacks: AfterVersion): QueryResult.Value<Unit> {
            migrated++
            return QueryResult.Unit
        }
    }

    private companion object {
        private const val THREADS = 4
        private const val TRANSACTIONS_PER_THREAD = 25
    }
}
