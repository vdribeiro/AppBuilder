package com.app.builder.core.locale

import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import com.app.builder.core.locale.DateTime.clock
import com.app.builder.core.locale.DateTime.timeSource

/** Date and time sources. */
object DateTime {
    /** The source of [Instant] values. */
    @Volatile
    var clock: Clock = Clock.System

    /** The source of time for measuring time intervals. */
    @Volatile
    var timeSource: TimeSource = TimeSource.Monotonic
}

/** The [Instant] of the Unix epoch, `1970-01-01T00:00:00Z`. */
val Instant.Companion.UNIX_EPOCH: Instant
    get() = fromEpochSeconds(epochSeconds = 0)

/**
 * Returns the current time [Instant].
 *
 * @return The current [Instant].
 */
fun now(): Instant = clock.now()

/**
 * Returns the current number of milliseconds since the Unix epoch.
 *
 * @return The current timestamp in milliseconds.
 */
fun epoch(): Long = now().toEpochMilliseconds()

/**
 * Marks a point in time on the [timeSource].
 *
 * @return The [TimeMark] representing the marked point in time.
 */
fun markNow(): TimeMark = timeSource.markNow()

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


