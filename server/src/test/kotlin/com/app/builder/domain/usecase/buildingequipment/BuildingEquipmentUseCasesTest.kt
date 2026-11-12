package com.app.builder.domain.usecase.buildingequipment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.database.reset
import com.app.builder.domain.usecase.Outcome
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class BuildingEquipmentUseCasesTest: TestCase() {

    /** Verifies that upserting a building equipment makes it retrievable and triggers a notification for the users that can read building equipments. */
    @Test
    fun crudBuildingEquipments() = runServerTest {
        val buildingEquipmentUseCases = dependency.get()
            .useCases
            .buildingEquipmentUseCases
        val notificationsUseCases = dependency.get()
            .useCases
            .notificationsUseCases
        val lastSyncUtc = now() - 1.days

        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isEmpty())
        assertTrue(actual = buildingEquipmentUseCases.getBuildingEquipments(lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData().isEmpty())
        assertEquals(expected = Outcome.Success(data = Unit), actual = buildingEquipmentUseCases.upsertBuildingEquipment(buildingEquipment = FakeData.buildingEquipment))
        assertEquals(
            expected = listOf(element = FakeData.buildingEquipment),
            actual = buildingEquipmentUseCases.getBuildingEquipments(lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData()
        )
        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isNotEmpty())
    }

    /** Verifies that deleting and relinking the same pair keeps a single building equipment, with the latest change. */
    @Test
    fun relinkingReusesTheLink() = runServerTest {
        val buildingEquipmentUseCases = dependency.get()
            .useCases
            .buildingEquipmentUseCases
        val link = FakeData.buildingEquipment

        buildingEquipmentUseCases.upsertBuildingEquipment(buildingEquipment = link)
        buildingEquipmentUseCases.upsertBuildingEquipment(buildingEquipment = link.copy(modifiedAt = link.modifiedAt + 1.seconds, deletedAt = link.modifiedAt + 1.seconds))
        val relinked = link.copy(modifiedAt = link.modifiedAt + 2.seconds)
        buildingEquipmentUseCases.upsertBuildingEquipment(buildingEquipment = relinked)

        val links = buildingEquipmentUseCases.getBuildingEquipments(lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0), cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData()
        assertEquals(expected = listOf(element = relinked), actual = links)
    }

    /** Verifies that fetching a single building equipment returns it, and not found for an unknown uuid. */
    @Test
    fun getBuildingEquipment() = runServerTest {
        val buildingEquipmentUseCases = dependency.get()
            .useCases
            .buildingEquipmentUseCases

        assertEquals(expected = Outcome.Success(data = Unit), actual = buildingEquipmentUseCases.upsertBuildingEquipment(buildingEquipment = FakeData.buildingEquipment))
        assertEquals(expected = FakeData.buildingEquipment, actual = buildingEquipmentUseCases.getBuildingEquipment(uuid = FakeData.buildingEquipment.uuid).successData())
        assertEquals(expected = Outcome.NotFound, actual = buildingEquipmentUseCases.getBuildingEquipment(uuid = uuid()))
    }

    /** Verifies that when the database fails, its reads and writes report [Outcome.Error]. */
    @Test
    fun databaseFailureIsAnError() = runServerTest {
        val buildingEquipmentUseCases = dependency.get()
            .useCases
            .buildingEquipmentUseCases
        dependency.get().database.reset()

        listOf(
            "getBuildingEquipments" to buildingEquipmentUseCases.getBuildingEquipments(lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0), cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize),
            "getBuildingEquipment" to buildingEquipmentUseCases.getBuildingEquipment(uuid = FakeData.buildingEquipment.uuid),
            "upsertBuildingEquipment" to buildingEquipmentUseCases.upsertBuildingEquipment(buildingEquipment = FakeData.buildingEquipment)
        ).forEach { (name, outcome) -> assertEquals(expected = Outcome.Error, actual = outcome, message = name) }
    }
}
