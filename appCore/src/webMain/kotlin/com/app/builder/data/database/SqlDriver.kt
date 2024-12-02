package com.app.builder.data.database

import kotlinx.coroutines.withContext
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.test.ExcludeFromTesting
import org.w3c.dom.Worker

@ExcludeFromTesting
actual suspend fun createSqlDriver(
    name: String,
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>
): SqlDriver = withContext(context = Dispatcher.IO) {
    WebWorkerDriver(worker = getWorker()).also { driver ->
        runCatching {
            schema.create(driver = driver).await()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to create database schema", throwable = it)
        }
    }
}

/**
 * Creates the Web Worker that hosts the SQL.js database engine off the main thread.
 *
 * @return A new [Worker] running the SQLDelight sql.js worker script.
 */
@JsFun(code = """() => new Worker(new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url))""")
private external fun getWorker(): Worker

private const val TAG = "SqlDriver"
