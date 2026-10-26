package com.app.builder.core.nfc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.data.serializer.json
import com.app.builder.domain.EntityType
import com.app.builder.test.TestCase

class NfcTagTest: TestCase() {

    /** Verifies that every [NfcRecord] is written with its serial name as the type discriminator, which is the format stored on physical tags. */
    @Test
    fun recordsAreEncodedWithTheirSerialNameAsType() = runUnitTest {
        val records: Map<String, NfcRecord> = mapOf(
            "open" to NfcRecord.Open(entityType = EntityType.TASK),
            "upsert" to NfcRecord.Upsert(entityType = EntityType.TASK),
        )

        records.forEach { (type, record) ->
            val encoded = encode<NfcRecord>(value = record).orEmpty()
            assertEquals(expected = type, actual = json.parseToJsonElement(string = encoded).jsonObject["type"]?.jsonPrimitive?.content)
        }
    }

    /** Verifies that every [NfcRecord] variant, with and without an entity uuid, round trips through encoding and decoding. */
    @Test
    fun recordsRoundTrip() = runUnitTest {
        val uuid = Uuid.random()
        val records = listOf(
            NfcRecord.Open(entityType = EntityType.TASK),
            NfcRecord.Open(entityType = EntityType.USER, entityUuid = uuid),
            NfcRecord.Upsert(entityType = EntityType.TASK),
            NfcRecord.Upsert(entityType = EntityType.DEVICE_LOCATION, entityUuid = uuid),
        )

        records.forEach { record ->
            assertEquals(expected = record, actual = decode<NfcRecord>(value = encode<NfcRecord>(value = record).orEmpty()))
        }
    }

    /** Verifies that a tag payload written in the stored wire format decodes into the matching record. */
    @Test
    fun wireFormatPayloadDecodesIntoRecord() = runUnitTest {
        val uuid = Uuid.random()

        assertEquals(
            expected = NfcRecord.Upsert(entityType = EntityType.TASK, entityUuid = uuid),
            actual = decode<NfcRecord>(value = """{"type":"upsert","entityType":"TASK","entityUuid":"$uuid"}""")
        )
        assertEquals(
            expected = NfcRecord.Open(entityType = EntityType.USER),
            actual = decode<NfcRecord>(value = """{"type":"open","entityType":"USER"}""")
        )
    }

    /** Verifies that a payload with an unknown record type decodes to null instead of throwing. */
    @Test
    fun unknownRecordTypeDecodesToNull() = runUnitTest {
        assertNull(actual = decode<NfcRecord>(value = """{"type":"delete","entityType":"TASK"}"""))
    }
}
