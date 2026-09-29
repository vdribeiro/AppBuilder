package com.app.builder.domain.gateway.equipment

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
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class EquipmentRepositoryTest: TestCase() {

    /** Verifies that executing a equipments sync job succeeds, records the last sync timestamp, and populates the equipment list. */
    @Test
    fun executeSyncEquipments() = runUnitTest {
        val equipmentUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .equipmentUseCases
        val equipmentRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .repositories
            .equipmentRepository
        assertNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.EQUIPMENTS_LAST_SYNC_UTC])
        assertTrue(actual = equipmentUseCases.observeEquipments().firstOrNull().orEmpty().isEmpty())

        val jobResult = equipmentRepository.executeSyncEquipments(job = Job(userUuid = Uuid.NIL, entityType = EntityType.EQUIPMENT, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Success)

        assertNotNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.EQUIPMENTS_LAST_SYNC_UTC])
        assertEquals(expected = listOf(element = FakeData.equipment), actual = equipmentUseCases.observeEquipments().firstOrNull().orEmpty())
    }

    /** Verifies that executing a equipments sync job fails when the http client flag is disabled. */
    @Test
    fun executeSyncEquipmentsOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val equipmentRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .repositories
            .equipmentRepository

        val jobResult = equipmentRepository.executeSyncEquipments(job = Job(userUuid = Uuid.NIL, entityType = EntityType.EQUIPMENT, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** Verifies that executing an upsert equipment job fails without a payload and succeeds once the equipment is encoded into it. */
    @Test
    fun executeUpsertEquipment() = runUnitTest {
        val equipmentUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .equipmentUseCases
        val equipmentRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .repositories
            .equipmentRepository

        assertNull(actual = equipmentUseCases.observeEquipment(uuid = FakeData.equipment.uuid).firstOrNull())

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.EQUIPMENT, type = Job.Type.POST)
        assertTrue(actual = equipmentRepository.executeUpsertEquipment(job = job) is JobResult.Error)

        val jobResult = equipmentRepository.executeUpsertEquipment(job = job.copy(payload = encode(value = FakeData.equipment)))
        assertTrue(actual = jobResult is JobResult.Success)

        assertEquals(expected = FakeData.equipment, actual = equipmentUseCases.observeEquipment(uuid = FakeData.equipment.uuid).firstOrNull())
    }

    /** Verifies that executing an upsert equipment job fails when the http client flag is disabled. */
    @Test
    fun executeUpsertEquipmentOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val equipmentRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .repositories
            .equipmentRepository

        val job = Job(
            userUuid = Uuid.NIL,
            entityType = EntityType.EQUIPMENT,
            type = Job.Type.POST,
            payload = encode(value = FakeData.equipment)
        )
        val jobResult = equipmentRepository.executeUpsertEquipment(job = job)
        assertTrue(actual = jobResult is JobResult.Error)
    }
}
