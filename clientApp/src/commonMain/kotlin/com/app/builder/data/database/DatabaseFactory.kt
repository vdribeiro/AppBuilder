package com.app.builder.data.database

import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.app.builder.data.database.adapter.InstantColumnAdapter
import com.app.builder.data.database.adapter.UuidColumnAdapter
import database.AppDatabase
import database.DeviceLocation
import database.Job
import database.Registry
import database.Session
import database.Task
import database.TaskUserJoin
import database.User

/**
 * Factory that initializes the [AppDatabase] with standardized column adapters, ensuring custom types are correctly mapped to SQL structures across all target platforms.
 *
 * @param driver The platform-specific SQL driver managing the database connection.
 */
class DatabaseFactory(driver: SqlDriver) {

    /** The initialized [AppDatabase] instance configured with required type adapters. */
    val database: AppDatabase = AppDatabase(
        driver = driver,
        JobAdapter = Job.Adapter(
            uuidAdapter = UuidColumnAdapter,
            userUuidAdapter = UuidColumnAdapter,
            utcAdapter = InstantColumnAdapter,
            entityUuidAdapter = UuidColumnAdapter,
            entityTypeAdapter = EnumColumnAdapter(),
            typeAdapter = EnumColumnAdapter(),
            conflictPolicyAdapter = EnumColumnAdapter(),
            stateAdapter = EnumColumnAdapter(),
            attemptAdapter = IntColumnAdapter
        ),
        RegistryAdapter = Registry.Adapter(
            requestUuidAdapter = UuidColumnAdapter,
            requestUtcAdapter = InstantColumnAdapter,
            requestSentUtcAdapter = InstantColumnAdapter,
            deletedAtAdapter = InstantColumnAdapter,
            userUuidAdapter = UuidColumnAdapter,
            deviceUuidAdapter = UuidColumnAdapter,
            entityUuidAdapter = UuidColumnAdapter,
            entityTypeAdapter = EnumColumnAdapter()
        ),
        DeviceLocationAdapter = DeviceLocation.Adapter(
            uuidAdapter = UuidColumnAdapter,
            userUuidAdapter = UuidColumnAdapter,
            fixTimeAdapter = InstantColumnAdapter,
            deviceTimeAdapter = InstantColumnAdapter
        ),
        SessionAdapter = Session.Adapter(
            userUuidAdapter = UuidColumnAdapter,
        ),
        TaskAdapter = Task.Adapter(
            uuidAdapter = UuidColumnAdapter,
            modifiedAtAdapter = InstantColumnAdapter,
            deletedAtAdapter = InstantColumnAdapter,
            stateAdapter = EnumColumnAdapter()
        ),
        TaskUserJoinAdapter = TaskUserJoin.Adapter(
            taskUuidAdapter = UuidColumnAdapter,
            userUuidAdapter = UuidColumnAdapter,
        ),
        UserAdapter = User.Adapter(
            uuidAdapter = UuidColumnAdapter,
            modifiedAtAdapter = InstantColumnAdapter,
            deletedAtAdapter = InstantColumnAdapter,
        )
    )

    companion object {
        /** The default file name for the local database storage. */
        const val DATABASE_FILE = "app.db"
    }
}
