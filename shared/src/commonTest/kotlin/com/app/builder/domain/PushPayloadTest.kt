package com.app.builder.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.data.serializer.encodeToJson
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class PushPayloadTest: TestCase() {

    /** Every [PushPayload] subtype paired with its expected serial name. */
    private val payloads: List<Pair<PushPayload, String>> = listOf(
        FakeData.notification to "notification",
        FakeData.entityNotification to "entity_notification",
        PushPayload.Flags() to "flags",
        PushPayload.Configs() to "configs",
        PushPayload.Reset() to "reset",
    )

    /** Verifies every subtype survives a polymorphic JSON round trip through the [PushPayload] type. */
    @Test
    fun polymorphicRoundTrip() = runUnitTest {
        payloads.forEach { (payload, _) ->
            assertEquals(
                expected = payload,
                actual = decode<PushPayload>(value = assertNotNull(actual = encode<PushPayload>(value = payload)))
            )
        }
    }

    /** Verifies every subtype is encoded with its expected type discriminator. */
    @Test
    fun typeDiscriminator() = runUnitTest {
        payloads.forEach { (payload, serialName) ->
            val element = assertNotNull(actual = encodeToJson<PushPayload>(value = payload))
            assertEquals(expected = serialName, actual = element.jsonObject.getValue(key = "type").jsonPrimitive.content)
        }
    }

    /** Verifies a payload is decoded into the subtype named by its discriminator, filling omitted defaults. */
    @Test
    fun decodeBySerialName() = runUnitTest {
        val uuid = "019a0000-0000-7000-8000-000000000000"
        val reset = assertIs<PushPayload.Reset>(value = decode<PushPayload>(value = """{"type":"reset","uuid":"$uuid"}"""))
        assertEquals(expected = uuid, actual = reset.uuid.toString())
        assertNull(actual = reset.userUuid)
        assertEquals(expected = "reset", actual = reset.title)
        assertEquals(expected = "", actual = reset.description)
    }

    /** Verifies decoding an unknown discriminator fails gracefully with null. */
    @Test
    fun decodeUnknownTypeReturnsNull() = runUnitTest {
        assertNull(actual = decode<PushPayload>(value = """{"type":"unknown","title":"t","description":"d"}"""))
    }

    /** Verifies the signal payloads default their titles to their own type names and their descriptions to empty. */
    @Test
    fun signalPayloadDefaults() = runUnitTest {
        assertEquals(expected = "flags", actual = PushPayload.Flags().title)
        assertEquals(expected = "configs", actual = PushPayload.Configs().title)
        assertEquals(expected = "reset", actual = PushPayload.Reset().title)
        listOf(PushPayload.Flags(), PushPayload.Configs(), PushPayload.Reset()).forEach { payload ->
            assertEquals(expected = "", actual = payload.description)
            assertNull(actual = payload.userUuid)
        }
    }
}
