package com.app.builder.domain.usecase.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class ConfigUseCasesTest: TestCase() {

    /** Verifies that loading configs populates the client and server flags and configs. */
    @Test
    fun load() = runServerTest {
        val configUseCases = dependency.get()
            .useCases
            .configUseCases

        configUseCases.load()

        assertEquals(expected = FakeData.clientFlags, actual = ClientFlags.flags)
        assertEquals(expected = FakeData.clientConfigs, actual = ClientConfigs.configs)
        assertEquals(expected = FakeData.serverFlags, actual = ServerFlags.flags)
        assertEquals(expected = FakeData.serverConfigs, actual = ServerConfigs.configs)
    }

    /** Verifies that updating configs and flags persists the new values and reflects them after load. */
    @Test
    fun updateAndLoad() = runServerTest {
        val configUseCases = dependency.get()
            .useCases
            .configUseCases

        val clientFlags = FakeData.clientFlags.copy(scheduler = !FakeData.clientFlags.scheduler)
        val clientConfigs = FakeData.clientConfigs.copy(clockDriftTolerance = 1234L)
        val serverFlags = FakeData.serverFlags.copy(broadcast = !FakeData.serverFlags.broadcast)
        val serverConfigs = FakeData.serverConfigs.copy(accessTokenValidity = 4321L)

        assertTrue(actual = configUseCases.updateClientFeatureFlags(flags = clientFlags))
        assertTrue(actual = configUseCases.updateClientConfigs(configs = clientConfigs))
        assertTrue(actual = configUseCases.updateServerFeatureFlags(flags = serverFlags))
        assertTrue(actual = configUseCases.updateServerConfigs(configs = serverConfigs))

        ClientFlags.reset()
        ClientConfigs.reset()
        ServerFlags.reset()
        ServerConfigs.reset()

        configUseCases.load()

        assertEquals(expected = clientFlags, actual = ClientFlags.flags)
        assertEquals(expected = clientConfigs, actual = ClientConfigs.configs)
        assertEquals(expected = serverFlags, actual = ServerFlags.flags)
        assertEquals(expected = serverConfigs, actual = ServerConfigs.configs)
    }
}
