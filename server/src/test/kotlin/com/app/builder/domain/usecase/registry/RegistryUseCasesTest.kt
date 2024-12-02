package com.app.builder.domain.usecase.registry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.database.create
import com.app.builder.data.database.reset
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class RegistryUseCasesTest: TestCase() {

    /** Verifies that a registry can be upserted and retrieved. */
    @Test
    fun crudRegistry() = runServerTest {
        val registryUseCases = dependency.get()
            .useCases
            .registryUseCases
        val lastSyncUtc = now() - 1.days

        assertTrue(actual = registryUseCases.getRegistries(lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.configs.pageSize).isEmpty())
        assertTrue(actual = registryUseCases.upsertRegistry(registry = FakeData.registry))
        assertEquals(expected = listOf(FakeData.registry), actual = registryUseCases.getRegistries(lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.configs.pageSize))
    }

    /** Verifies that fetching registries filters by last sync time and paginates using the cursor. */
    @Test
    fun getRegistriesSinceLastSyncAndPaginated() = runServerTest {
        dependency.get().database.reset()
        dependency.get().database.create()

        val registryUseCases = dependency.get()
            .useCases
            .registryUseCases

        val now = now()
        val old = FakeData.registry.copy(requestUuid = uuid(), requestUtc = now - 10.minutes)
        val recent = FakeData.registry.copy(requestUuid = uuid(), requestUtc = now)

        assertTrue(actual = registryUseCases.upsertRegistry(registry = old))
        assertTrue(actual = registryUseCases.upsertRegistry(registry = recent))

        assertEquals(expected = listOf(recent), actual = registryUseCases.getRegistries(lastSyncUtc = now - 1.minutes, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.configs.pageSize))
        assertTrue(actual = registryUseCases.getRegistries(lastSyncUtc = now + 1.minutes, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.configs.pageSize).isEmpty())

        val firstPage = registryUseCases.getRegistries(lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0), cursorUtc = null, cursorUuid = null, pageSize = 1)
        assertEquals(expected = listOf(recent), actual = firstPage)

        val secondPage = registryUseCases.getRegistries(
            lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0),
            cursorUtc = firstPage.last().requestUtc,
            cursorUuid = firstPage.last().requestUuid,
            pageSize = 1
        )
        assertEquals(expected = listOf(old), actual = secondPage)
    }

    /** Verifies that upserting an existing requestUuid updates the record in place instead of creating a duplicate. */
    @Test
    fun upsertRegistryUpdatesExistingEntry() = runServerTest {
        val registryUseCases = dependency.get()
            .useCases
            .registryUseCases

        assertTrue(actual = registryUseCases.upsertRegistry(registry = FakeData.registry))
        val updated = FakeData.registry.copy(appVersion = "2.0.0")
        assertTrue(actual = registryUseCases.upsertRegistry(registry = updated))

        val registries = registryUseCases.getRegistries(lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0), cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.configs.pageSize)
        assertEquals(expected = listOf(updated), actual = registries)
    }
}
