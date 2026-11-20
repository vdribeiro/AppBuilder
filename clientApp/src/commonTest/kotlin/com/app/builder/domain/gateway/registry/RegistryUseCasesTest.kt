package com.app.builder.domain.gateway.registry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.Dependency.getUserDependency
import com.app.builder.domain.EntityType
import com.app.builder.domain.gateway.authentication.toUserSchema
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class RegistryUseCasesTest: TestCase() {

    /** Verifies that stored registries are observable both individually and in the registry list for a user allowed to read them. */
    @Test
    fun observeRegistries() = runUnitTest {
        val registryUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .registryUseCases
        val registryDao = dependency.get()
            .database
            .registryQueries

        assertTrue(actual = registryUseCases.observeRegistries().firstOrNull().orEmpty().isEmpty())
        assertNull(actual = registryUseCases.observeRegistry(uuid = FakeData.registry.requestUuid).firstOrNull())

        registryDao.upsertRegistry(Registry = FakeData.registry.toRegistrySchema())

        val entry = RegistryEntry(registry = FakeData.registry, userName = null)
        assertEquals(expected = listOf(element = entry), actual = registryUseCases.observeRegistries().firstOrNull().orEmpty())
        assertEquals(expected = entry, actual = registryUseCases.observeRegistry(uuid = FakeData.registry.requestUuid).firstOrNull())
    }

    /** Verifies that observed registries carry the name of their user once it is stored, for a user allowed to read users. */
    @Test
    fun observeRegistriesWithUserName() = runUnitTest {
        val registryUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .registryUseCases
        val database = dependency.get().database

        database.registryQueries.upsertRegistry(Registry = FakeData.registry.toRegistrySchema())
        database.userQueries.upsertUser(User = FakeData.adminUser.toUserSchema())

        val entry = RegistryEntry(registry = FakeData.registry, userName = FakeData.adminUser.name)
        assertEquals(expected = listOf(element = entry), actual = registryUseCases.observeRegistries().firstOrNull().orEmpty())
        assertEquals(expected = entry, actual = registryUseCases.observeRegistry(uuid = FakeData.registry.requestUuid).firstOrNull())
    }

    /** Verifies that observed registries carry no user name, even when the user is stored, for a user not allowed to read users. */
    @Test
    fun observeRegistriesWithoutUserPermission() = runUnitTest {
        val registryUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount.copy(permissions = FakeData.adminAccount.permissions - EntityType.USER))
            .useCases
            .registryUseCases
        val database = dependency.get().database

        database.registryQueries.upsertRegistry(Registry = FakeData.registry.toRegistrySchema())
        database.userQueries.upsertUser(User = FakeData.adminUser.toUserSchema())

        val entry = RegistryEntry(registry = FakeData.registry, userName = null)
        assertEquals(expected = listOf(element = entry), actual = registryUseCases.observeRegistries().firstOrNull().orEmpty())
        assertEquals(expected = entry, actual = registryUseCases.observeRegistry(uuid = FakeData.registry.requestUuid).firstOrNull())
    }

    /** Verifies that a user without read permission on registries observes nothing, even when registries are stored. */
    @Test
    fun observeRegistriesWithoutPermission() = runUnitTest {
        val registryUseCases = dependency.get()
            .getUserDependency(account = FakeData.account)
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
