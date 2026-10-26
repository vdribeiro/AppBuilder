package com.app.builder.core.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class ClientFlagsTest: TestCase() {

    /** Verifies set applies the transformation, returns the updated flags and exposes them through the snapshot. */
    @Test
    fun setUpdatesSnapshot() = runUnitTest {
        val updated = ClientFlags.set { it.copy(tasks = false) }
        assertEquals(expected = false, actual = updated.tasks)
        assertEquals(expected = updated, actual = ClientFlags.flags)
    }

    /** Verifies set receives the current snapshot, so successive updates accumulate. */
    @Test
    fun setReceivesCurrentSnapshot() = runUnitTest {
        ClientFlags.set { it.copy(tasks = false) }
        var received: ClientFlags? = null
        ClientFlags.set { current -> received = current; current }
        assertEquals(expected = false, actual = received?.tasks)
    }

    /** Verifies reset restores and returns the default flags. */
    @Test
    fun resetRestoresDefaults() = runUnitTest {
        ClientFlags.set { it.copy(tasks = false) }
        assertEquals(expected = FakeData.clientFlags, actual = ClientFlags.reset())
        assertEquals(expected = FakeData.clientFlags, actual = ClientFlags.flags)
    }

    /** Verifies the flags survive a JSON round trip. */
    @Test
    fun serializationRoundTrip() = runUnitTest {
        val value = ClientFlags.flags.copy(tasks = false)
        assertEquals(
            expected = value,
            actual = decode(value = assertNotNull(actual = encode(value = value)))
        )
    }
}
