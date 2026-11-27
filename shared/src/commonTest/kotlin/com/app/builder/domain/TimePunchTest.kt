package com.app.builder.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import com.app.builder.core.locale.now
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TimePunchTest: TestCase() {

    /** Verifies a default time punch is an undeleted manual punch at the current time with a fresh uuid. */
    @Test
    fun defaults() = runUnitTest {
        val timePunch = TimePunch(userUuid = FakeData.user.uuid)
        assertEquals(expected = now(), actual = timePunch.modifiedAt)
        assertNull(actual = timePunch.deletedAt)
        assertEquals(expected = now(), actual = timePunch.time)
        assertEquals(expected = TimePunch.Source.MANUAL, actual = timePunch.source)
        assertNotEquals(illegal = timePunch.uuid, actual = TimePunch(userUuid = FakeData.user.uuid).uuid)
    }

    /** Verifies a time punch survives a JSON round trip. */
    @Test
    fun serializationRoundTrip() = runUnitTest {
        val timePunch = FakeData.timePunch.copy(source = TimePunch.Source.NFC, deletedAt = now())
        assertEquals(
            expected = timePunch,
            actual = decode(value = assertNotNull(actual = encode(value = timePunch)))
        )
    }

    /** Verifies a time punch decodes from JSON holding only its uuid and user uuid, filling every other field from its defaults. */
    @Test
    fun decodeFillsDefaults() = runUnitTest {
        val uuid = FakeData.timePunch.uuid
        val userUuid = FakeData.timePunch.userUuid
        val timePunch = assertNotNull(actual = decode<TimePunch>(value = """{"uuid":"$uuid","userUuid":"$userUuid"}"""))
        assertEquals(expected = uuid, actual = timePunch.uuid)
        assertEquals(expected = userUuid, actual = timePunch.userUuid)
        assertEquals(expected = now(), actual = timePunch.time)
        assertEquals(expected = TimePunch.Source.MANUAL, actual = timePunch.source)
    }
}
