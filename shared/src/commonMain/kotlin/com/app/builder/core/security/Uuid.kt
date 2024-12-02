package com.app.builder.core.security

import kotlin.random.Random
import kotlin.uuid.Uuid
import com.app.builder.core.locale.epoch
import com.app.builder.core.telemetry.Telemetry

/**
 * Generates a unique identifier using a tiered fallback strategy: UUID v7, falling back to UUID v4, and finally to an unsecure pseudo-v7 implementation.
 *
 * @return A unique [Uuid] instance.
 */
fun uuid(): Uuid =
    uuidV7() ?: uuidV4() ?: unsecureUuid()

/**
 * Generates a time-ordered UUID v7 instance.
 *
 * @return A [Uuid] instance, or null if generation fails.
 */
fun uuidV7(): Uuid? = runCatching {
    Uuid.generateV7()
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to generate UUID v7", throwable = it)
}.getOrNull()

/**
 * Generates a random UUID v4 instance.
 *
 * @return A [Uuid] instance, or null if generation fails.
 */
fun uuidV4(): Uuid? = runCatching {
    Uuid.generateV4()
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to generate UUID v4", throwable = it)
}.getOrNull()

/**
 * Manually constructs a non-standard, cryptographically insecure fallback UUID that mimics the v7 layout using system time and pseudo-random numbers.
 *
 * @return A pseudo-v7 [Uuid] instance.
 */
fun unsecureUuid(): Uuid {
    // 48 bits timestamp based on system clock
    val timestamp = epoch() shl 16
    // 4 bits to add v7 identifier
    val version7 = 0x7000L
    // 12 bits of randomness
    val randomMsb = Random.nextLong(until = 0x1000)
    // uuid identifier: sets top bit to 1, leaving 2nd bit as 0
    val variant10 = Long.MIN_VALUE
    // 62 bits of randomness
    val randomLsb = Random.nextLong() and 0x3FFFFFFFFFFFFFFFL

    return Uuid.fromLongs(
        mostSignificantBits = timestamp or version7 or randomMsb,
        leastSignificantBits = variant10 or randomLsb
    )
}

/**
 * Parses a string in the standard hex-and-dash or hexadecimal formats.
 * This function is case-insensitive.
 *
 * @receiver The string in one of the supported uuid formats.
 * @return The [Uuid] instance, or null on failure.
 */
fun String.toUuid(): Uuid? = runCatching {
    Uuid.parse(uuidString = this)
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to parse $this", throwable = it)
}.getOrNull()

private const val TAG = "UUID"
