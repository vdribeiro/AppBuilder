package com.app.builder.domain.usecase.devicetoken

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.app.builder.core.security.uuid
import com.app.builder.domain.DeviceToken
import com.app.builder.domain.usecase.Outcome
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
        assertTrue(actual = deviceTokenUseCases.getTokens(userUuid = userUuid).successData().isEmpty())

        val deviceToken = DeviceToken(deviceUuid = uuid(), token = "fcm-token")
        assertEquals(expected = Outcome.Success(data = Unit), actual = deviceTokenUseCases.registerToken(userUuid = userUuid, registration = deviceToken))
        assertEquals(expected = listOf(deviceToken), actual = deviceTokenUseCases.getTokens(userUuid = userUuid).successData())

        val rotated = deviceToken.copy(token = "rotated-fcm-token")
        assertEquals(expected = Outcome.Success(data = Unit), actual = deviceTokenUseCases.registerToken(userUuid = userUuid, registration = rotated))
        assertEquals(expected = listOf(rotated), actual = deviceTokenUseCases.getTokens(userUuid = userUuid).successData())

        assertTrue(actual = deviceTokenUseCases.getTokens(userUuid = uuid()).successData().isEmpty())

        assertEquals(expected = Outcome.NotFound, actual = deviceTokenUseCases.removeToken(token = "unknown-token"))
        assertEquals(expected = Outcome.NotFound, actual = deviceTokenUseCases.removeToken(token = deviceToken.token))
        assertEquals(expected = Outcome.Success(data = Unit), actual = deviceTokenUseCases.removeToken(token = rotated.token))
        assertTrue(actual = deviceTokenUseCases.getTokens(userUuid = userUuid).successData().isEmpty())
    }

    /**
     * Verifies that registering under another user's device uuid cannot drop that user's registration.
     * The device uuid arrives from the client and is discoverable through the request registry, so nothing but the key's shape stops one account from silently cutting off another's push delivery.
     */
    @Test
    fun registerTokenCannotDisplaceAnotherUsersDevice() = runServerTest {
        val deviceTokenUseCases = dependency.get()
            .useCases
            .deviceTokenUseCases

        val victim = FakeData.user.uuid
        val attacker = FakeData.adminUser.uuid
        val deviceUuid = uuid()

        val victimToken = DeviceToken(deviceUuid = deviceUuid, token = "victim-token")
        assertEquals(expected = Outcome.Success(data = Unit), actual = deviceTokenUseCases.registerToken(userUuid = victim, registration = victimToken))

        // The attacker claims the victim's device uuid, carrying a token of its own since it cannot know the victim's.
        assertEquals(expected = Outcome.Success(data = Unit), actual = deviceTokenUseCases.registerToken(userUuid = attacker, registration = DeviceToken(deviceUuid = deviceUuid, token = "attacker-token")))

        assertEquals(expected = listOf(victimToken), actual = deviceTokenUseCases.getTokens(userUuid = victim).successData())
    }

    /**
     * Verifies that a device changing hands moves the registration rather than leaving two.
     * The incoming token already belongs to another account, which only happens when the same app install signs in as someone else, and the stale row would otherwise keep delivering the previous user's notifications to whoever is holding the device now.
     */
    @Test
    fun registerTokenMovesTheRegistrationWhenADeviceChangesHands() = runServerTest {
        val deviceTokenUseCases = dependency.get()
            .useCases
            .deviceTokenUseCases

        val previous = FakeData.user.uuid
        val next = FakeData.adminUser.uuid
        val shared = DeviceToken(deviceUuid = uuid(), token = "shared-install-token")

        assertEquals(expected = Outcome.Success(data = Unit), actual = deviceTokenUseCases.registerToken(userUuid = previous, registration = shared))
        assertEquals(expected = Outcome.Success(data = Unit), actual = deviceTokenUseCases.registerToken(userUuid = next, registration = shared))

        assertTrue(actual = deviceTokenUseCases.getTokens(userUuid = previous).successData().isEmpty())
        assertEquals(expected = listOf(shared), actual = deviceTokenUseCases.getTokens(userUuid = next).successData())
    }

    /** Verifies that one user keeps a registration per device, so signing in on a second device does not displace the first. */
    @Test
    fun registerTokenKeepsOneRegistrationPerDevice() = runServerTest {
        val deviceTokenUseCases = dependency.get()
            .useCases
            .deviceTokenUseCases

        val userUuid = FakeData.user.uuid
        val phone = DeviceToken(deviceUuid = uuid(), token = "phone-token")
        val tablet = DeviceToken(deviceUuid = uuid(), token = "tablet-token")

        assertEquals(expected = Outcome.Success(data = Unit), actual = deviceTokenUseCases.registerToken(userUuid = userUuid, registration = phone))
        assertEquals(expected = Outcome.Success(data = Unit), actual = deviceTokenUseCases.registerToken(userUuid = userUuid, registration = tablet))
        assertEquals(expected = setOf(phone, tablet), actual = deviceTokenUseCases.getTokens(userUuid = userUuid).successData().toSet())

        // Rotating one device's token leaves the other device alone.
        val rotatedPhone = phone.copy(token = "phone-token-rotated")
        assertEquals(expected = Outcome.Success(data = Unit), actual = deviceTokenUseCases.registerToken(userUuid = userUuid, registration = rotatedPhone))
        assertEquals(expected = setOf(rotatedPhone, tablet), actual = deviceTokenUseCases.getTokens(userUuid = userUuid).successData().toSet())

        // As does dropping one.
        assertEquals(expected = Outcome.Success(data = Unit), actual = deviceTokenUseCases.removeToken(token = rotatedPhone.token))
        assertEquals(expected = listOf(tablet), actual = deviceTokenUseCases.getTokens(userUuid = userUuid).successData())
    }
}
