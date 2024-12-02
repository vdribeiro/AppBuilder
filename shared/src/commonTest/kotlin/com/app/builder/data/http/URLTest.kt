package com.app.builder.data.http

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.test.TestCase

class URLTest: TestCase() {

    /** Verifies the URL host, dev port, and dev host constants have their expected values. */
    @Test
    fun hostConstants() = runUnitTest {
        assertEquals(expected = "server-670810444998.europe-west1.run.app", actual = URL.HOST)
        assertEquals(expected = 8080, actual = URL.DEV_PORT)
        assertEquals(expected = "localhost:8080", actual = URL.DEV_HOST)
    }

    /** Verifies each URL endpoint constant maps to its expected API path. */
    @Test
    fun urlPaths() = runUnitTest {
        assertEquals(expected = "/api/serverflags", actual = URL.ServerFlags.path)
        assertEquals(expected = "/api/serverconfigs", actual = URL.ServerConfigs.path)
        assertEquals(expected = "/api/clientflags", actual = URL.ClientFlags.path)
        assertEquals(expected = "/api/clientconfigs", actual = URL.ClientConfigs.path)
        assertEquals(expected = "/api/probe", actual = URL.Probe.path)
        assertEquals(expected = "/api/registry", actual = URL.Registries.path)
        assertEquals(expected = "/api/data/translations.json", actual = URL.Translations.path)
        assertEquals(expected = "/api/device-tokens", actual = URL.DeviceTokens.path)
        assertEquals(expected = "/api/broadcast/subscribe", actual = URL.BroadcastSubscribe.path)
        assertEquals(expected = "/api/broadcast", actual = URL.Broadcast.path)
        assertEquals(expected = "/api/tickets", actual = URL.Tickets.path)
        assertEquals(expected = "/api/push", actual = URL.Push.path)
        assertEquals(expected = "/api/register", actual = URL.Register.path)
        assertEquals(expected = "/api/login", actual = URL.Login.path)
        assertEquals(expected = "/api/logout", actual = URL.Logout.path)
        assertEquals(expected = "/api/refresh-tokens", actual = URL.RefreshTokens.path)
        assertEquals(expected = "/api/users", actual = URL.Users.path)
        assertEquals(expected = "/api/device-location", actual = URL.DeviceLocations.path)
        assertEquals(expected = "/api/tasks", actual = URL.Tasks.path)
    }
}
