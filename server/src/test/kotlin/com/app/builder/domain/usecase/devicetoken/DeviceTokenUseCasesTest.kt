package com.app.builder.domain.usecase.devicetoken

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.app.builder.core.security.uuid
import com.app.builder.domain.DeviceToken
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class DeviceTokenUseCasesTest: TestCase() {

    /** Verifies that a device token can be registered, rotated, and removed. */
    @Test
    fun registerGetRemoveTokens() = runServerTest {
        val deviceTokenUseCases = dependency.get()
            .useCases
            .deviceTokenUseCases

        val userUuid = FakeData.adminUser.uuid
        assertTrue(actual = deviceTokenUseCases.getTokens(userUuid = userUuid).isEmpty())

        val deviceToken = DeviceToken(deviceUuid = uuid(), token = "fcm-token")
        assertTrue(actual = deviceTokenUseCases.registerToken(userUuid = userUuid, registration = deviceToken))
        assertEquals(expected = listOf(deviceToken), actual = deviceTokenUseCases.getTokens(userUuid = userUuid))

        val rotated = deviceToken.copy(token = "rotated-fcm-token")
        assertTrue(actual = deviceTokenUseCases.registerToken(userUuid = userUuid, registration = rotated))
        assertEquals(expected = listOf(rotated), actual = deviceTokenUseCases.getTokens(userUuid = userUuid))

        assertTrue(actual = deviceTokenUseCases.getTokens(userUuid = uuid()).isEmpty())

        assertFalse(actual = deviceTokenUseCases.removeToken(token = "unknown-token"))
        assertFalse(actual = deviceTokenUseCases.removeToken(token = deviceToken.token))
        assertTrue(actual = deviceTokenUseCases.removeToken(token = rotated.token))
        assertTrue(actual = deviceTokenUseCases.getTokens(userUuid = userUuid).isEmpty())
    }
}
