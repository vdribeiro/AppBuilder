package com.app.builder.core.security

import java.security.MessageDigest
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

/** Refresh token hash algorithm. */
private const val DIGEST_ALGORITHM = "SHA-256"
/** JWT signing algorithm. */
private val jwtAlgorithm: Algorithm = Algorithm.HMAC512(Env.jwtSecret)

/**
 * Creates a JWT verifier with the configured audience and issuer.
 *
 * @return A JWT verifier instance.
 */
fun createVerifier(): JWTVerifier = JWT
    .require(jwtAlgorithm)
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
        .withExpiresAt((now() + ServerConfigs.value.accessTokenValidity.milliseconds).toJavaInstant())
        .sign(jwtAlgorithm)
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

/**
 * Hashes a refresh token.
 *
 * @param refreshToken The refresh token handed to the client.
 * @return The hex-encoded SHA-256 hash of [refreshToken].
 */
fun hashRefreshToken(refreshToken: String): String =
    MessageDigest.getInstance(DIGEST_ALGORITHM)
        .digest(refreshToken.encodeToByteArray())
        .joinToString(separator = "") { "%02x".format(it) }

private const val TAG = "Authentication"
