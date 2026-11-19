package com.app.builder.domain.usecase.material

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.locale.UNIX_EPOCH
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.database.reset
import com.app.builder.domain.usecase.Outcome
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class MaterialUseCasesTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ServerFlags.set { it.copy(push = true) }
    }

    /** Verifies that upserting a material makes it retrievable and triggers a notification for the users that can read materials. */
    @Test
    fun crudMaterials() = runServerTest {
        val materialUseCases = dependency.get()
            .useCases
            .materialUseCases
        val notificationsUseCases = dependency.get()
            .useCases
            .notificationsUseCases
        val lastSyncUtc = now() - 1.days

        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isEmpty())
        assertTrue(actual = materialUseCases.getMaterials(lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData().isEmpty())
        assertEquals(expected = Outcome.Success(data = Unit), actual = materialUseCases.upsertMaterial(material = FakeData.material))
        assertEquals(expected = listOf(element = FakeData.material), actual = materialUseCases.getMaterials(lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData())
        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isNotEmpty())
    }

    /** Verifies that fetching a single material returns it, and null for an unknown uuid. */
    @Test
    fun getMaterial() = runServerTest {
        val materialUseCases = dependency.get()
            .useCases
            .materialUseCases

        assertEquals(expected = Outcome.Success(data = Unit), actual = materialUseCases.upsertMaterial(material = FakeData.material))
        assertEquals(expected = FakeData.material, actual = materialUseCases.getMaterial(uuid = FakeData.material.uuid).successData())
        assertEquals(expected = Outcome.NotFound, actual = materialUseCases.getMaterial(uuid = uuid()))
    }

    /** Verifies that when the database fails, its reads and writes report [Outcome.Error] rather than an empty list, a missing material or a refused write. */
    @Test
    fun databaseFailureIsAnError() = runServerTest {
        val materialUseCases = dependency.get()
            .useCases
            .materialUseCases
        dependency.get().database.reset()

        listOf(
            "getMaterials" to materialUseCases.getMaterials(lastSyncUtc = Instant.UNIX_EPOCH, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize),
            "getMaterial" to materialUseCases.getMaterial(uuid = FakeData.material.uuid),
            "upsertMaterial" to materialUseCases.upsertMaterial(material = FakeData.material)
        ).forEach { (name, outcome) -> assertEquals(expected = Outcome.Error, actual = outcome, message = name) }
    }
}
