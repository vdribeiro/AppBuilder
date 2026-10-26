package com.app.builder.domain.gateway.registry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class RegistryRepositoryTest: TestCase() {

    /** Verifies that syncing registries queues a GET job for a user allowed to read them. */
    @Test
    fun syncRegistries() = runUnitTest {
        val registryRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .repositories
            .registryRepository
        val jobDao = dependency.get()
            .database
            .jobQueries

        registryRepository.syncRegistries()
        val jobs = jobDao.getJobsByState(states = Job.State.entries).awaitAsList()

        assertNotNull(actual = jobs.find { it.entityType == EntityType.REGISTRY && it.type == Job.Type.GET && it.userUuid == FakeData.adminUser.uuid })
    }

    /** Verifies that syncing registries queues nothing for a user without read permission on them. */
    @Test
    fun syncRegistriesWithoutPermission() = runUnitTest {
        val registryRepository = dependency.get()
            .getUserDependency(user = FakeData.user)
            .repositories
            .registryRepository
        val jobDao = dependency.get()
            .database
            .jobQueries

        registryRepository.syncRegistries()
        val jobs = jobDao.getJobsByState(states = Job.State.entries).awaitAsList()

        assertNull(actual = jobs.find { it.entityType == EntityType.REGISTRY })
    }

    /** Verifies that executing a registries sync job succeeds, records the last sync timestamp, and stores the fetched registries. */
    @Test
    fun executeSyncRegistries() = runUnitTest {
        val registryRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .repositories
            .registryRepository
        val registryDao = dependency.get()
            .database
            .registryQueries
        assertNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.REGISTRIES_LAST_SYNC_UTC])
        assertTrue(actual = registryDao.getRegistries().awaitAsList().isEmpty())

        val jobResult = registryRepository.executeSyncRegistries(job = Job(userUuid = Uuid.NIL, entityType = EntityType.REGISTRY, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Success)

        assertNotNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.REGISTRIES_LAST_SYNC_UTC])
        assertEquals(expected = listOf(element = FakeData.registry), actual = registryDao.getRegistries().awaitAsList().map { it.toRegistry() })
    }

    /** Verifies that executing a registries sync job fails and stores nothing for a user without read permission on them. */
    @Test
    fun executeSyncRegistriesWithoutPermission() = runUnitTest {
        val registryRepository = dependency.get()
            .getUserDependency(user = FakeData.user)
            .repositories
            .registryRepository
        val registryDao = dependency.get()
            .database
            .registryQueries

        val jobResult = registryRepository.executeSyncRegistries(job = Job(userUuid = Uuid.NIL, entityType = EntityType.REGISTRY, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)

        assertNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.REGISTRIES_LAST_SYNC_UTC])
        assertTrue(actual = registryDao.getRegistries().awaitAsList().isEmpty())
    }

    /** Verifies that executing a registries sync job fails when the http client flag is disabled. */
    @Test
    fun executeSyncRegistriesOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val registryRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .repositories
            .registryRepository

        val jobResult = registryRepository.executeSyncRegistries(job = Job(userUuid = Uuid.NIL, entityType = EntityType.REGISTRY, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
        assertNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.REGISTRIES_LAST_SYNC_UTC])
    }
}
