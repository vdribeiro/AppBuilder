package com.app.builder.data.database

import kotlinx.coroutines.withContext
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.app.builder.applicationContext
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.test.ExcludeFromTesting

@ExcludeFromTesting
actual suspend fun createSqlDriver(
    name: String,
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>
): SqlDriver = withContext(context = Dispatcher.IO) {
    val schema = schema.synchronous()
    AndroidSqliteDriver(
        schema = schema,
        context = applicationContext,
        name = name,
        callback = object: AndroidSqliteDriver.Callback(schema = schema) {
            override fun onConfigure(db: SupportSQLiteDatabase) {
                super.onConfigure(db = db)
                runCatching {
                    db.enableWriteAheadLogging()
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to enable WAL mode", throwable = it)
                }
                runCatching {
                    db.execSQL("PRAGMA busy_timeout=5000;")
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to set busy timeout", throwable = it)
                }
            }
        }
    )
}

private const val TAG = "SqlDriver"