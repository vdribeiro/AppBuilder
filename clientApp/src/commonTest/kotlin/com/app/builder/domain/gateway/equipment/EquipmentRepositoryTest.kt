package com.app.builder.domain.gateway.equipment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.data.serializer.encode
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Account
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
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .equipmentUseCases
        val equipmentRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
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
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .equipmentRepository

        val jobResult = equipmentRepository.executeSyncEquipments(job = Job(userUuid = Uuid.NIL, entityType = EntityType.EQUIPMENT, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** Verifies that executing an upsert equipment job fails without a payload and succeeds once the equipment is encoded into it. */
    @Test
    fun executeUpsertEquipment() = runUnitTest {
        val equipmentUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .equipmentUseCases
        val equipmentRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
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
            .getUserDependency(account = FakeData.adminAccount)
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

    /** An account without any permission, so every equipment operation is refused. */
    private val unauthorizedAccount: Account = FakeData.account.copy(permissions = emptyMap())

    /**
     * Lists the jobs queued in the scheduler, in every state.
     *
     * @return The queued jobs, oldest first.
     */
    private suspend fun queuedJobs(): List<database.Job> = dependency.get()
        .database
        .jobQueries
        .getJobsByState(states = Job.State.entries)
        .awaitAsList()

    /** Verifies that requesting an equipment list sync and a single equipment sync queues a get job for each. */
    @Test
    fun syncEquipmentsQueuesGetJobs() = runUnitTest {
        val equipmentRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .equipmentRepository

        equipmentRepository.syncEquipments()
        equipmentRepository.syncEquipment(uuid = FakeData.equipment.uuid)

        val jobs = queuedJobs().filter { it.entityType == EntityType.EQUIPMENT && it.type == Job.Type.GET }
        assertEquals(expected = setOf(null, FakeData.equipment.uuid), actual = jobs.map { it.entityUuid }.toSet())
    }

    /** Verifies that a user without read permission observes no equipments, queues no sync and fails every sync job. */
    @Test
    fun withoutReadPermission() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = unauthorizedAccount)
        val equipmentUseCases = userDependency.useCases.equipmentUseCases
        val equipmentRepository = userDependency.repositories.equipmentRepository

        assertNull(actual = equipmentUseCases.observeEquipments().firstOrNull())
        assertNull(actual = equipmentUseCases.observeEquipment(uuid = FakeData.equipment.uuid).firstOrNull())

        equipmentRepository.syncEquipments()
        equipmentRepository.syncEquipment(uuid = FakeData.equipment.uuid)
        assertTrue(actual = queuedJobs().isEmpty())

        val job = Job(userUuid = Uuid.NIL, entityUuid = FakeData.equipment.uuid, entityType = EntityType.EQUIPMENT, type = Job.Type.GET)
        assertTrue(actual = equipmentRepository.executeSyncEquipments(job = job) is JobResult.Error)
        assertTrue(actual = equipmentRepository.executeSyncEquipment(job = job) is JobResult.Error)
    }

    /** Verifies that a equipments sync keeps a local equipment that is newer than the remote one, and replaces one that is older. */
    @Test
    fun executeSyncEquipmentsResolvesConflictsByModificationTime() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = FakeData.adminAccount)
        val equipmentUseCases = userDependency.useCases.equipmentUseCases
        val equipmentRepository = userDependency.repositories.equipmentRepository
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.EQUIPMENT, type = Job.Type.GET)

        val newer = FakeData.equipment.copy(modifiedAt = FakeData.equipment.modifiedAt + 1.hours, name = "Local")
        equipmentUseCases.upsertEquipment(equipment = newer)
        assertTrue(actual = equipmentRepository.executeSyncEquipments(job = job) is JobResult.Success)
        assertEquals(expected = newer, actual = equipmentUseCases.observeEquipment(uuid = FakeData.equipment.uuid).firstOrNull())

        val older = FakeData.equipment.copy(modifiedAt = FakeData.equipment.modifiedAt - 1.hours, name = "Local")
        equipmentUseCases.upsertEquipment(equipment = older)
        assertTrue(actual = equipmentRepository.executeSyncEquipments(job = job) is JobResult.Success)
        assertEquals(expected = FakeData.equipment, actual = equipmentUseCases.observeEquipment(uuid = FakeData.equipment.uuid).firstOrNull())
    }

    /** Verifies that a single equipment sync keeps a local equipment that is newer than the remote one, and replaces one that is older. */
    @Test
    fun executeSyncEquipmentResolvesConflictsByModificationTime() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = FakeData.adminAccount)
        val equipmentUseCases = userDependency.useCases.equipmentUseCases
        val equipmentRepository = userDependency.repositories.equipmentRepository
        val job = Job(userUuid = Uuid.NIL, entityUuid = FakeData.equipment.uuid, entityType = EntityType.EQUIPMENT, type = Job.Type.GET)

        val newer = FakeData.equipment.copy(modifiedAt = FakeData.equipment.modifiedAt + 1.hours, name = "Local")
        equipmentUseCases.upsertEquipment(equipment = newer)
        assertTrue(actual = equipmentRepository.executeSyncEquipment(job = job) is JobResult.Success)
        assertEquals(expected = newer, actual = equipmentUseCases.observeEquipment(uuid = FakeData.equipment.uuid).firstOrNull())

        val older = FakeData.equipment.copy(modifiedAt = FakeData.equipment.modifiedAt - 1.hours, name = "Local")
        equipmentUseCases.upsertEquipment(equipment = older)
        assertTrue(actual = equipmentRepository.executeSyncEquipment(job = job) is JobResult.Success)
        assertEquals(expected = FakeData.equipment, actual = equipmentUseCases.observeEquipment(uuid = FakeData.equipment.uuid).firstOrNull())
    }

    /** Verifies that a single equipment sync job without an entity uuid fails. */
    @Test
    fun executeSyncEquipmentWithoutEntityUuid() = runUnitTest {
        val equipmentRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .equipmentRepository

        val jobResult = equipmentRepository.executeSyncEquipment(job = Job(userUuid = Uuid.NIL, entityType = EntityType.EQUIPMENT, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }
}
