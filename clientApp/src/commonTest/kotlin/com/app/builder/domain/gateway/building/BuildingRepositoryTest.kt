package com.app.builder.domain.gateway.building

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

class BuildingRepositoryTest: TestCase() {

    /** Verifies that executing a buildings sync job succeeds, records the last sync timestamp, and populates the building list. */
    @Test
    fun executeSyncBuildings() = runUnitTest {
        val buildingUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .buildingUseCases
        val buildingRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .buildingRepository
        assertNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.BUILDINGS_LAST_SYNC_UTC])
        assertTrue(actual = buildingUseCases.observeBuildings().firstOrNull().orEmpty().isEmpty())

        val jobResult = buildingRepository.executeSyncBuildings(job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Success)

        assertNotNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.BUILDINGS_LAST_SYNC_UTC])
        assertEquals(expected = listOf(element = FakeData.building), actual = buildingUseCases.observeBuildings().firstOrNull().orEmpty())
    }

    /** Verifies that executing a buildings sync job fails when the http client flag is disabled. */
    @Test
    fun executeSyncBuildingsOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val buildingRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .buildingRepository

        val jobResult = buildingRepository.executeSyncBuildings(job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** Verifies that executing an upsert building job fails without a payload and succeeds once the building is encoded into it. */
    @Test
    fun executeUpsertBuilding() = runUnitTest {
        val buildingUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .buildingUseCases
        val buildingRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .buildingRepository

        assertNull(actual = buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).firstOrNull())

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING, type = Job.Type.POST)
        assertTrue(actual = buildingRepository.executeUpsertBuilding(job = job) is JobResult.Error)

        val jobResult = buildingRepository.executeUpsertBuilding(job = job.copy(payload = encode(value = FakeData.building)))
        assertTrue(actual = jobResult is JobResult.Success)

        assertEquals(expected = FakeData.building, actual = buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).firstOrNull())
    }

    /** Verifies that executing an upsert building job fails when the http client flag is disabled. */
    @Test
    fun executeUpsertBuildingOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val buildingRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .buildingRepository

        val job = Job(
            userUuid = Uuid.NIL,
            entityType = EntityType.BUILDING,
            type = Job.Type.POST,
            payload = encode(value = FakeData.building)
        )
        val jobResult = buildingRepository.executeUpsertBuilding(job = job)
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** An account without any permission, so every building operation is refused. */
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

    /** Verifies that requesting a building list sync and a single building sync queues a get job for each. */
    @Test
    fun syncBuildingsQueuesGetJobs() = runUnitTest {
        val buildingRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .buildingRepository

        buildingRepository.syncBuildings()
        buildingRepository.syncBuilding(uuid = FakeData.building.uuid)

        val jobs = queuedJobs().filter { it.entityType == EntityType.BUILDING && it.type == Job.Type.GET }
        assertEquals(expected = setOf(null, FakeData.building.uuid), actual = jobs.map { it.entityUuid }.toSet())
    }

    /** Verifies that a user without read permission observes no buildings, queues no sync and fails every sync job. */
    @Test
    fun withoutReadPermission() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = unauthorizedAccount)
        val buildingUseCases = userDependency.useCases.buildingUseCases
        val buildingRepository = userDependency.repositories.buildingRepository

        assertNull(actual = buildingUseCases.observeBuildings().firstOrNull())
        assertNull(actual = buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).firstOrNull())

        buildingRepository.syncBuildings()
        buildingRepository.syncBuilding(uuid = FakeData.building.uuid)
        assertTrue(actual = queuedJobs().isEmpty())

        val job = Job(userUuid = Uuid.NIL, entityUuid = FakeData.building.uuid, entityType = EntityType.BUILDING, type = Job.Type.GET)
        assertTrue(actual = buildingRepository.executeSyncBuildings(job = job) is JobResult.Error)
        assertTrue(actual = buildingRepository.executeSyncBuilding(job = job) is JobResult.Error)
    }

    /** Verifies that a buildings sync keeps a local building that is newer than the remote one, and replaces one that is older. */
    @Test
    fun executeSyncBuildingsResolvesConflictsByModificationTime() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = FakeData.adminAccount)
        val buildingUseCases = userDependency.useCases.buildingUseCases
        val buildingRepository = userDependency.repositories.buildingRepository
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING, type = Job.Type.GET)

        val newer = FakeData.building.copy(modifiedAt = FakeData.building.modifiedAt + 1.hours, name = "Local")
        buildingUseCases.upsertBuilding(building = newer)
        assertTrue(actual = buildingRepository.executeSyncBuildings(job = job) is JobResult.Success)
        assertEquals(expected = newer, actual = buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).firstOrNull())

        val older = FakeData.building.copy(modifiedAt = FakeData.building.modifiedAt - 1.hours, name = "Local")
        buildingUseCases.upsertBuilding(building = older)
        assertTrue(actual = buildingRepository.executeSyncBuildings(job = job) is JobResult.Success)
        assertEquals(expected = FakeData.building, actual = buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).firstOrNull())
    }

    /** Verifies that a single building sync keeps a local building that is newer than the remote one, and replaces one that is older. */
    @Test
    fun executeSyncBuildingResolvesConflictsByModificationTime() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = FakeData.adminAccount)
        val buildingUseCases = userDependency.useCases.buildingUseCases
        val buildingRepository = userDependency.repositories.buildingRepository
        val job = Job(userUuid = Uuid.NIL, entityUuid = FakeData.building.uuid, entityType = EntityType.BUILDING, type = Job.Type.GET)

        val newer = FakeData.building.copy(modifiedAt = FakeData.building.modifiedAt + 1.hours, name = "Local")
        buildingUseCases.upsertBuilding(building = newer)
        assertTrue(actual = buildingRepository.executeSyncBuilding(job = job) is JobResult.Success)
        assertEquals(expected = newer, actual = buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).firstOrNull())

        val older = FakeData.building.copy(modifiedAt = FakeData.building.modifiedAt - 1.hours, name = "Local")
        buildingUseCases.upsertBuilding(building = older)
        assertTrue(actual = buildingRepository.executeSyncBuilding(job = job) is JobResult.Success)
        assertEquals(expected = FakeData.building, actual = buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).firstOrNull())
    }

    /** Verifies that a single building sync job without an entity uuid fails. */
    @Test
    fun executeSyncBuildingWithoutEntityUuid() = runUnitTest {
        val buildingRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .buildingRepository

        val jobResult = buildingRepository.executeSyncBuilding(job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }
}
