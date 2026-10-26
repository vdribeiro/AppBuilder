package com.app.builder.domain.gateway.authentication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.app.builder.domain.UserSession
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class MappersTest: TestCase() {

    /** Verifies that users survive a round trip through their database schema, permissions included. */
    @Test
    fun userRoundTrip() = runUnitTest {
        assertEquals(expected = FakeData.adminUser, actual = FakeData.adminUser.toUserSchema().toUser())
        assertEquals(expected = FakeData.user, actual = FakeData.user.toUserSchema().toUser())
    }

    /** Verifies that a user stored with unreadable permissions is mapped back without any permission. */
    @Test
    fun userWithInvalidPermissions() = runUnitTest {
        val schema = FakeData.adminUser.toUserSchema().copy(permissions = "invalid")

        assertTrue(actual = schema.toUser().permissions.isEmpty())
    }

    /** Verifies that a user session maps to a session schema whose bearer token matches the session tokens. */
    @Test
    fun sessionToBearerToken() = runUnitTest {
        val session = UserSession(
            user = FakeData.adminUser,
            username = FakeData.adminCredentials.username,
            salt = "salt",
            verifier = "verifier",
            accessToken = FakeData.adminBearerToken.accessToken,
            refreshToken = FakeData.adminBearerToken.refreshToken
        )
        val schema = session.toSessionSchema()

        assertEquals(expected = FakeData.adminUser.uuid, actual = schema.userUuid)
        assertEquals(expected = session.username, actual = schema.username)
        assertEquals(expected = FakeData.adminBearerToken, actual = schema.toBearerToken())

        val bearerTokens = FakeData.adminBearerToken.toBearerTokens()
        assertEquals(expected = FakeData.adminBearerToken.accessToken, actual = bearerTokens.accessToken)
        assertEquals(expected = FakeData.adminBearerToken.refreshToken, actual = bearerTokens.refreshToken)
    }

    /** Verifies that an encrypted bearer token hides both tokens and decrypts back to the original. */
    @Test
    fun bearerTokenEncryption() = runUnitTest {
        val encrypted = FakeData.adminBearerToken.encrypt()
        assertNotNull(actual = encrypted)

        assertNotEquals(illegal = FakeData.adminBearerToken.accessToken, actual = encrypted.accessToken)
        assertNotEquals(illegal = FakeData.adminBearerToken.refreshToken, actual = encrypted.refreshToken)
        assertEquals(expected = FakeData.adminBearerToken, actual = encrypted.decrypt())
    }

    /** Verifies that the derived verifier is stable for the same salt and changes with the salt. */
    @Test
    fun deriveVerifier() = runUnitTest {
        val verifier = FakeData.adminCredentials.deriveVerifier(salt = "salt")
        assertNotNull(actual = verifier)

        assertEquals(expected = verifier, actual = FakeData.adminCredentials.deriveVerifier(salt = "salt"))
        assertNotEquals(illegal = verifier, actual = FakeData.adminCredentials.deriveVerifier(salt = "pepper"))
        assertNotEquals(illegal = verifier, actual = FakeData.credentials.deriveVerifier(salt = "salt"))
    }
}
