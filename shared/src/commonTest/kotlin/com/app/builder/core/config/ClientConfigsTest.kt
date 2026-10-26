package com.app.builder.core.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class ClientConfigsTest: TestCase() {

    /** Verifies set applies the transformation, returns the updated configs and exposes them through the snapshot. */
    @Test
    fun setUpdatesSnapshot() = runUnitTest {
        val updated = ClientConfigs.set { it.copy(batchSize = 1) }
        assertEquals(expected = 1, actual = updated.batchSize)
        assertEquals(expected = updated, actual = ClientConfigs.configs)
    }

    /** Verifies set receives the current snapshot, so successive updates accumulate. */
    @Test
    fun setReceivesCurrentSnapshot() = runUnitTest {
        ClientConfigs.set { it.copy(batchSize = 1) }
        var received: ClientConfigs? = null
        ClientConfigs.set { current -> received = current; current }
        assertEquals(expected = 1, actual = received?.batchSize)
    }

    /** Verifies reset restores and returns the default configs. */
    @Test
    fun resetRestoresDefaults() = runUnitTest {
        ClientConfigs.set { it.copy(batchSize = 1) }
        assertEquals(expected = FakeData.clientConfigs, actual = ClientConfigs.reset())
        assertEquals(expected = FakeData.clientConfigs, actual = ClientConfigs.configs)
    }

    /** Verifies the configs survive a JSON round trip. */
    @Test
    fun serializationRoundTrip() = runUnitTest {
        val value = ClientConfigs.configs.copy(batchSize = 1)
        assertEquals(
            expected = value,
            actual = decode(value = assertNotNull(actual = encode(value = value)))
        )
    }
}
