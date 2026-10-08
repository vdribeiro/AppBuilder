package com.app.builder.core.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import com.app.builder.core.platform.Env
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.permission.createAccessToken
import com.app.builder.domain.permission.UserAccess
import com.app.builder.domain.permission.getAccess
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class AccessTest: TestCase() {

    /** Verifies that access tokens generated for different users are distinct. */
    @Test
    fun accessTokens() = runServerTest {
        val adminToken = testAccessToken(userUuid = FakeData.adminUser.uuid, access = UserAccess(permissions = mapOf(EntityType.USER to Permission.READ), deletedAt = null))
        val userToken = testAccessToken(userUuid = FakeData.user.uuid, access = UserAccess(permissions = emptyMap(), deletedAt = null))
        assertNotEquals(illegal = adminToken, actual = userToken)
    }

    /**
     * Creates an access token and asserts its claims match the given user and authorization state.
     *
     * @param userUuid UUID of the user the token is issued for.
     * @param access The authorization state the token is expected to carry.
     * @return The created access token.
     */
    private fun testAccessToken(userUuid: Uuid, access: UserAccess): String {
        val token = assertNotNull(actual = createAccessToken(userUuid = userUuid, access = access))
        val payload = createVerifier().verify(token)

        assertEquals(expected = userUuid.toString(), actual = payload.subject)
        assertEquals(expected = access, actual = payload.getAccess())
        assertTrue(actual = payload.audience.contains(element = Env.jwtAudience))
        assertEquals(expected = Env.jwtIssuer, actual = payload.issuer)
        assertNotNull(actual = payload.expiresAt)
        return token
    }

    /** Verifies that generated refresh tokens are well-formed and unique. */
    @Test
    fun refreshTokens() = runServerTest {
        val first = assertNotNull(actual = createRefreshToken())
        val second = assertNotNull(actual = createRefreshToken())

        assertEquals(expected = 64, actual = first.length)
        assertTrue(actual = first.all { it in "0123456789abcdef" })
        assertTrue(actual = second.all { it in "0123456789abcdef" })
        assertNotEquals(illegal = first, actual = second)
    }
}
