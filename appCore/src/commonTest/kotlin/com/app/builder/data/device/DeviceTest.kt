package com.app.builder.data.device

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.app.builder.data.storage.CoreFile
import com.app.builder.test.TestCase

class DeviceTest: TestCase() {

    /** Verifies a device UUID is generated and persisted when none exists yet. */
    @Test
    fun generatesAndPersistsUuidWhenMissing() = runUnitTest {
        assertNull(actual = CoreFile.Device.cache().value.orEmpty()[CoreFile.Device.Key.UUID])
        val uuid = getDeviceUuid()
        assertEquals(expected = uuid.toString(), actual = CoreFile.Device.cache().value.orEmpty()[CoreFile.Device.Key.UUID])
    }

    /** Verifies subsequent calls return the same persisted device UUID. */
    @Test
    fun returnsSameUuidOnSubsequentCalls() = runUnitTest {
        val first = getDeviceUuid()
        val second = getDeviceUuid()

        assertEquals(expected = first, actual = second)
    }

    /** Verifies that a corrupted persisted UUID string is discarded, a new UUID is generated and persisted in its place. */
    @Test
    fun regeneratesUuidWhenPersistedValueIsCorrupted() = runUnitTest {
        CoreFile.Device.save { it.orEmpty().plus(pair = (CoreFile.Device.Key.UUID to "not-a-valid-uuid")) }

        val uuid = getDeviceUuid()

        assertEquals(expected = uuid.toString(), actual = CoreFile.Device.cache().value.orEmpty()[CoreFile.Device.Key.UUID])
    }
}
