package com.app.builder.domain.gateway.equipmentuser

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

class EquipmentUserRepositoryTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(http = true) }
    }

    /** Verifies that executing an equipment users sync job succeeds, records the last sync timestamp, and stores the equipment users. */
    @Test
    fun executeSyncEquipmentUsers() = runUnitTest {
        val graph = dependency.get().getUserDependency(account = FakeData.adminAccount)
        graph.useCases.userUseCases.upsertUser(user = FakeData.user)
        assertNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.EQUIPMENT_USERS_LAST_SYNC_UTC])

        val jobResult = graph.repositories.equipmentUserRepository.executeSyncEquipmentUsers(job = Job(userUuid = Uuid.NIL, entityType = EntityType.EQUIPMENT_USER, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Success)

        assertNotNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.EQUIPMENT_USERS_LAST_SYNC_UTC])
        assertEquals(expected = listOf(element = FakeData.user), actual = graph.useCases.equipmentUserUseCases.observeUsersOfEquipment(equipmentUuid = FakeData.equipment.uuid).firstOrNull())
    }

    /** Verifies that executing a single equipment user sync job stores it. */
    @Test
    fun executeSyncEquipmentUser() = runUnitTest {
        val graph = dependency.get().getUserDependency(account = FakeData.adminAccount)
        graph.useCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)

        val job = Job(userUuid = Uuid.NIL, entityUuid = FakeData.equipmentUser.uuid, entityType = EntityType.EQUIPMENT_USER, type = Job.Type.GET)
        assertTrue(actual = graph.repositories.equipmentUserRepository.executeSyncEquipmentUser(job = job) is JobResult.Success)

        assertEquals(expected = listOf(element = FakeData.equipment), actual = graph.useCases.equipmentUserUseCases.observeEquipmentsOfUser(userUuid = FakeData.user.uuid).firstOrNull())
    }

    /** Verifies that executing an upsert equipment user job fails without a payload and succeeds once the equipment user is encoded into it. */
    @Test
    fun executeUpsertEquipmentUser() = runUnitTest {
        val graph = dependency.get().getUserDependency(account = FakeData.adminAccount)
        val equipmentUserRepository = graph.repositories.equipmentUserRepository
        graph.useCases.userUseCases.upsertUser(user = FakeData.adminUser)

        val equipmentUser = FakeData.equipmentUser.copy(userUuid = FakeData.adminUser.uuid)
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.EQUIPMENT_USER, type = Job.Type.POST)
        assertTrue(actual = equipmentUserRepository.executeUpsertEquipmentUser(job = job) is JobResult.Error)

        val jobResult = equipmentUserRepository.executeUpsertEquipmentUser(job = job.copy(payload = encode(value = equipmentUser)))
        assertTrue(actual = jobResult is JobResult.Success)
        assertEquals(expected = listOf(element = FakeData.adminUser), actual = graph.useCases.equipmentUserUseCases.observeUsersOfEquipment(equipmentUuid = FakeData.equipment.uuid).firstOrNull())
    }

    /** Verifies that executing an equipment users sync job fails when the http client flag is disabled. */
    @Test
    fun executeSyncEquipmentUsersOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val equipmentUserRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .equipmentUserRepository

        val jobResult = equipmentUserRepository.executeSyncEquipmentUsers(job = Job(userUuid = Uuid.NIL, entityType = EntityType.EQUIPMENT_USER, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** Verifies that an account without equipment user permission is refused every equipment user job. */
    @Test
    fun unauthorizedJobsAreRefused() = runUnitTest {
        val equipmentUserRepository = dependency.get()
            .getUserDependency(account = FakeData.account.copy(permissions = mapOf(EntityType.USER to Permission.WRITE)))
            .repositories
            .equipmentUserRepository

        listOf(
            equipmentUserRepository.executeSyncEquipmentUsers(job = Job(userUuid = Uuid.NIL, entityType = EntityType.EQUIPMENT_USER, type = Job.Type.GET)),
            equipmentUserRepository.executeSyncEquipmentUser(job = Job(userUuid = Uuid.NIL, entityUuid = FakeData.equipmentUser.uuid, entityType = EntityType.EQUIPMENT_USER, type = Job.Type.GET)),
            equipmentUserRepository.executeUpsertEquipmentUser(job = Job(userUuid = Uuid.NIL, entityType = EntityType.EQUIPMENT_USER, type = Job.Type.POST, payload = encode(value = FakeData.equipmentUser)))
        ).forEach { assertTrue(actual = it is JobResult.Error) }
    }
}
