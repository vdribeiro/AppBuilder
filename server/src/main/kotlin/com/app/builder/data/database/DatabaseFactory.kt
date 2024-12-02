package com.app.builder.data.database

import org.jetbrains.exposed.v1.core.vendors.DatabaseDialect
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import com.app.builder.core.platform.Env
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory

/** Factory responsible for configuring and initializing the reactive database connection. */
object DatabaseFactory {

    /**
     * Builds the in-memory H2 connection factory.
     * `DB_CLOSE_DELAY=-1` keeps the database alive for the whole lifetime instead of dropping it the moment the last connection closes.
     *
     * @return The in-memory [ConnectionFactory].
     */
    fun getInMemoryConnectionFactory(): ConnectionFactory = ConnectionFactories.get("r2dbc:h2:mem:///test;DB_CLOSE_DELAY=-1")

    /**
     * Builds the Postgres connection factory from the environment.
     *
     * @throws Throwable If any required database environment variable is missing.
     * @return The configured [PostgresqlConnectionFactory].
     */
    @Throws(Throwable::class)
    fun getPostgresConnectionFactory(): PostgresqlConnectionFactory {
        val host = Env.dbHost
        val port = Env.dbPort
        val name = Env.dbName
        val user = Env.dbUser
        val password = Env.dbPassword
        if (host.isNullOrBlank() ||
            port == null ||
            name.isNullOrBlank() ||
            user.isNullOrBlank() ||
            password.isNullOrBlank()
        ) error(message = "Invalid database parameters for configuration")

        return PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host(host)
                .port(port)
                .database(name)
                .username(user)
                .password(password)
                .build()
        )
    }

    /**
     * Configures and initializes the reactive database connection through the given factory, resolving the SQL dialect from the factory's own metadata.
     * Upon successful connection, it creates the necessary database schemas.
     *
     * @param connectionFactory The connection factory to connect through.
     * @return [R2dbcDatabase] The configured reactive database instance.
     */
    suspend fun create(connectionFactory: ConnectionFactory): R2dbcDatabase {
        val database = R2dbcDatabase.connect(
            connectionFactory = connectionFactory,
            databaseConfig = R2dbcDatabaseConfig.Builder().apply { explicitDialect = connectionFactory.dialect() }
        )
        database.create()
        return database
    }

    /**
     * Resolves the Exposed dialect for the driver behind this factory.
     *
     * @throws Throwable If the driver is not supported.
     * @return The matching [DatabaseDialect].
     */
    private fun ConnectionFactory.dialect(): DatabaseDialect = when (metadata.name) {
        "H2" -> H2Dialect()
        "PostgreSQL" -> PostgreSQLDialect()
        else -> error(message = "Unsupported database driver: ${metadata.name}")
    }
}
