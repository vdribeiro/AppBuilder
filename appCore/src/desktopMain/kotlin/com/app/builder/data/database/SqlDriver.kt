package com.app.builder.data.database

import java.io.File
import java.util.Properties
import kotlinx.coroutines.withContext
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.flow.runSuspendCatching
import com.app.builder.core.platform.appDataPath
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.test.ExcludeFromTesting

@ExcludeFromTesting
actual suspend fun createSqlDriver(
    name: String,
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>
): SqlDriver = withContext(context = Dispatcher.IO) {
    DesktopSqliteDriver(
        url = "jdbc:sqlite:${File(appDataPath, name).absolutePath}",
        properties = Properties(),
        schema = schema.synchronous(),
    ).apply {
        runSuspendCatching {
            execute(
                identifier = null,
                sql = "PRAGMA journal_mode=WAL;",
                parameters = 0,
                binders = null
            ).await()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to enable WAL mode", throwable = it)
        }
    }
}

private const val TAG = "SqlDriver"
