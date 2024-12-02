package com.app.builder.data.database

import kotlin.test.Test
import kotlin.test.assertFailsWith
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryMetadata
import org.reactivestreams.Publisher
import com.app.builder.test.TestCase

class DatabaseFactoryTest: TestCase() {

    /** Verifies that the in-memory connection factory resolves against the H2 dialect and connects without requiring a live Postgres connection. */
    @Test
    fun createResolvesH2DialectForInMemoryFactory() = runServerTest {
        DatabaseFactory.create(connectionFactory = DatabaseFactory.getInMemoryConnectionFactory())
    }

    /** Verifies that resolving the dialect for an unrecognized driver throws before any connection is attempted. */
    @Test
    fun createThrowsForUnsupportedDriver() = runServerTest {
        val unsupportedConnectionFactory = object: ConnectionFactory {
            override fun create(): Publisher<out Connection> = error(message = "Should not be called: dialect resolution must fail first")
            override fun getMetadata(): ConnectionFactoryMetadata = object: ConnectionFactoryMetadata {
                override fun getName(): String = "SQLite"
            }
        }

        assertFailsWith<IllegalStateException> { DatabaseFactory.create(connectionFactory = unsupportedConnectionFactory) }
    }
}
