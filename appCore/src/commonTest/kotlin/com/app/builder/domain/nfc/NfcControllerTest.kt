package com.app.builder.domain.nfc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.nfc.NfcController
import com.app.builder.core.nfc.NfcRecord
import com.app.builder.core.nfc.createNfcController
import com.app.builder.domain.EntityType
import com.app.builder.test.TestCase

class NfcControllerTest: TestCase() {

    /** A filler [NfcRecord] used only to exercise the write path; its content is irrelevant to these tests. */
    private val fillerRecord = NfcRecord.Open(entityType = EntityType.TASK)

    /** A test fixture [NfcController] that records every invoked action as an event. */
    private class RecordingNfcController: NfcController() {
        /** The events recorded so far by this test fixture. */
        val events = mutableListOf<String>()

        override val available: Boolean = true

        override fun platformRead() {
            events.add(element = "read")
        }

        override fun platformWrite(records: List<NfcRecord>) {
            events.add(element = "write")
        }

        override fun platformStop() {
            events.add(element = "stop")
        }

        override fun platformDispose() {
            events.add(element = "dispose")
        }
    }

    /** Verifies that a new [NfcController] starts in the idle state. */
    @Test
    fun initialStateIsIdle() = runUnitTest {
        val reader = RecordingNfcController()
        assertEquals(expected = NfcController.State.Idle, actual = reader.state.value)
    }

    /** Verifies that starting a read transitions the reader to scanning. */
    @Test
    fun readTransitionsToScanning() = runUnitTest {
        val reader = RecordingNfcController()

        reader.read()

        assertEquals(expected = listOf("stop", "read"), actual = reader.events)
        assertEquals(expected = NfcController.State.Scanning, actual = reader.state.value)
    }

    /** Verifies that stopping the session transitions the reader back to idle. */
    @Test
    fun stopTransitionsToIdle() = runUnitTest {
        val reader = RecordingNfcController()
        reader.read()
        reader.events.clear()

        reader.stop()

        assertEquals(expected = listOf("stop"), actual = reader.events)
        assertEquals(expected = NfcController.State.Idle, actual = reader.state.value)
    }

    /** Verifies that requesting a write transitions the reader to writing. */
    @Test
    fun writeTransitionsToWriting() = runUnitTest {
        val reader = RecordingNfcController()

        reader.write(records = listOf(fillerRecord))

        assertEquals(expected = listOf("stop", "write"), actual = reader.events)
        assertEquals(expected = NfcController.State.Writing, actual = reader.state.value)
    }

    /** Verifies that requesting a write while scanning stops the scan first. */
    @Test
    fun writeStopsAnActiveScanBeforeWriting() = runUnitTest {
        val reader = RecordingNfcController()
        reader.read()
        reader.events.clear()

        reader.write(records = listOf(fillerRecord))

        assertEquals(expected = listOf("stop", "write"), actual = reader.events)
        assertEquals(expected = NfcController.State.Writing, actual = reader.state.value)
    }

    /** Verifies that disabling the NFC feature flag forces the reader back to idle. */
    @Test
    fun disabledFlagForcesIdle() = runUnitTest {
        val reader = RecordingNfcController()
        reader.read()
        assertEquals(expected = NfcController.State.Scanning, actual = reader.state.value)

        ClientFlags.set { it.copy(nfc = false) }
        reader.read()

        assertEquals(expected = NfcController.State.Idle, actual = reader.state.value)
    }

    /** Verifies that an unavailable controller ignores read and write requests. */
    @Test
    fun unavailableControllerIgnoresActions() = runUnitTest {
        val reader = NfcController()

        reader.read()
        reader.write(records = listOf(fillerRecord))

        assertEquals(expected = NfcController.State.Idle, actual = reader.state.value)
    }

    /** Verifies that disposing the controller stops the session and releases platform resources. */
    @Test
    fun disposeStopsSessionAndReleasesPlatformResources() = runUnitTest {
        val reader = RecordingNfcController()
        reader.read()
        reader.events.clear()

        reader.dispose()

        assertEquals(expected = listOf("stop", "dispose"), actual = reader.events)
        assertEquals(expected = NfcController.State.Idle, actual = reader.state.value)
    }

    /** Verifies that an exception thrown by the platform's read call is caught and force-stops the session. */
    @Test
    fun exceptionFromPlatformReadForceStopsSession() = runUnitTest {
        val reader = object: NfcController() {
            override val available: Boolean = true
            override fun platformRead() {
                throw IllegalStateException("boom")
            }
        }

        reader.read()

        assertEquals(expected = NfcController.State.Idle, actual = reader.state.value)
    }

    /** Verifies that an exception thrown by the platform's write call is caught and force-stops the session. */
    @Test
    fun exceptionFromPlatformWriteForceStopsSession() = runUnitTest {
        val reader = object: NfcController() {
            override val available: Boolean = true
            override fun platformWrite(records: List<NfcRecord>) {
                throw IllegalStateException("boom")
            }
        }

        reader.write(records = listOf(fillerRecord))

        assertEquals(expected = NfcController.State.Idle, actual = reader.state.value)
    }

    /** Verifies that the platform NFC controller handles every call without throwing. */
    @Test
    fun platformNfcControllerHandlesEveryCallWithoutThrowing() = runUnitTest {
        val reader = createNfcController()

        reader.read()
        reader.write(records = listOf(fillerRecord))
        reader.stop()
        reader.dispose()
    }

    /** Verifies that [NfcController.create] produces a usable controller instance. */
    @Test
    fun createProducesAUsableController() = runUnitTest {
        val reader = NfcController.create()

        assertNotNull(actual = reader)
        assertEquals(expected = NfcController.State.Idle, actual = reader.state.value)
    }
}
