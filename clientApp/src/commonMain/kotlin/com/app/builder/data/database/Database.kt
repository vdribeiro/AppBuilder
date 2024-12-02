package com.app.builder.data.database

import kotlinx.coroutines.withContext
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import database.AppDatabase
import database.DeviceLocation
import database.Job
import database.Registry
import database.Session
import database.Task
import database.TaskUserJoin
import database.Translation
import database.User

// Aliases mapping generated database schema models to more convenient names.
typealias JobSchema = Job
typealias RegistrySchema = Registry
typealias TranslationSchema = Translation
typealias SessionSchema = Session
typealias UserSchema = User
typealias DeviceLocationSchema = DeviceLocation
typealias TaskSchema = Task
typealias TaskUserJoinSchema = TaskUserJoin

/**
 * Clears all persisted records across all database tables.
 * Executes within an IO dispatcher and captures transaction failures gracefully.
 */
suspend fun AppDatabase.reset() = withContext(context = Dispatcher.IO) {
    runCatching {
        transaction {
            jobQueries.truncateJob()
            registryQueries.truncateRegistry()
            translationQueries.truncateTranslation()
            sessionQueries.truncateSession()
            userQueries.truncateUser()
            deviceLocationQueries.truncateDeviceLocation()
            taskQueries.truncateTask()
            taskUserJoinQueries.truncateTaskUserJoin()
        }
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to clear database", throwable = it)
    }.getOrDefault(defaultValue = Unit)
}

private const val TAG = "Database"
