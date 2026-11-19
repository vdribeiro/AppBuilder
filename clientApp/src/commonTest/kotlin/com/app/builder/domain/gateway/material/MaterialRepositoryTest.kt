package com.app.builder.domain.gateway.material

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

class MaterialRepositoryTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(http = true, scheduler = true) }
    }

    /** Verifies that executing a materials sync job succeeds, records the last sync timestamp, and populates the material list. */
    @Test
    fun executeSyncMaterials() = runUnitTest {
        val materialUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .materialUseCases
        val materialRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .materialRepository
        assertNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.MATERIALS_LAST_SYNC_UTC])
        assertTrue(actual = materialUseCases.observeMaterials().firstOrNull().orEmpty().isEmpty())

        val jobResult = materialRepository.executeSyncMaterials(job = Job(userUuid = Uuid.NIL, entityType = EntityType.MATERIAL, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Success)

        assertNotNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.MATERIALS_LAST_SYNC_UTC])
        assertEquals(expected = listOf(element = FakeData.material), actual = materialUseCases.observeMaterials().firstOrNull().orEmpty())
    }

    /** Verifies that executing a materials sync job fails when the http client flag is disabled. */
    @Test
    fun executeSyncMaterialsOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val materialRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .materialRepository

        val jobResult = materialRepository.executeSyncMaterials(job = Job(userUuid = Uuid.NIL, entityType = EntityType.MATERIAL, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** Verifies that executing an upsert material job fails without a payload and succeeds once the material is encoded into it. */
    @Test
    fun executeUpsertMaterial() = runUnitTest {
        val materialUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .materialUseCases
        val materialRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .materialRepository

        assertNull(actual = materialUseCases.observeMaterial(uuid = FakeData.material.uuid).firstOrNull())

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.MATERIAL, type = Job.Type.POST)
        assertTrue(actual = materialRepository.executeUpsertMaterial(job = job) is JobResult.Error)

        val jobResult = materialRepository.executeUpsertMaterial(job = job.copy(payload = encode(value = FakeData.material)))
        assertTrue(actual = jobResult is JobResult.Success)

        assertEquals(expected = FakeData.material, actual = materialUseCases.observeMaterial(uuid = FakeData.material.uuid).firstOrNull())
    }

    /** Verifies that executing an upsert material job fails when the http client flag is disabled. */
    @Test
    fun executeUpsertMaterialOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val materialRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .materialRepository

        val job = Job(
            userUuid = Uuid.NIL,
            entityType = EntityType.MATERIAL,
            type = Job.Type.POST,
            payload = encode(value = FakeData.material)
        )
        val jobResult = materialRepository.executeUpsertMaterial(job = job)
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** An account without any permission, so every material operation is refused. */
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

    /** Verifies that requesting a material list sync and a single material sync queues a get job for each. */
    @Test
    fun syncMaterialsQueuesGetJobs() = runUnitTest {
        val materialRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .materialRepository

        materialRepository.syncMaterials()
        materialRepository.syncMaterial(uuid = FakeData.material.uuid)

        val jobs = queuedJobs().filter { it.entityType == EntityType.MATERIAL && it.type == Job.Type.GET }
        assertEquals(expected = setOf(null, FakeData.material.uuid), actual = jobs.map { it.entityUuid }.toSet())
    }

    /** Verifies that a user without read permission observes no materials, queues no sync and fails every sync job. */
    @Test
    fun withoutReadPermission() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = unauthorizedAccount)
        val materialUseCases = userDependency.useCases.materialUseCases
        val materialRepository = userDependency.repositories.materialRepository

        assertNull(actual = materialUseCases.observeMaterials().firstOrNull())
        assertNull(actual = materialUseCases.observeMaterial(uuid = FakeData.material.uuid).firstOrNull())

        materialRepository.syncMaterials()
        materialRepository.syncMaterial(uuid = FakeData.material.uuid)
        assertTrue(actual = queuedJobs().isEmpty())

        val job = Job(userUuid = Uuid.NIL, entityUuid = FakeData.material.uuid, entityType = EntityType.MATERIAL, type = Job.Type.GET)
        assertTrue(actual = materialRepository.executeSyncMaterials(job = job) is JobResult.Error)
        assertTrue(actual = materialRepository.executeSyncMaterial(job = job) is JobResult.Error)
    }

    /** Verifies that a materials sync keeps a local material that is newer than the remote one, and replaces one that is older. */
    @Test
    fun executeSyncMaterialsResolvesConflictsByModificationTime() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = FakeData.adminAccount)
        val materialUseCases = userDependency.useCases.materialUseCases
        val materialRepository = userDependency.repositories.materialRepository
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.MATERIAL, type = Job.Type.GET)

        val newer = FakeData.material.copy(modifiedAt = FakeData.material.modifiedAt + 1.hours, name = "Local")
        materialUseCases.upsertMaterial(material = newer)
        assertTrue(actual = materialRepository.executeSyncMaterials(job = job) is JobResult.Success)
        assertEquals(expected = newer, actual = materialUseCases.observeMaterial(uuid = FakeData.material.uuid).firstOrNull())

        val older = FakeData.material.copy(modifiedAt = FakeData.material.modifiedAt - 1.hours, name = "Local")
        materialUseCases.upsertMaterial(material = older)
        assertTrue(actual = materialRepository.executeSyncMaterials(job = job) is JobResult.Success)
        assertEquals(expected = FakeData.material, actual = materialUseCases.observeMaterial(uuid = FakeData.material.uuid).firstOrNull())
    }

    /** Verifies that a single material sync keeps a local material that is newer than the remote one, and replaces one that is older. */
    @Test
    fun executeSyncMaterialResolvesConflictsByModificationTime() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = FakeData.adminAccount)
        val materialUseCases = userDependency.useCases.materialUseCases
        val materialRepository = userDependency.repositories.materialRepository
        val job = Job(userUuid = Uuid.NIL, entityUuid = FakeData.material.uuid, entityType = EntityType.MATERIAL, type = Job.Type.GET)

        val newer = FakeData.material.copy(modifiedAt = FakeData.material.modifiedAt + 1.hours, name = "Local")
        materialUseCases.upsertMaterial(material = newer)
        assertTrue(actual = materialRepository.executeSyncMaterial(job = job) is JobResult.Success)
        assertEquals(expected = newer, actual = materialUseCases.observeMaterial(uuid = FakeData.material.uuid).firstOrNull())

        val older = FakeData.material.copy(modifiedAt = FakeData.material.modifiedAt - 1.hours, name = "Local")
        materialUseCases.upsertMaterial(material = older)
        assertTrue(actual = materialRepository.executeSyncMaterial(job = job) is JobResult.Success)
        assertEquals(expected = FakeData.material, actual = materialUseCases.observeMaterial(uuid = FakeData.material.uuid).firstOrNull())
    }

    /** Verifies that a single material sync job without an entity uuid fails. */
    @Test
    fun executeSyncMaterialWithoutEntityUuid() = runUnitTest {
        val materialRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .materialRepository

        val jobResult = materialRepository.executeSyncMaterial(job = Job(userUuid = Uuid.NIL, entityType = EntityType.MATERIAL, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }
}
