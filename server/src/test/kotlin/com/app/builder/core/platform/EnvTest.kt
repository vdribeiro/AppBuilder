package com.app.builder.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.app.builder.test.TestCase

class EnvTest: TestCase() {

    /** Verifies that the environment variables provided to the test run are parsed into their typed values, and unset optional ones fall back to their defaults. */
    @Test
    fun env() = runServerTest {
        assertTrue(actual = Env.developmentMode)
        assertEquals(expected = "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF", actual = Env.adminUuid)
        assertEquals(expected = "admin", actual = Env.adminUsername)
        assertEquals(expected = "admin", actual = Env.adminPassword)
        assertEquals(expected = "jwt_issuer", actual = Env.jwtIssuer)
        assertEquals(expected = "jwt_audience", actual = Env.jwtAudience)
        assertEquals(expected = "jwt_secret", actual = Env.jwtSecret)
        assertEquals(expected = getEnv(name = "TRUSTED_PROXY_HOPS")?.toIntOrNull() ?: 0, actual = Env.trustedProxyHops)
        assertEquals(expected = getEnv(name = "PORT")?.toIntOrNull(), actual = Env.port)
    }
}
