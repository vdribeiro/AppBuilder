package com.app.builder.core.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class ServerFlagsTest: TestCase() {

    /** Verifies set applies the transformation, returns the updated flags and exposes them through the snapshot. */
    @Test
    fun setUpdatesSnapshot() = runUnitTest {
        val updated = ServerFlags.set { it.copy(tasks = false) }
        assertEquals(expected = false, actual = updated.tasks)
        assertEquals(expected = updated, actual = ServerFlags.flags)
    }

    /** Verifies set receives the current snapshot, so successive updates accumulate. */
    @Test
    fun setReceivesCurrentSnapshot() = runUnitTest {
        ServerFlags.set { it.copy(tasks = false) }
        var received: ServerFlags? = null
        ServerFlags.set { current -> received = current; current }
        assertEquals(expected = false, actual = received?.tasks)
    }

    /** Verifies reset restores and returns the default flags. */
    @Test
    fun resetRestoresDefaults() = runUnitTest {
        ServerFlags.set { it.copy(tasks = false) }
        assertEquals(expected = FakeData.serverFlags, actual = ServerFlags.reset())
        assertEquals(expected = FakeData.serverFlags, actual = ServerFlags.flags)
    }

    /** Verifies the flags survive a JSON round trip. */
    @Test
    fun serializationRoundTrip() = runUnitTest {
        val value = ServerFlags.flags.copy(tasks = false)
        assertEquals(
            expected = value,
            actual = decode(value = assertNotNull(actual = encode(value = value)))
        )
    }
}
