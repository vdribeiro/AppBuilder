package com.app.builder.data.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.JournalMode

actual suspend fun createMockSqlDriver(
    name: String,
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>
): SqlDriver = NativeSqliteDriver(
    schema = schema.synchronous(),
    name = name,
    onConfiguration = { config ->
        config.copy(
            inMemory = true,
            journalMode = JournalMode.WAL
        )
    }
)
