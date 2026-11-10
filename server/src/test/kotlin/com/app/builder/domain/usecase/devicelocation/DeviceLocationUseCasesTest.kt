package com.app.builder.domain.usecase.devicelocation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.database.reset
import com.app.builder.domain.usecase.Outcome
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class DeviceLocationUseCasesTest: TestCase() {

    /** Verifies that a device location fix can be upserted and retrieved. */
    @Test
    fun crudDeviceLocations() = runServerTest {
        val deviceLocationUseCases = dependency.get()
            .useCases
            .deviceLocationUseCases
        val lastSyncUtc = now() - 1.days

        assertTrue(
            actual = deviceLocationUseCases.getDeviceLocations(
                userUuid = FakeData.adminUser.uuid,
                lastSyncUtc = lastSyncUtc,
                cursorUtc = null,
                cursorUuid = null,
                pageSize = ServerConfigs.value.pageSize
            ).successData().isEmpty()
        )
        assertEquals(expected = Outcome.Success(data = Unit), actual = deviceLocationUseCases.upsertDeviceLocation(userUuid = FakeData.adminUser.uuid, deviceLocation = FakeData.deviceLocation))
        assertEquals(
            expected = listOf(FakeData.deviceLocation),
            actual = deviceLocationUseCases.getDeviceLocations(
                userUuid = FakeData.adminUser.uuid,
                lastSyncUtc = lastSyncUtc,
                cursorUtc = null,
                cursorUuid = null,
                pageSize = ServerConfigs.value.pageSize
            ).successData()
        )
    }

    /** Verifies that fetching device locations filters by last sync time and paginates using the cursor. */
    @Test
    fun getDeviceLocationsSinceLastSyncAndPaginated() = runServerTest {
        val deviceLocationUseCases = dependency.get()
            .useCases
            .deviceLocationUseCases

        val now = now()
        val old = FakeData.deviceLocation.copy(uuid = uuid(), deviceTime = now - 10.minutes)
        val recent = FakeData.deviceLocation.copy(uuid = uuid(), deviceTime = now)

        assertEquals(expected = Outcome.Success(data = Unit), actual = deviceLocationUseCases.upsertDeviceLocation(userUuid = FakeData.adminUser.uuid, deviceLocation = old))
        assertEquals(expected = Outcome.Success(data = Unit), actual = deviceLocationUseCases.upsertDeviceLocation(userUuid = FakeData.adminUser.uuid, deviceLocation = recent))

        assertEquals(
            expected = listOf(recent),
            actual = deviceLocationUseCases.getDeviceLocations(userUuid = FakeData.adminUser.uuid, lastSyncUtc = now - 1.minutes, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData()
        )
        assertTrue(
            actual = deviceLocationUseCases.getDeviceLocations(userUuid = FakeData.adminUser.uuid, lastSyncUtc = now + 1.minutes, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData().isEmpty()
        )

        val firstPage = deviceLocationUseCases.getDeviceLocations(
            userUuid = FakeData.adminUser.uuid,
            lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0),
            cursorUtc = null,
            cursorUuid = null,
            pageSize = 1
        ).successData()
        assertEquals(expected = listOf(recent), actual = firstPage)

        val secondPage = deviceLocationUseCases.getDeviceLocations(
            userUuid = FakeData.adminUser.uuid,
            lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0),
            cursorUtc = firstPage.last().deviceTime,
            cursorUuid = firstPage.last().uuid,
            pageSize = 1
        ).successData()
        assertEquals(expected = listOf(old), actual = secondPage)
    }

    /** Verifies that a user cannot see device locations reported by another user. */
    @Test
    fun getDeviceLocationsIsScopedToUser() = runServerTest {
        val deviceLocationUseCases = dependency.get()
            .useCases
            .deviceLocationUseCases

        assertEquals(expected = Outcome.Success(data = Unit), actual = deviceLocationUseCases.upsertDeviceLocation(userUuid = FakeData.adminUser.uuid, deviceLocation = FakeData.deviceLocation))
        assertTrue(
            actual = deviceLocationUseCases.getDeviceLocations(
                userUuid = FakeData.user.uuid,
                lastSyncUtc = now() - 1.days,
                cursorUtc = null,
                cursorUuid = null,
                pageSize = ServerConfigs.value.pageSize
            ).successData().isEmpty()
        )
    }

    /** Verifies that when the database fails, its read and write report [Outcome.Error] rather than an empty list or a refused write. */
    @Test
    fun databaseFailureIsAnError() = runServerTest {
        val deviceLocationUseCases = dependency.get()
            .useCases
            .deviceLocationUseCases
        dependency.get().database.reset()

        listOf(
            "getDeviceLocations" to deviceLocationUseCases.getDeviceLocations(userUuid = FakeData.adminUser.uuid, lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0), cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize),
            "upsertDeviceLocation" to deviceLocationUseCases.upsertDeviceLocation(userUuid = FakeData.adminUser.uuid, deviceLocation = FakeData.deviceLocation)
        ).forEach { (name, outcome) -> assertEquals(expected = Outcome.Error, actual = outcome, message = name) }
    }
}
