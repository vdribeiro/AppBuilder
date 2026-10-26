package com.app.builder.domain.clock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import com.app.builder.core.config.ClientConfigs
import com.app.builder.data.http.HttpClientFactory
import com.app.builder.data.http.TestEngine
import com.app.builder.data.http.respondMock
import com.app.builder.data.storage.CoreFile
import com.app.builder.test.TestCase

class ClockServiceTest: TestCase() {

    /**
     * Builds a [ClockService] backed by a mock HTTP engine for testing.
     *
     * @return The fixture [ClockService] instance.
     */
    private fun clockService(onProbe: suspend () -> Unit = {}): ClockService = ClockManager(
        httpClient = HttpClientFactory(engine = TestEngine {
            onProbe()
            respondMock()
        }).httpClient
    )

    /** Verifies the clock is trusted when the offset is within tolerance. */
    @Test
    fun trustedWhenOffsetWithinTolerance() = runUnitTest {
        val clockService = clockService()

        clockService.start()
        CoreFile.Device.save { it.orEmpty().plus(pair = (CoreFile.Device.Key.CLOCK_OFFSET to 1_000.milliseconds.toString())) }
        assertTrue(actual = clockService.trusted.value)
        clockService.stop()
    }

    /** Verifies the clock is untrusted when the offset exceeds tolerance. */
    @Test
    fun untrustedWhenOffsetExceedsTolerance() = runUnitTest {
        val clockService = clockService()

        clockService.start()
        CoreFile.Device.save { it.orEmpty().plus(pair = (CoreFile.Device.Key.CLOCK_OFFSET to 120_000.milliseconds.toString())) }
        assertFalse(actual = clockService.trusted.value)
        clockService.stop()
    }

    /** Verifies the clock becomes trusted again once the offset returns within tolerance. */
    @Test
    fun trustedRecoversWhenOffsetReturnsWithinTolerance() = runUnitTest {
        val clockService = clockService()

        clockService.start()
        CoreFile.Device.save { it.orEmpty().plus(pair = (CoreFile.Device.Key.CLOCK_OFFSET to 120_000.milliseconds.toString())) }
        assertFalse(actual = clockService.trusted.value)

        CoreFile.Device.save { it.orEmpty().plus(pair = (CoreFile.Device.Key.CLOCK_OFFSET to 1_000.milliseconds.toString())) }
        assertTrue(actual = clockService.trusted.value)
        clockService.stop()
    }

    /** Verifies the probe loop issues no further requests once the service is stopped. */
    @Test
    fun probingStopsAfterStop() = runUnitTest {
        val probes = MutableStateFlow(value = 0)
        val clockService = clockService(onProbe = { probes.update { it + 1 } })

        clockService.start()
        // The mock engine answers off the test scheduler, so wait for the first probe rather than advancing virtual time
        probes.first { it > 0 }

        clockService.stop()
        val probesAtStop = probes.value
        testScheduler.advanceTimeBy(delayTimeMillis = ClientConfigs.configs.clockProbeInterval * 5)
        assertEquals(expected = probesAtStop, actual = probes.value)
    }

    /** Verifies the clock is untrusted until an offset has been recorded. */
    @Test
    fun untrustedWithoutRecordedOffset() = runUnitTest {
        val clockService = clockService()

        clockService.start()
        assertFalse(actual = clockService.trusted.value)
        clockService.stop()
    }

    /** Verifies the clock loses its trust when the recorded offset becomes unparseable. */
    @Test
    fun untrustedWhenOffsetIsCorrupted() = runUnitTest {
        val clockService = clockService()

        clockService.start()
        CoreFile.Device.save { it.orEmpty().plus(pair = (CoreFile.Device.Key.CLOCK_OFFSET to 1_000.milliseconds.toString())) }
        assertTrue(actual = clockService.trusted.value)

        CoreFile.Device.save { it.orEmpty().plus(pair = (CoreFile.Device.Key.CLOCK_OFFSET to "corrupted")) }
        assertFalse(actual = clockService.trusted.value)
        clockService.stop()
    }

    /** Verifies the probe loop stops issuing requests once a probe's recalibration makes the clock trusted. */
    @Test
    fun probingStopsOnceTheClockIsTrusted() = runUnitTest {
        val probes = MutableStateFlow(value = 0)
        val clockService = clockService(onProbe = {
            probes.update { it + 1 }
            CoreFile.Device.save { it.orEmpty().plus(pair = (CoreFile.Device.Key.CLOCK_OFFSET to 0.milliseconds.toString())) }
        })

        clockService.start()
        clockService.trusted.first { it }

        testScheduler.advanceTimeBy(delayTimeMillis = ClientConfigs.configs.clockProbeInterval * 5)
        assertEquals(expected = 1, actual = probes.value)
        clockService.stop()
    }
}
