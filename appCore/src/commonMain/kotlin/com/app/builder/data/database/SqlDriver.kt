package com.app.builder.data.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.app.builder.test.ExcludeFromTesting

/**
 * Creates a platform-specific [SqlDriver] instance.
 * Supports asynchronous initialization required by async-enabled platform drivers.
 *
 * @param name The name of the database file.
 * @param schema The SQLDelight schema used to create or migrate the database.
 * @return A configured [SqlDriver] ready for use.
 */
@ExcludeFromTesting
expect suspend fun createSqlDriver(
    name: String,
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>
): SqlDriver
