package com.app.builder.core.locale

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.TimeSource

/**
 * Returns the current time [Instant].
 *
 * @return The current [Instant].
 */
fun now(): Instant = Clock.System.now()

/**
 * Returns the current number of milliseconds since the Unix epoch.
 *
 * @return The current timestamp in milliseconds.
 */
fun epoch(): Long = now().toEpochMilliseconds()

/**
 * Marks a point in time on the [TimeSource.Monotonic] time source.
 *
 * @return The [TimeSource.Monotonic.ValueTimeMark] representing the marked point in time.
 */
fun markNow(): TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()

/**
 * Parses a string representation of an [Instant] into an [Instant] object.
 *
 * @return The parsed [Instant] object, or null if parsing fails.
 */
fun String.toInstant(): Instant? = runCatching { Instant.parse(input = this) }.getOrNull()

/**
 * Parses an epoch in milliseconds into an [Instant] object.
 *
 * @return The parsed [Instant] object, or null if parsing fails.
 */
fun Long.toInstant(): Instant? = runCatching { Instant.fromEpochMilliseconds(epochMilliseconds = this) }.getOrNull()
