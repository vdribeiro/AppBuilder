package com.app.builder.domain.gateway.registry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.Dependency.getUserDependency
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class RegistryUseCasesTest: TestCase() {

    /** Verifies that stored registries are observable both individually and in the registry list for a user allowed to read them. */
    @Test
    fun observeRegistries() = runUnitTest {
        val registryUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .registryUseCases
        val registryDao = dependency.get()
            .database
            .registryQueries

        assertTrue(actual = registryUseCases.observeRegistries().firstOrNull().orEmpty().isEmpty())
        assertNull(actual = registryUseCases.observeRegistry(uuid = FakeData.registry.requestUuid).firstOrNull())

        registryDao.upsertRegistry(Registry = FakeData.registry.toRegistrySchema())

        assertEquals(expected = listOf(element = FakeData.registry), actual = registryUseCases.observeRegistries().firstOrNull().orEmpty())
        assertEquals(expected = FakeData.registry, actual = registryUseCases.observeRegistry(uuid = FakeData.registry.requestUuid).firstOrNull())
    }

    /** Verifies that a user without read permission on registries observes nothing, even when registries are stored. */
    @Test
    fun observeRegistriesWithoutPermission() = runUnitTest {
        val registryUseCases = dependency.get()
            .getUserDependency(user = FakeData.user)
            .useCases
            .registryUseCases
        val registryDao = dependency.get()
            .database
            .registryQueries

        registryDao.upsertRegistry(Registry = FakeData.registry.toRegistrySchema())

        assertNull(actual = registryUseCases.observeRegistries().firstOrNull())
        assertNull(actual = registryUseCases.observeRegistry(uuid = FakeData.registry.requestUuid).firstOrNull())
    }
}
