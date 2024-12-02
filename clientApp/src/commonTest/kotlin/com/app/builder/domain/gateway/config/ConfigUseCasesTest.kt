package com.app.builder.domain.gateway.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class ConfigUseCasesTest: TestCase() {

    /** Verifies that updating client feature flags remotely returns the updated flags. */
    @Test
    fun updateClientFeatureFlags() = runUnitTest {
        val configUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .configUseCases

        assertEquals(expected = FakeData.clientFlags, actual = configUseCases.updateFeatureFlags(flags = FakeData.clientFlags))
    }

    /** Verifies that updating client feature flags returns null when the http client flag is disabled. */
    @Test
    fun updateClientFeatureFlagsOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val configUseCases = dependency.get()
            .userGraph
            .useCases
            .configUseCases

        assertNull(actual = configUseCases.updateFeatureFlags(flags = FakeData.clientFlags))
    }

    /** Verifies that updating client remote configs remotely returns the updated configs. */
    @Test
    fun updateClientConfigs() = runUnitTest {
        val configUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .configUseCases

        assertEquals(expected = FakeData.clientConfigs, actual = configUseCases.updateConfigs(configs = FakeData.clientConfigs))
    }

    /** Verifies that updating client remote configs returns null when the http client flag is disabled. */
    @Test
    fun updateClientConfigsOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val configUseCases = dependency.get()
            .userGraph
            .useCases
            .configUseCases

        assertNull(actual = configUseCases.updateConfigs(configs = FakeData.clientConfigs))
    }

    /** Verifies that fetching and updating server feature flags returns the expected flags. */
    @Test
    fun updateServerFeatureFlags() = runUnitTest {
        val configUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .configUseCases

        assertEquals(expected = FakeData.serverFlags, actual = configUseCases.getServerFeatureFlags())
        assertEquals(expected = FakeData.serverFlags, actual = configUseCases.updateServerFeatureFlags(flags = FakeData.serverFlags))
    }

    /** Verifies that fetching and updating server feature flags both return null when the http client flag is disabled. */
    @Test
    fun updateServerFeatureFlagsOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val configUseCases = dependency.get()
            .userGraph
            .useCases
            .configUseCases

        assertNull(actual = configUseCases.getServerFeatureFlags())
        assertNull(actual = configUseCases.updateServerFeatureFlags(flags = FakeData.serverFlags))
    }

    /** Verifies that fetching and updating server remote configs returns the expected configs. */
    @Test
    fun updateServerConfigs() = runUnitTest {
        val configUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .configUseCases

        assertEquals(expected = FakeData.serverConfigs, actual = configUseCases.getServerConfigs())
        assertEquals(expected = FakeData.serverConfigs, actual = configUseCases.updateServerConfigs(configs = FakeData.serverConfigs))
    }

    /** Verifies that fetching and updating server remote configs both return null when the http client flag is disabled. */
    @Test
    fun updateServerConfigsOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val configUseCases = dependency.get()
            .userGraph
            .useCases
            .configUseCases

        assertNull(actual = configUseCases.getServerConfigs())
        assertNull(actual = configUseCases.updateServerConfigs(configs = FakeData.serverConfigs))
    }
}
