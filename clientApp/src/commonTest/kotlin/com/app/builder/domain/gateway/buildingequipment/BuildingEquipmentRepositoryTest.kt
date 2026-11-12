package com.app.builder.domain.gateway.buildingequipment

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

class BuildingEquipmentRepositoryTest: TestCase() {

    /** Verifies that executing a building equipments sync job succeeds, records the last sync timestamp, and stores the building equipments. */
    @Test
    fun executeSyncBuildingEquipments() = runUnitTest {
        val graph = dependency.get().getUserDependency(account = FakeData.adminAccount)
        graph.useCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)
        assertNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.BUILDING_EQUIPMENTS_LAST_SYNC_UTC])

        val jobResult = graph.repositories.buildingEquipmentRepository.executeSyncBuildingEquipments(job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING_EQUIPMENT, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Success)

        assertNotNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.BUILDING_EQUIPMENTS_LAST_SYNC_UTC])
        assertEquals(expected = listOf(element = FakeData.equipment), actual = graph.useCases.buildingEquipmentUseCases.observeEquipmentsInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull())
    }

    /** Verifies that executing a single building equipment sync job stores it. */
    @Test
    fun executeSyncBuildingEquipment() = runUnitTest {
        val graph = dependency.get().getUserDependency(account = FakeData.adminAccount)
        graph.useCases.buildingUseCases.upsertBuilding(building = FakeData.building)

        val job = Job(userUuid = Uuid.NIL, entityUuid = FakeData.buildingEquipment.uuid, entityType = EntityType.BUILDING_EQUIPMENT, type = Job.Type.GET)
        assertTrue(actual = graph.repositories.buildingEquipmentRepository.executeSyncBuildingEquipment(job = job) is JobResult.Success)

        assertEquals(expected = listOf(element = FakeData.building), actual = graph.useCases.buildingEquipmentUseCases.observeBuildingsOfEquipment(equipmentUuid = FakeData.equipment.uuid).firstOrNull())
    }

    /** Verifies that executing an upsert building equipment job fails without a payload and succeeds once the building equipment is encoded into it. */
    @Test
    fun executeUpsertBuildingEquipment() = runUnitTest {
        val graph = dependency.get().getUserDependency(account = FakeData.adminAccount)
        val buildingEquipmentRepository = graph.repositories.buildingEquipmentRepository
        graph.useCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING_EQUIPMENT, type = Job.Type.POST)
        assertTrue(actual = buildingEquipmentRepository.executeUpsertBuildingEquipment(job = job) is JobResult.Error)

        val jobResult = buildingEquipmentRepository.executeUpsertBuildingEquipment(job = job.copy(payload = encode(value = FakeData.buildingEquipment)))
        assertTrue(actual = jobResult is JobResult.Success)
        assertEquals(expected = listOf(element = FakeData.equipment), actual = graph.useCases.buildingEquipmentUseCases.observeEquipmentsInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull())
    }

    /** Verifies that executing a building equipments sync job fails when the http client flag is disabled. */
    @Test
    fun executeSyncBuildingEquipmentsOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val buildingEquipmentRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .buildingEquipmentRepository

        val jobResult = buildingEquipmentRepository.executeSyncBuildingEquipments(job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING_EQUIPMENT, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** Verifies that an account without building equipment permission is refused every building equipment job. */
    @Test
    fun unauthorizedJobsAreRefused() = runUnitTest {
        val buildingEquipmentRepository = dependency.get()
            .getUserDependency(account = FakeData.account.copy(permissions = mapOf(EntityType.EQUIPMENT to Permission.WRITE)))
            .repositories
            .buildingEquipmentRepository

        listOf(
            buildingEquipmentRepository.executeSyncBuildingEquipments(job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING_EQUIPMENT, type = Job.Type.GET)),
            buildingEquipmentRepository.executeSyncBuildingEquipment(job = Job(userUuid = Uuid.NIL, entityUuid = FakeData.buildingEquipment.uuid, entityType = EntityType.BUILDING_EQUIPMENT, type = Job.Type.GET)),
            buildingEquipmentRepository.executeUpsertBuildingEquipment(job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING_EQUIPMENT, type = Job.Type.POST, payload = encode(value = FakeData.buildingEquipment)))
        ).forEach { assertTrue(actual = it is JobResult.Error) }
    }
}
