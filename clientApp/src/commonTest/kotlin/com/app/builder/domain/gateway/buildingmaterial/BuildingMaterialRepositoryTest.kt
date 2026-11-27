package com.app.builder.domain.gateway.buildingmaterial

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.data.serializer.encode
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class BuildingMaterialRepositoryTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(http = true) }
    }

    /** Verifies that executing a building materials sync job succeeds, records the last sync timestamp, and stores the building materials. */
    @Test
    fun executeSyncBuildingMaterials() = runUnitTest {
        val graph = dependency.get().getUserDependency(account = FakeData.adminAccount)
        graph.useCases.materialUseCases.upsertMaterial(material = FakeData.material)
        assertNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.BUILDING_MATERIALS_LAST_SYNC_UTC])

        val jobResult = graph.repositories.buildingMaterialRepository.executeSyncBuildingMaterials(job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING_MATERIAL, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Success)

        assertNotNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.BUILDING_MATERIALS_LAST_SYNC_UTC])
        assertEquals(expected = listOf(element = MaterialInBuilding(material = FakeData.material, quantity = FakeData.buildingMaterial.quantity)), actual = graph.useCases.buildingMaterialUseCases.observeMaterialsInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull())
    }

    /** Verifies that executing a single building material sync job stores it. */
    @Test
    fun executeSyncBuildingMaterial() = runUnitTest {
        val graph = dependency.get().getUserDependency(account = FakeData.adminAccount)
        graph.useCases.buildingUseCases.upsertBuilding(building = FakeData.building)

        val job = Job(userUuid = Uuid.NIL, entityUuid = FakeData.buildingMaterial.uuid, entityType = EntityType.BUILDING_MATERIAL, type = Job.Type.GET)
        assertTrue(actual = graph.repositories.buildingMaterialRepository.executeSyncBuildingMaterial(job = job) is JobResult.Success)

        assertEquals(expected = listOf(element = BuildingOfMaterial(building = FakeData.building, quantity = FakeData.buildingMaterial.quantity)), actual = graph.useCases.buildingMaterialUseCases.observeBuildingsOfMaterial(materialUuid = FakeData.material.uuid).firstOrNull())
    }

    /** Verifies that executing an upsert building material job fails without a payload and succeeds once the building material is encoded into it. */
    @Test
    fun executeUpsertBuildingMaterial() = runUnitTest {
        val graph = dependency.get().getUserDependency(account = FakeData.adminAccount)
        val buildingMaterialRepository = graph.repositories.buildingMaterialRepository
        graph.useCases.materialUseCases.upsertMaterial(material = FakeData.material)

        val buildingMaterial = FakeData.buildingMaterial.copy(quantity = FakeData.buildingMaterial.quantity + 1)
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING_MATERIAL, type = Job.Type.POST)
        assertTrue(actual = buildingMaterialRepository.executeUpsertBuildingMaterial(job = job) is JobResult.Error)

        val jobResult = buildingMaterialRepository.executeUpsertBuildingMaterial(job = job.copy(payload = encode(value = buildingMaterial)))
        assertTrue(actual = jobResult is JobResult.Success)
        assertEquals(expected = listOf(element = MaterialInBuilding(material = FakeData.material, quantity = buildingMaterial.quantity)), actual = graph.useCases.buildingMaterialUseCases.observeMaterialsInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull())
    }

    /** Verifies that executing a building materials sync job fails when the http client flag is disabled. */
    @Test
    fun executeSyncBuildingMaterialsOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val buildingMaterialRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .buildingMaterialRepository

        val jobResult = buildingMaterialRepository.executeSyncBuildingMaterials(job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING_MATERIAL, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** Verifies that an account without building material permission is refused every building material job. */
    @Test
    fun unauthorizedJobsAreRefused() = runUnitTest {
        val buildingMaterialRepository = dependency.get()
            .getUserDependency(account = FakeData.account.copy(permissions = mapOf(EntityType.MATERIAL to Permission.WRITE)))
            .repositories
            .buildingMaterialRepository

        listOf(
            buildingMaterialRepository.executeSyncBuildingMaterials(job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING_MATERIAL, type = Job.Type.GET)),
            buildingMaterialRepository.executeSyncBuildingMaterial(job = Job(userUuid = Uuid.NIL, entityUuid = FakeData.buildingMaterial.uuid, entityType = EntityType.BUILDING_MATERIAL, type = Job.Type.GET)),
            buildingMaterialRepository.executeUpsertBuildingMaterial(job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING_MATERIAL, type = Job.Type.POST, payload = encode(value = FakeData.buildingMaterial)))
        ).forEach { assertTrue(actual = it is JobResult.Error) }
    }
}
