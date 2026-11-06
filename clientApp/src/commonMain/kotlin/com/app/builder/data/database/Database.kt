package com.app.builder.data.database

import kotlinx.coroutines.withContext
import app.cash.sqldelight.db.SqlDriver
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import database.Account
import database.AppDatabase
import database.DeviceLocation
import database.Job
import database.Registry
import database.Session
import database.Task
import database.Translation
import database.User

// Aliases mapping generated database schema models to more convenient names.
typealias JobSchema = Job
typealias RegistrySchema = Registry
typealias TranslationSchema = Translation
typealias SessionSchema = Session
typealias AccountSchema = Account
typealias UserSchema = User
typealias DeviceLocationSchema = DeviceLocation
typealias TaskSchema = Task

/**
 * Drops every table in the database and creates them again from [AppDatabase.Schema], so a database left with an older schema is rebuilt too.
 * Observers of the dropped tables are notified, so their queries run again against the new tables.
 * Executes within an IO dispatcher and captures failures gracefully.
 */
suspend fun SqlDriver.reset() = withContext(context = Dispatcher.IO) {
    runCatching {
        val tables = getTableNames()
        tables.forEach { execute(identifier = null, sql = "DROP TABLE IF EXISTS \"$it\"", parameters = 0).await() }
        AppDatabase.Schema.create(driver = this@reset).await()
        notifyListeners(queryKeys = tables.toTypedArray())
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to recreate database", throwable = it)
    }.getOrDefault(defaultValue = Unit)
}

private const val TAG = "Database"
