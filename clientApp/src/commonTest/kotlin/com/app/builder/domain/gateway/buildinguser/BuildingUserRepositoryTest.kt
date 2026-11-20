package com.app.builder.domain.gateway.buildinguser

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

class BuildingUserRepositoryTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(http = true) }
    }

    /** Verifies that executing a building users sync job succeeds, records the last sync timestamp, and stores the building users. */
    @Test
    fun executeSyncBuildingUsers() = runUnitTest {
        val graph = dependency.get().getUserDependency(account = FakeData.adminAccount)
        graph.useCases.userUseCases.upsertUser(user = FakeData.user)
        assertNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.BUILDING_USERS_LAST_SYNC_UTC])

        val jobResult = graph.repositories.buildingUserRepository.executeSyncBuildingUsers(job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING_USER, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Success)

        assertNotNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.BUILDING_USERS_LAST_SYNC_UTC])
        assertEquals(expected = listOf(element = FakeData.user), actual = graph.useCases.buildingUserUseCases.observeUsersInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull())
    }

    /** Verifies that executing a single building user sync job stores it. */
    @Test
    fun executeSyncBuildingUser() = runUnitTest {
        val graph = dependency.get().getUserDependency(account = FakeData.adminAccount)
        graph.useCases.buildingUseCases.upsertBuilding(building = FakeData.building)

        val job = Job(userUuid = Uuid.NIL, entityUuid = FakeData.buildingUser.uuid, entityType = EntityType.BUILDING_USER, type = Job.Type.GET)
        assertTrue(actual = graph.repositories.buildingUserRepository.executeSyncBuildingUser(job = job) is JobResult.Success)

        assertEquals(expected = listOf(element = FakeData.building), actual = graph.useCases.buildingUserUseCases.observeBuildingsOfUser(userUuid = FakeData.user.uuid).firstOrNull())
    }

    /** Verifies that executing an upsert building user job fails without a payload and succeeds once the building user is encoded into it. */
    @Test
    fun executeUpsertBuildingUser() = runUnitTest {
        val graph = dependency.get().getUserDependency(account = FakeData.adminAccount)
        val buildingUserRepository = graph.repositories.buildingUserRepository
        graph.useCases.userUseCases.upsertUser(user = FakeData.user)

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING_USER, type = Job.Type.POST)
        assertTrue(actual = buildingUserRepository.executeUpsertBuildingUser(job = job) is JobResult.Error)

        val jobResult = buildingUserRepository.executeUpsertBuildingUser(job = job.copy(payload = encode(value = FakeData.buildingUser)))
        assertTrue(actual = jobResult is JobResult.Success)
        assertEquals(expected = listOf(element = FakeData.user), actual = graph.useCases.buildingUserUseCases.observeUsersInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull())
    }

    /** Verifies that executing a building users sync job fails when the http client flag is disabled. */
    @Test
    fun executeSyncBuildingUsersOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val buildingUserRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .buildingUserRepository

        val jobResult = buildingUserRepository.executeSyncBuildingUsers(job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING_USER, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** Verifies that an account without building user permission is refused every building user job. */
    @Test
    fun unauthorizedJobsAreRefused() = runUnitTest {
        val buildingUserRepository = dependency.get()
            .getUserDependency(account = FakeData.account.copy(permissions = mapOf(EntityType.USER to Permission.WRITE)))
            .repositories
            .buildingUserRepository

        listOf(
            buildingUserRepository.executeSyncBuildingUsers(job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING_USER, type = Job.Type.GET)),
            buildingUserRepository.executeSyncBuildingUser(job = Job(userUuid = Uuid.NIL, entityUuid = FakeData.buildingUser.uuid, entityType = EntityType.BUILDING_USER, type = Job.Type.GET)),
            buildingUserRepository.executeUpsertBuildingUser(job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING_USER, type = Job.Type.POST, payload = encode(value = FakeData.buildingUser)))
        ).forEach { assertTrue(actual = it is JobResult.Error) }
    }
}
