package com.app.builder.core.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaInstant
import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.now
import com.app.builder.core.platform.Env
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.permission.UserAccess
import com.app.builder.domain.permission.createAccessToken
import com.app.builder.domain.permission.getAccess
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException

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

    /** Verifies that the verifier rejects a token signed with a different secret. */
    @Test
    fun verifierRejectsForeignSignature() = runServerTest {
        val forgedToken = JWT.create()
            .withSubject(FakeData.adminUser.uuid.toString())
            .withAudience(Env.jwtAudience)
            .withIssuer(Env.jwtIssuer)
            .withExpiresAt((now() + 60_000L.milliseconds).toJavaInstant())
            .sign(Algorithm.HMAC512("${Env.jwtSecret}-forged"))

        assertFailsWith<JWTVerificationException> { createVerifier().verify(forgedToken) }
    }

    /** Verifies that the verifier rejects a token whose audience or issuer does not match the configured ones. */
    @Test
    fun verifierRejectsWrongAudienceAndIssuer() = runServerTest {
        val expiresAt = (now() + 60_000L.milliseconds).toJavaInstant()
        val wrongAudienceToken = JWT.create()
            .withAudience("${Env.jwtAudience}-other")
            .withIssuer(Env.jwtIssuer)
            .withExpiresAt(expiresAt)
            .sign(Algorithm.HMAC512(Env.jwtSecret))
        val wrongIssuerToken = JWT.create()
            .withAudience(Env.jwtAudience)
            .withIssuer("${Env.jwtIssuer}-other")
            .withExpiresAt(expiresAt)
            .sign(Algorithm.HMAC512(Env.jwtSecret))

        assertFailsWith<JWTVerificationException> { createVerifier().verify(wrongAudienceToken) }
        assertFailsWith<JWTVerificationException> { createVerifier().verify(wrongIssuerToken) }
    }

    /** Verifies that an access token is rejected once its configured validity has elapsed. */
    @Test
    fun expiredAccessTokenIsRejected() = runServerTest {
        ServerConfigs.set { it.copy(accessTokenValidity = -60_000L) }
        val token = assertNotNull(actual = createAccessToken(userUuid = FakeData.user.uuid, access = UserAccess(permissions = emptyMap(), deletedAt = null)))

        assertFailsWith<TokenExpiredException> { createVerifier().verify(token) }
    }

    /** Verifies that reading the access claim yields null when the token carries no access claim or an undecodable one. */
    @Test
    fun getAccessWithoutValidClaim() = runServerTest {
        val noClaimToken = assertNotNull(actual = createAccessToken(userUuid = FakeData.user.uuid, claimName = "other", claimValue = "value"))
        val invalidClaimToken = assertNotNull(actual = createAccessToken(userUuid = FakeData.user.uuid, claimName = "access", claimValue = "not json"))

        assertNull(actual = createVerifier().verify(noClaimToken).getAccess())
        assertNull(actual = createVerifier().verify(invalidClaimToken).getAccess())
    }
}
