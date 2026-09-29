package com.app.builder.domain.gateway.material

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

class MaterialRepositoryTest: TestCase() {

    /** Verifies that executing a materials sync job succeeds, records the last sync timestamp, and populates the material list. */
    @Test
    fun executeSyncMaterials() = runUnitTest {
        val materialUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .materialUseCases
        val materialRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
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
            .getUserDependency(user = FakeData.adminUser)
            .repositories
            .materialRepository

        val jobResult = materialRepository.executeSyncMaterials(job = Job(userUuid = Uuid.NIL, entityType = EntityType.MATERIAL, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** Verifies that executing an upsert material job fails without a payload and succeeds once the material is encoded into it. */
    @Test
    fun executeUpsertMaterial() = runUnitTest {
        val materialUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .materialUseCases
        val materialRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
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
            .getUserDependency(user = FakeData.adminUser)
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
}
