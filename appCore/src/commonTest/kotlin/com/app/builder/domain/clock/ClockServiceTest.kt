package com.app.builder.domain.clock

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import io.ktor.client.engine.mock.MockEngineConfig
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
    private fun clockService(): ClockService = ClockManager(
        httpClient = HttpClientFactory(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { respondMock() }
        })).httpClient
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
}
