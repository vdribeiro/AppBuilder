package com.app.builder.core.security

import java.security.SecureRandom
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaInstant
import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.now
import com.app.builder.core.platform.Env
import com.app.builder.core.telemetry.Telemetry
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Payload

/** JWT signing algorithm. */
private val algorithm: Algorithm = Algorithm.HMAC512(Env.jwtSecret)

/**
 * Creates a JWT verifier with the configured audience and issuer.
 *
 * @return A JWT verifier instance.
 */
fun createVerifier(): JWTVerifier = JWT
    .require(algorithm)
    .withAudience(Env.jwtAudience)
    .withIssuer(Env.jwtIssuer)
    .build()

/**
 * Generates a new JWT access token for the given subject.
 * The token is created with the configured audience and issuer.
 *
 * @param userUuid The principal identifier to be embedded in the token.
 * @param claimName The name of the claim to embed in the token.
 * @param claimValue The value of the claim to embed in the token.
 * @return A signed JWT string if successful, or `null` if the token creation fails.
 */
fun createAccessToken(userUuid: Uuid, claimName: String, claimValue: String): String? = runCatching {
    JWT
        .create()
        .withSubject(userUuid.toString())
        .withClaim(claimName, claimValue)
        .withAudience(Env.jwtAudience)
        .withIssuer(Env.jwtIssuer)
        .withExpiresAt((now() + ServerConfigs.configs.accessTokenValidity.milliseconds).toJavaInstant())
        .sign(algorithm)
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to create access token", throwable = it)
}.getOrNull()

/**
 * Reads a claim's raw string value from the JWT payload.
 *
 * @receiver The JWT payload.
 * @param claimName The name of the claim to read.
 * @return The claim's string value, or `null` if it is missing or not a string.
 */
fun Payload.getClaimValue(claimName: String): String? = getClaim(claimName).asString()

/**
 * Generates a securely random refresh token.
 * Uses [SecureRandom] to generate 32 bytes of random data and encodes it as a hex string.
 *
 * @return A hex-encoded random string if successful, or `null` if the token creation fails.
 */
fun createRefreshToken(): String? = runCatching {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    bytes.joinToString(separator = "") { "%02x".format(it) }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to create refresh token", throwable = it)
}.getOrNull()

private const val TAG = "Authentication"
