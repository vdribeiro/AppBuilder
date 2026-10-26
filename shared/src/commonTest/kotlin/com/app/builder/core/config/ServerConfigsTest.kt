package com.app.builder.core.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class ServerConfigsTest: TestCase() {

    /** Verifies set applies the transformation, returns the updated configs and exposes them through the snapshot. */
    @Test
    fun setUpdatesSnapshot() = runUnitTest {
        val updated = ServerConfigs.set { it.copy(maxPageSize = 1) }
        assertEquals(expected = 1, actual = updated.maxPageSize)
        assertEquals(expected = updated, actual = ServerConfigs.configs)
    }

    /** Verifies set receives the current snapshot, so successive updates accumulate. */
    @Test
    fun setReceivesCurrentSnapshot() = runUnitTest {
        ServerConfigs.set { it.copy(maxPageSize = 1) }
        var received: ServerConfigs? = null
        ServerConfigs.set { current -> received = current; current }
        assertEquals(expected = 1, actual = received?.maxPageSize)
    }

    /** Verifies reset restores and returns the default configs. */
    @Test
    fun resetRestoresDefaults() = runUnitTest {
        ServerConfigs.set { it.copy(maxPageSize = 1) }
        assertEquals(expected = FakeData.serverConfigs, actual = ServerConfigs.reset())
        assertEquals(expected = FakeData.serverConfigs, actual = ServerConfigs.configs)
    }

    /** Verifies the configs survive a JSON round trip. */
    @Test
    fun serializationRoundTrip() = runUnitTest {
        val value = ServerConfigs.configs.copy(maxPageSize = 1)
        assertEquals(
            expected = value,
            actual = decode(value = assertNotNull(actual = encode(value = value)))
        )
    }
}
