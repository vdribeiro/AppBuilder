package com.app.builder.domain.gateway.timepunch

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
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.serializer.encode
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.TimePunch
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TimePunchRepositoryTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(http = true, scheduler = true) }
    }

    /** Verifies that executing a time punches sync job succeeds, records the last sync timestamp, and populates the time punch list. */
    @Test
    fun executeSyncTimePunches() = runUnitTest {
        val timePunchUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .timePunchUseCases
        val timePunchRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .timePunchRepository
        assertNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.TIME_PUNCHES_LAST_SYNC_UTC])
        assertTrue(actual = timePunchUseCases.observeTimePunches().firstOrNull().orEmpty().isEmpty())

        val jobResult = timePunchRepository.executeSyncTimePunches(job = Job(userUuid = Uuid.NIL, entityType = EntityType.TIME_PUNCH, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Success)

        assertNotNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.TIME_PUNCHES_LAST_SYNC_UTC])
        assertEquals(expected = listOf(element = FakeData.timePunch), actual = timePunchUseCases.observeTimePunches().firstOrNull().orEmpty())
    }

    /** Verifies that executing a time punches sync job fails when the http client flag is disabled. */
    @Test
    fun executeSyncTimePunchesOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val timePunchRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .timePunchRepository

        val jobResult = timePunchRepository.executeSyncTimePunches(job = Job(userUuid = Uuid.NIL, entityType = EntityType.TIME_PUNCH, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** Verifies that executing an upsert time punch job fails without a payload and succeeds once the time punch is encoded into it. */
    @Test
    fun executeUpsertTimePunch() = runUnitTest {
        val timePunchUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .timePunchUseCases
        val timePunchRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .timePunchRepository

        assertNull(actual = timePunchUseCases.observeTimePunch(uuid = FakeData.timePunch.uuid).firstOrNull())

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.TIME_PUNCH, type = Job.Type.POST)
        assertTrue(actual = timePunchRepository.executeUpsertTimePunch(job = job) is JobResult.Error)

        val jobResult = timePunchRepository.executeUpsertTimePunch(job = job.copy(payload = encode(value = FakeData.timePunch)))
        assertTrue(actual = jobResult is JobResult.Success)

        assertEquals(expected = FakeData.timePunch, actual = timePunchUseCases.observeTimePunch(uuid = FakeData.timePunch.uuid).firstOrNull())
    }

    /** Verifies that executing an upsert time punch job fails when the http client flag is disabled. */
    @Test
    fun executeUpsertTimePunchOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val timePunchRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .timePunchRepository

        val job = Job(
            userUuid = Uuid.NIL,
            entityType = EntityType.TIME_PUNCH,
            type = Job.Type.POST,
            payload = encode(value = FakeData.timePunch)
        )
        val jobResult = timePunchRepository.executeUpsertTimePunch(job = job)
        assertTrue(actual = jobResult is JobResult.Error)
    }

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

    /** Verifies that requesting a time punch list sync and a single time punch sync queues a get job for each. */
    @Test
    fun syncTimePunchesQueuesGetJobs() = runUnitTest {
        val timePunchRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .timePunchRepository

        timePunchRepository.syncTimePunches()
        timePunchRepository.syncTimePunch(uuid = FakeData.timePunch.uuid)

        val jobs = queuedJobs().filter { it.entityType == EntityType.TIME_PUNCH && it.type == Job.Type.GET }
        assertEquals(expected = setOf(null, FakeData.timePunch.uuid), actual = jobs.map { it.entityUuid }.toSet())
    }

    /** Verifies that a user without any time punch permission syncs with its own watermark and observes only its own time punches. */
    @Test
    fun withoutReadPermission() = runUnitTest {
        val adminTimePunch = FakeData.timePunch.copy(uuid = uuid(), userUuid = FakeData.adminUser.uuid)
        dependency.get().getUserDependency(account = FakeData.adminAccount).useCases.timePunchUseCases.upsertTimePunch(timePunch = adminTimePunch)

        // FakeData.account has no time punch permission, and owns FakeData.timePunch.
        val userDependency = dependency.get().getUserDependency(account = FakeData.account)
        val timePunchUseCases = userDependency.useCases.timePunchUseCases
        val timePunchRepository = userDependency.repositories.timePunchRepository

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.TIME_PUNCH, type = Job.Type.GET)
        assertTrue(actual = timePunchRepository.executeSyncTimePunches(job = job) is JobResult.Success)
        assertNotNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.OWN_TIME_PUNCHES_LAST_SYNC_UTC])
        assertNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.TIME_PUNCHES_LAST_SYNC_UTC])

        assertEquals(expected = listOf(element = FakeData.timePunch), actual = timePunchUseCases.observeTimePunches().firstOrNull().orEmpty())
        assertEquals(expected = FakeData.timePunch, actual = timePunchUseCases.observeTimePunch(uuid = FakeData.timePunch.uuid).firstOrNull())
        assertNull(actual = timePunchUseCases.observeTimePunch(uuid = adminTimePunch.uuid).firstOrNull())
    }

    /** Verifies that an upsert job of a user without time punch write permission succeeds for its own time punch, and fails for a deleted one or one of another user. */
    @Test
    fun executeUpsertTimePunchWithoutWritePermission() = runUnitTest {
        val timePunchRepository = dependency.get()
            .getUserDependency(account = FakeData.account)
            .repositories
            .timePunchRepository
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.TIME_PUNCH, type = Job.Type.POST)

        assertTrue(actual = timePunchRepository.executeUpsertTimePunch(job = job.copy(payload = encode(value = FakeData.timePunch))) is JobResult.Success)
        assertTrue(actual = timePunchRepository.executeUpsertTimePunch(job = job.copy(payload = encode(value = FakeData.timePunch.copy(deletedAt = now())))) is JobResult.Error)
        assertTrue(actual = timePunchRepository.executeUpsertTimePunch(job = job.copy(payload = encode(value = FakeData.timePunch.copy(userUuid = FakeData.adminUser.uuid)))) is JobResult.Error)
    }

    /** Verifies that a time punches sync keeps a local time punch that is newer than the remote one, and replaces one that is older. */
    @Test
    fun executeSyncTimePunchesResolvesConflictsByModificationTime() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = FakeData.adminAccount)
        val timePunchUseCases = userDependency.useCases.timePunchUseCases
        val timePunchRepository = userDependency.repositories.timePunchRepository
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.TIME_PUNCH, type = Job.Type.GET)

        val newer = FakeData.timePunch.copy(modifiedAt = FakeData.timePunch.modifiedAt + 1.hours, source = TimePunch.Source.NFC)
        timePunchUseCases.upsertTimePunch(timePunch = newer)
        assertTrue(actual = timePunchRepository.executeSyncTimePunches(job = job) is JobResult.Success)
        assertEquals(expected = newer, actual = timePunchUseCases.observeTimePunch(uuid = FakeData.timePunch.uuid).firstOrNull())

        val older = FakeData.timePunch.copy(modifiedAt = FakeData.timePunch.modifiedAt - 1.hours, source = TimePunch.Source.NFC)
        timePunchUseCases.upsertTimePunch(timePunch = older)
        assertTrue(actual = timePunchRepository.executeSyncTimePunches(job = job) is JobResult.Success)
        assertEquals(expected = FakeData.timePunch, actual = timePunchUseCases.observeTimePunch(uuid = FakeData.timePunch.uuid).firstOrNull())
    }

    /** Verifies that a single time punch sync keeps a local time punch that is newer than the remote one, and replaces one that is older. */
    @Test
    fun executeSyncTimePunchResolvesConflictsByModificationTime() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = FakeData.adminAccount)
        val timePunchUseCases = userDependency.useCases.timePunchUseCases
        val timePunchRepository = userDependency.repositories.timePunchRepository
        val job = Job(userUuid = Uuid.NIL, entityUuid = FakeData.timePunch.uuid, entityType = EntityType.TIME_PUNCH, type = Job.Type.GET)

        val newer = FakeData.timePunch.copy(modifiedAt = FakeData.timePunch.modifiedAt + 1.hours, source = TimePunch.Source.NFC)
        timePunchUseCases.upsertTimePunch(timePunch = newer)
        assertTrue(actual = timePunchRepository.executeSyncTimePunch(job = job) is JobResult.Success)
        assertEquals(expected = newer, actual = timePunchUseCases.observeTimePunch(uuid = FakeData.timePunch.uuid).firstOrNull())

        val older = FakeData.timePunch.copy(modifiedAt = FakeData.timePunch.modifiedAt - 1.hours, source = TimePunch.Source.NFC)
        timePunchUseCases.upsertTimePunch(timePunch = older)
        assertTrue(actual = timePunchRepository.executeSyncTimePunch(job = job) is JobResult.Success)
        assertEquals(expected = FakeData.timePunch, actual = timePunchUseCases.observeTimePunch(uuid = FakeData.timePunch.uuid).firstOrNull())
    }

    /** Verifies that a single time punch sync job without an entity uuid fails. */
    @Test
    fun executeSyncTimePunchWithoutEntityUuid() = runUnitTest {
        val timePunchRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .timePunchRepository

        val jobResult = timePunchRepository.executeSyncTimePunch(job = Job(userUuid = Uuid.NIL, entityType = EntityType.TIME_PUNCH, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }
}
