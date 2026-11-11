package com.app.builder.domain.usecase.building

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.database.reset
import com.app.builder.domain.usecase.Outcome
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class BuildingUseCasesTest: TestCase() {

    /** Verifies that upserting a building makes it retrievable and triggers a notification for the users that can read buildings. */
    @Test
    fun crudBuildings() = runServerTest {
        val buildingUseCases = dependency.get()
            .useCases
            .buildingUseCases
        val notificationsUseCases = dependency.get()
            .useCases
            .notificationsUseCases
        val lastSyncUtc = now() - 1.days

        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isEmpty())
        assertTrue(actual = buildingUseCases.getBuildings(lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData().isEmpty())
        assertEquals(expected = Outcome.Success(data = Unit), actual = buildingUseCases.upsertBuilding(building = FakeData.building))
        assertEquals(expected = listOf(element = FakeData.building), actual = buildingUseCases.getBuildings(lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData())
        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isNotEmpty())
    }

    /** Verifies that fetching a single building returns it, and null for an unknown uuid. */
    @Test
    fun getBuilding() = runServerTest {
        val buildingUseCases = dependency.get()
            .useCases
            .buildingUseCases

        assertEquals(expected = Outcome.Success(data = Unit), actual = buildingUseCases.upsertBuilding(building = FakeData.building))
        assertEquals(expected = FakeData.building, actual = buildingUseCases.getBuilding(uuid = FakeData.building.uuid).successData())
        assertEquals(expected = Outcome.NotFound, actual = buildingUseCases.getBuilding(uuid = uuid()))
    }

    /** Verifies that when the database fails, its reads and writes report [Outcome.Error] rather than an empty list, a missing building or a refused write. */
    @Test
    fun databaseFailureIsAnError() = runServerTest {
        val buildingUseCases = dependency.get()
            .useCases
            .buildingUseCases
        dependency.get().database.reset()

        listOf(
            "getBuildings" to buildingUseCases.getBuildings(lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0), cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize),
            "getBuilding" to buildingUseCases.getBuilding(uuid = FakeData.building.uuid),
            "upsertBuilding" to buildingUseCases.upsertBuilding(building = FakeData.building)
        ).forEach { (name, outcome) -> assertEquals(expected = Outcome.Error, actual = outcome, message = name) }
    }
}
