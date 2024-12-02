package com.app.builder.data.database

import kotlinx.coroutines.withContext
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.app.builder.core.flow.Dispatcher
import org.w3c.dom.Worker

actual suspend fun createMockSqlDriver(
    name: String,
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>
): SqlDriver = withContext(context = Dispatcher.IO) {
    WebWorkerDriver(worker = getWorker()).also { driver ->
        schema.create(driver = driver).await()
    }
}

/** Creates the web worker running the SQL.js SQLDelight driver. */
@JsFun(code = """() => new Worker(new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url))""")
private external fun getWorker(): Worker
