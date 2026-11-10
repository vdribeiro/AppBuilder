package com.app.builder.domain.usecase.config

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.data.database.reset
import com.app.builder.domain.usecase.Outcome
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

        assertEquals(expected = FakeData.clientFlags, actual = ClientFlags.value)
        assertEquals(expected = FakeData.clientConfigs, actual = ClientConfigs.value)
        assertEquals(expected = FakeData.serverFlags, actual = ServerFlags.value)
        assertEquals(expected = FakeData.serverConfigs, actual = ServerConfigs.value)
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

        assertEquals(expected = Outcome.Success(data = Unit), actual = configUseCases.updateClientFeatureFlags(flags = clientFlags))
        assertEquals(expected = Outcome.Success(data = Unit), actual = configUseCases.updateClientConfigs(configs = clientConfigs))
        assertEquals(expected = Outcome.Success(data = Unit), actual = configUseCases.updateServerFeatureFlags(flags = serverFlags))
        assertEquals(expected = Outcome.Success(data = Unit), actual = configUseCases.updateServerConfigs(configs = serverConfigs))

        ClientFlags.reset()
        ClientConfigs.reset()
        ServerFlags.reset()
        ServerConfigs.reset()

        configUseCases.load()

        assertEquals(expected = clientFlags, actual = ClientFlags.value)
        assertEquals(expected = clientConfigs, actual = ClientConfigs.value)
        assertEquals(expected = serverFlags, actual = ServerFlags.value)
        assertEquals(expected = serverConfigs, actual = ServerConfigs.value)
    }

    /** Verifies that when the database fails, its updates report [Outcome.Error] rather than a refused write. */
    @Test
    fun databaseFailureIsAnError() = runServerTest {
        val configUseCases = dependency.get()
            .useCases
            .configUseCases
        dependency.get().database.reset()

        listOf(
            "updateClientFeatureFlags" to configUseCases.updateClientFeatureFlags(flags = ClientFlags.value),
            "updateClientConfigs" to configUseCases.updateClientConfigs(configs = ClientConfigs.value),
            "updateServerFeatureFlags" to configUseCases.updateServerFeatureFlags(flags = ServerFlags.value),
            "updateServerConfigs" to configUseCases.updateServerConfigs(configs = ServerConfigs.value)
        ).forEach { (name, outcome) -> assertEquals(expected = Outcome.Error, actual = outcome, message = name) }
    }
}
