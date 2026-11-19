package com.app.builder.domain.gateway.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class ConfigUseCasesTest: TestCase() {

    /** Verifies that updating client feature flags remotely returns the updated flags. */
    @Test
    fun updateClientFeatureFlags() = runUnitTest {
        val configUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .configUseCases

        val flags = ClientFlags.value.copy(music = !ClientFlags.value.music)

        assertEquals(expected = flags, actual = configUseCases.updateFeatureFlags(flags = flags))
    }

    /** Verifies that updating client feature flags returns null when the http client flag is disabled. */
    @Test
    fun updateClientFeatureFlagsOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val configUseCases = dependency.get()
            .userGraph
            .useCases
            .configUseCases

        assertNull(actual = configUseCases.updateFeatureFlags(flags = ClientFlags.value))
    }

    /** Verifies that updating client remote configs remotely returns the updated configs. */
    @Test
    fun updateClientConfigs() = runUnitTest {
        val configUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .configUseCases

        val configs = ClientConfigs.value.copy(httpConnectTimeoutMillis = 1234L)

        assertEquals(expected = configs, actual = configUseCases.updateConfigs(configs = configs))
    }

    /** Verifies that updating client remote configs returns null when the http client flag is disabled. */
    @Test
    fun updateClientConfigsOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val configUseCases = dependency.get()
            .userGraph
            .useCases
            .configUseCases

        assertNull(actual = configUseCases.updateConfigs(configs = ClientConfigs.value))
    }

    /** Verifies that fetching and updating server feature flags returns the expected flags. */
    @Test
    fun updateServerFeatureFlags() = runUnitTest {
        val configUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .configUseCases

        assertEquals(expected = ServerFlags.value, actual = configUseCases.getServerFeatureFlags())
        val flags = ServerFlags.value.copy(probe = !ServerFlags.value.probe)
        assertEquals(expected = flags, actual = configUseCases.updateServerFeatureFlags(flags = flags))
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
        assertNull(actual = configUseCases.updateServerFeatureFlags(flags = ServerFlags.value))
    }

    /** Verifies that fetching and updating server remote configs returns the expected configs. */
    @Test
    fun updateServerConfigs() = runUnitTest {
        val configUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .configUseCases

        assertEquals(expected = ServerConfigs.value, actual = configUseCases.getServerConfigs())
        val configs = ServerConfigs.value.copy(accessTokenValidity = 4321L)
        assertEquals(expected = configs, actual = configUseCases.updateServerConfigs(configs = configs))
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
        assertNull(actual = configUseCases.updateServerConfigs(configs = ServerConfigs.value))
    }
}
