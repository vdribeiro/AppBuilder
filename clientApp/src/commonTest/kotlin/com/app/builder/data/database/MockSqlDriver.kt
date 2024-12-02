package com.app.builder.data.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.app.builder.data.database.DatabaseFactory.Companion.DATABASE_FILE
import database.AppDatabase

/**
 * Creates a platform-specific [SqlDriver] instance for test cases.
 *
 * @param name The name of the database file.
 * @param schema The SQLDelight schema used to create or migrate the database.
 * @return A configured [SqlDriver] ready for use.
 */
expect suspend fun createMockSqlDriver(
    name: String = DATABASE_FILE,
    schema: SqlSchema<QueryResult.AsyncValue<Unit>> = AppDatabase.Schema
): SqlDriver
