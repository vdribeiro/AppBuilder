package com.app.builder.domain.gateway.user

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

class UserRepositoryTest: TestCase() {

    /** Verifies that executing a users sync job succeeds, records the last sync timestamp, and populates the user list. */
    @Test
    fun executeSyncUsers() = runUnitTest {
        val userUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .userUseCases
        val userRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .repositories
            .userRepository
        assertNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.USERS_LAST_SYNC_UTC])
        assertTrue(actual = userUseCases.observeUsers().firstOrNull().orEmpty().isEmpty())

        val jobResult = userRepository.executeSyncUsers(job = Job(userUuid = Uuid.NIL, entityType = EntityType.USER, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Success)

        assertNotNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.USERS_LAST_SYNC_UTC])
        assertEquals(expected = listOf(FakeData.adminUser, FakeData.user), actual = userUseCases.observeUsers().firstOrNull().orEmpty())
    }

    /** Verifies that executing a users sync job fails when the http client flag is disabled. */
    @Test
    fun executeSyncUsersOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val userRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .repositories
            .userRepository

        val jobResult = userRepository.executeSyncUsers(job = Job(userUuid = Uuid.NIL, entityType = EntityType.USER, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** Verifies that executing a get-user job fetches the remote user and syncs it into the local database. */
    @Test
    fun executeSyncUser() = runUnitTest {
        val userUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .userUseCases
        val userRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .repositories
            .userRepository

        assertNull(actual = userUseCases.observeUser(uuid = FakeData.adminUser.uuid).firstOrNull())

        val jobResult = userRepository.executeSyncUser(
            job = Job(userUuid = Uuid.NIL, entityUuid = FakeData.adminUser.uuid, entityType = EntityType.USER, type = Job.Type.GET)
        )
        assertTrue(actual = jobResult is JobResult.Success)

        assertEquals(expected = FakeData.adminUser, actual = userUseCases.observeUser(uuid = FakeData.adminUser.uuid).firstOrNull())
    }

    /** Verifies that executing an upsert user job fails without a payload and succeeds once the user is encoded into it. */
    @Test
    fun executeUpsertUser() = runUnitTest {
        val userUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .userUseCases
        val userRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .repositories
            .userRepository

        assertNull(actual = userUseCases.observeUser(uuid = FakeData.adminUser.uuid).firstOrNull())

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.USER, type = Job.Type.POST)
        assertTrue(actual = userRepository.executeUpsertUser(job = job) is JobResult.Error)

        val jobResult = userRepository.executeUpsertUser(job = job.copy(payload = encode(value = FakeData.adminUser)))
        assertTrue(actual = jobResult is JobResult.Success)

        assertEquals(expected = FakeData.adminUser, actual = userUseCases.observeUser(uuid = FakeData.adminUser.uuid).firstOrNull())
    }

    /** Verifies that executing an upsert user job fails when the http client flag is disabled. */
    @Test
    fun executeUpsertUserOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val userRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .repositories
            .userRepository

        val job = Job(
            userUuid = Uuid.NIL,
            entityType = EntityType.USER,
            type = Job.Type.POST,
            payload = encode(value = FakeData.adminUser)
        )
        val jobResult = userRepository.executeUpsertUser(job = job)
        assertTrue(actual = jobResult is JobResult.Error)
    }
}
