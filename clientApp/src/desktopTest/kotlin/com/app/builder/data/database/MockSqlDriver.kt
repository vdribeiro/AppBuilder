package com.app.builder.data.database

import java.util.Properties
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

actual suspend fun createMockSqlDriver(
    name: String,
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>
): SqlDriver = JdbcSqliteDriver(
    url = JdbcSqliteDriver.IN_MEMORY,
    properties = Properties(),
    schema = schema.synchronous(),
).apply {
    execute(
        identifier = null,
        sql = "PRAGMA journal_mode=WAL;",
        parameters = 0,
        binders = null
    ).await()
}
