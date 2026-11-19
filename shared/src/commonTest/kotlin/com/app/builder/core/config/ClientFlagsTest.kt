package com.app.builder.core.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.test.TestCase

class ClientFlagsTest: TestCase() {

    /** Verifies set applies the transformation, returns the updated flags and exposes them through the snapshot. */
    @Test
    fun setUpdatesSnapshot() = runUnitTest {
        val updated = ClientFlags.set { it.copy(tasks = true) }
        assertEquals(expected = true, actual = updated.tasks)
        assertEquals(expected = updated, actual = ClientFlags.value)
    }

    /** Verifies set receives the current snapshot, so successive updates accumulate. */
    @Test
    fun setReceivesCurrentSnapshot() = runUnitTest {
        ClientFlags.set { it.copy(tasks = true) }
        var received: ClientFlags? = null
        ClientFlags.set { current -> received = current; current }
        assertEquals(expected = true, actual = received?.tasks)
    }

    /** Verifies reset restores and returns the default flags. */
    @Test
    fun resetRestoresDefaults() = runUnitTest {
        ClientFlags.set { it.copy(tasks = true) }
        assertEquals(expected = ClientFlags.default, actual = ClientFlags.reset())
        assertEquals(expected = ClientFlags.default, actual = ClientFlags.value)
    }

    /** Verifies off sets every flag to false and returns the updated flags. */
    @Test
    fun offDisablesEveryFlag() = runUnitTest {
        ClientFlags.reset()
        val off = ClientFlags.off()
        assertEquals(expected = off, actual = ClientFlags.value)
        assertFalse(actual = assertNotNull(actual = encode(value = off)).contains(other = "true"))
    }

    /** Verifies the flags survive a JSON round trip. */
    @Test
    fun serializationRoundTrip() = runUnitTest {
        val value = ClientFlags.value.copy(tasks = true)
        assertEquals(
            expected = value,
            actual = decode(value = assertNotNull(actual = encode(value = value)))
        )
    }
}
