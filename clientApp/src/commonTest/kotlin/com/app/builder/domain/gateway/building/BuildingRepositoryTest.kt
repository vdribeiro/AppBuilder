package com.app.builder.domain.gateway.building

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

class BuildingRepositoryTest: TestCase() {

    /** Verifies that executing a buildings sync job succeeds, records the last sync timestamp, and populates the building list. */
    @Test
    fun executeSyncBuildings() = runUnitTest {
        val buildingUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .buildingUseCases
        val buildingRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
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
            .getUserDependency(user = FakeData.adminUser)
            .repositories
            .buildingRepository

        val jobResult = buildingRepository.executeSyncBuildings(job = Job(userUuid = Uuid.NIL, entityType = EntityType.BUILDING, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** Verifies that executing an upsert building job fails without a payload and succeeds once the building is encoded into it. */
    @Test
    fun executeUpsertBuilding() = runUnitTest {
        val buildingUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .buildingUseCases
        val buildingRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
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
            .getUserDependency(user = FakeData.adminUser)
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
}
