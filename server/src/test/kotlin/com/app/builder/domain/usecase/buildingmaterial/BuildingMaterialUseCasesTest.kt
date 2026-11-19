package com.app.builder.domain.usecase.buildingmaterial

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.database.reset
import com.app.builder.domain.usecase.Outcome
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class BuildingMaterialUseCasesTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ServerFlags.set { it.copy(push = true) }
    }

    /** Verifies that upserting a building material makes it retrievable and triggers a notification for the users that can read building materials. */
    @Test
    fun crudBuildingMaterials() = runServerTest {
        val buildingMaterialUseCases = dependency.get()
            .useCases
            .buildingMaterialUseCases
        val notificationsUseCases = dependency.get()
            .useCases
            .notificationsUseCases
        val lastSyncUtc = now() - 1.days

        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isEmpty())
        assertTrue(actual = buildingMaterialUseCases.getBuildingMaterials(lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData().isEmpty())
        assertEquals(expected = Outcome.Success(data = Unit), actual = buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial))
        assertEquals(
            expected = listOf(element = FakeData.buildingMaterial),
            actual = buildingMaterialUseCases.getBuildingMaterials(lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData()
        )
        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isNotEmpty())
    }

    /** Verifies that deleting and relinking the same pair keeps a single building material, with the latest change. */
    @Test
    fun relinkingReusesTheLink() = runServerTest {
        val buildingMaterialUseCases = dependency.get()
            .useCases
            .buildingMaterialUseCases
        val link = FakeData.buildingMaterial

        buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = link)
        buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = link.copy(modifiedAt = link.modifiedAt + 1.seconds, deletedAt = link.modifiedAt + 1.seconds))
        val relinked = link.copy(modifiedAt = link.modifiedAt + 2.seconds)
        buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = relinked)

        val links = buildingMaterialUseCases.getBuildingMaterials(lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0), cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData()
        assertEquals(expected = listOf(element = relinked), actual = links)
    }

    /** Verifies that fetching a single building material returns it, and not found for an unknown uuid. */
    @Test
    fun getBuildingMaterial() = runServerTest {
        val buildingMaterialUseCases = dependency.get()
            .useCases
            .buildingMaterialUseCases

        assertEquals(expected = Outcome.Success(data = Unit), actual = buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial))
        assertEquals(expected = FakeData.buildingMaterial, actual = buildingMaterialUseCases.getBuildingMaterial(uuid = FakeData.buildingMaterial.uuid).successData())
        assertEquals(expected = Outcome.NotFound, actual = buildingMaterialUseCases.getBuildingMaterial(uuid = uuid()))
    }

    /** Verifies that when the database fails, its reads and writes report [Outcome.Error]. */
    @Test
    fun databaseFailureIsAnError() = runServerTest {
        val buildingMaterialUseCases = dependency.get()
            .useCases
            .buildingMaterialUseCases
        dependency.get().database.reset()

        listOf(
            "getBuildingMaterials" to buildingMaterialUseCases.getBuildingMaterials(lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0), cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize),
            "getBuildingMaterial" to buildingMaterialUseCases.getBuildingMaterial(uuid = FakeData.buildingMaterial.uuid),
            "upsertBuildingMaterial" to buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial)
        ).forEach { (name, outcome) -> assertEquals(expected = Outcome.Error, actual = outcome, message = name) }
    }
}
