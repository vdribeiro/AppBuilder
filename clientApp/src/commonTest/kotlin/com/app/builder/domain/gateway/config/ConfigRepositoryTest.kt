package com.app.builder.domain.gateway.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.domain.EntityType
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class ConfigRepositoryTest: TestCase() {

    /** Verifies that loading configs restores the client flags and configs after they were locally mutated. */
    @Test
    fun load() = runUnitTest {
        val configRepository = dependency.get()
            .userGraph
            .repositories
            .configRepository

        configRepository.executeSyncFeatureFlags(job = Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_FLAG, type = Job.Type.GET))
        configRepository.executeSyncConfigs(job = Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_CONFIG, type = Job.Type.GET))

        assertEquals(expected = FakeData.clientFlags, actual = ClientFlags.flags)
        assertEquals(expected = FakeData.clientConfigs, actual = ClientConfigs.configs)

        ClientFlags.set { it.copy(http = !it.http, scheduler = !it.scheduler) }
        ClientConfigs.set { it.copy(httpConnectTimeoutMillis = it.httpConnectTimeoutMillis * 2) }

        configRepository.load()

        assertEquals(expected = FakeData.clientFlags, actual = ClientFlags.flags)
        assertEquals(expected = FakeData.clientConfigs, actual = ClientConfigs.configs)
    }

    /** Verifies that syncing feature flags queues a GET job for client feature flags. */
    @Test
    fun syncFeatureFlags() = runUnitTest {
        val configRepository = dependency.get()
            .userGraph
            .repositories
            .configRepository
        val jobDao = dependency.get()
            .database
            .jobQueries

        configRepository.syncFeatureFlags()
        val jobs = jobDao.getJobsByState(states = Job.State.entries).awaitAsList()

        assertNotNull(actual = jobs.find { it.entityType == EntityType.CLIENT_FLAG && it.type == Job.Type.GET })
    }

    /** Verifies that executing a feature flags sync job succeeds and updates the client flags. */
    @Test
    fun executeSyncFeatureFlags() = runUnitTest {
        val configRepository = dependency.get()
            .userGraph
            .repositories
            .configRepository

        val result = configRepository.executeSyncFeatureFlags(job = Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_FLAG, type = Job.Type.GET))

        assertEquals(expected = JobResult.Success, actual = result)
        assertEquals(expected = FakeData.clientFlags, actual = ClientFlags.flags)
    }

    /** Verifies that executing a feature flags sync job fails when the http client flag is disabled. */
    @Test
    fun executeSyncFeatureFlagsOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val configRepository = dependency.get()
            .userGraph
            .repositories
            .configRepository

        val result = configRepository.executeSyncFeatureFlags(job = Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_FLAG, type = Job.Type.GET))

        assertTrue(actual = result is JobResult.Error)
    }

    /** Verifies that syncing configs queues a GET job for client remote configs. */
    @Test
    fun syncConfigs() = runUnitTest {
        val configRepository = dependency.get()
            .userGraph
            .repositories
            .configRepository
        val jobDao = dependency.get()
            .database
            .jobQueries

        configRepository.syncConfigs()
        val jobs = jobDao.getJobsByState(states = Job.State.entries).awaitAsList()

        assertNotNull(actual = jobs.find { it.entityType == EntityType.CLIENT_CONFIG && it.type == Job.Type.GET })
    }

    /** Verifies that executing a configs sync job succeeds and updates the client configs. */
    @Test
    fun executeSyncConfigs() = runUnitTest {
        val configRepository = dependency.get()
            .userGraph
            .repositories
            .configRepository

        val result = configRepository.executeSyncConfigs(job = Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_CONFIG, type = Job.Type.GET))

        assertEquals(expected = JobResult.Success, actual = result)
        assertEquals(expected = FakeData.clientConfigs, actual = ClientConfigs.configs)
    }

    /** Verifies that executing a configs sync job fails when the http client flag is disabled. */
    @Test
    fun executeSyncConfigsOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val configRepository = dependency.get().userGraph.repositories.configRepository

        val result = configRepository.executeSyncConfigs(job = Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_CONFIG, type = Job.Type.GET))

        assertTrue(actual = result is JobResult.Error)
    }
}
