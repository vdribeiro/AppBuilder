package com.app.builder.core.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.test.TestCase

class ServerFlagsTest: TestCase() {

    /** Verifies set applies the transformation, returns the updated flags and exposes them through the snapshot. */
    @Test
    fun setUpdatesSnapshot() = runUnitTest {
        val updated = ServerFlags.set { it.copy(tasks = true) }
        assertEquals(expected = true, actual = updated.tasks)
        assertEquals(expected = updated, actual = ServerFlags.value)
    }

    /** Verifies set receives the current snapshot, so successive updates accumulate. */
    @Test
    fun setReceivesCurrentSnapshot() = runUnitTest {
        ServerFlags.set { it.copy(tasks = true) }
        var received: ServerFlags? = null
        ServerFlags.set { current -> received = current; current }
        assertEquals(expected = true, actual = received?.tasks)
    }

    /** Verifies reset restores and returns the initial flags. */
    @Test
    fun resetRestoresDefaults() = runUnitTest {
        ServerFlags.set { it.copy(tasks = true) }
        assertEquals(expected = ServerFlags.initialValue, actual = ServerFlags.reset())
        assertEquals(expected = ServerFlags.initialValue, actual = ServerFlags.value)
    }

    /** Verifies off sets every flag to false and returns the updated flags. */
    @Test
    fun offDisablesEveryFlag() = runUnitTest {
        ServerFlags.reset()
        val off = ServerFlags.off()
        assertEquals(expected = off, actual = ServerFlags.value)
        assertFalse(actual = assertNotNull(actual = encode(value = off)).contains(other = "true"))
    }

    /** Verifies the flags survive a JSON round trip. */
    @Test
    fun serializationRoundTrip() = runUnitTest {
        val value = ServerFlags.value.copy(tasks = true)
        assertEquals(
            expected = value,
            actual = decode(value = assertNotNull(actual = encode(value = value)))
        )
    }
}
