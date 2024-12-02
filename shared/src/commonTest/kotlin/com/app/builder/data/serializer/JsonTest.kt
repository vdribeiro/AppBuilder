package com.app.builder.data.serializer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonPrimitive
import com.app.builder.domain.Translation
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class JsonTest: TestCase() {

    /** Verifies a value encoded via encode can be decoded back into an equal value using reified type inference. */
    @Test
    fun encodeDecode() = runUnitTest {
        assertEquals(
            expected = FakeData.translations,
            actual = decode(value = assertNotNull(actual = encode(value = FakeData.translations)))
        )
    }

    /** Verifies a value encoded with an explicit serializer can be decoded back into an equal value. */
    @Test
    fun encodeDecodeSerializer() = runUnitTest {
        val serializer = Translation.serializer()
        val translation = FakeData.translations.first()
        assertEquals(
            expected = translation,
            actual = decode(serializer = serializer, value = assertNotNull(actual = encode(serializer = serializer, value = translation)))
        )
    }

    /** Verifies decode returns null when given malformed JSON. */
    @Test
    fun decodeInvalidReturnsNull() = runUnitTest {
        assertNull(actual = decode<Translation>(value = "{ not json }"))
    }

    /** Verifies decode ignores unknown JSON keys not present in the target type. */
    @Test
    fun decodeIgnoresUnknownKeys() = runUnitTest {
        val encoded = """{"languageIso":"en","key":"key","value":"value","unknown":"field"}"""
        assertEquals(
            expected = Translation(languageIso = "en", key = "key", value = "value"),
            actual = decode(value = encoded)
        )
    }

    /** Verifies decode falls back to an empty collection or map when given a blank string. */
    @Test
    fun decodeBlankFallback() = runUnitTest {
        assertEquals(expected = emptyMap(), actual = decode<Map<String, String>>(value = " "))
        assertEquals(expected = emptyList(), actual = decode<List<Translation>>(value = " "))
        assertEquals(expected = emptySet(), actual = decode<Set<Translation>>(value = " "))
    }

    /** Verifies decode with an explicit serializer falls back to an empty list for a blank string. */
    @Test
    fun decodeSerializerBlankFallback() = runUnitTest {
        assertEquals(
            expected = emptyList(),
            actual = decode(serializer = ListSerializer(Translation.serializer()), value = " ")
        )
    }

    /** Verifies encode and encodeToJson return null when serialization genuinely fails, such as encoding an unsupported special floating point value. */
    @Test
    fun encodeReturnsNullOnSerializationFailure() = runUnitTest {
        assertNull(actual = encode(value = Double.NaN))
        assertNull(actual = encodeToJson(value = Double.NaN))
    }

    /** Verifies a value encoded via encodeToJson can be decoded back into an equal value using decodeFromJson. */
    @Test
    fun encodeToJsonDecodeFromJson() = runUnitTest {
        assertEquals(
            expected = FakeData.translations,
            actual = decodeFromJson(value = assertNotNull(actual = encodeToJson(value = FakeData.translations)))
        )
    }

    /** Verifies decodeFromJson returns null when the JsonElement does not match the target type. */
    @Test
    fun decodeFromJsonInvalidReturnsNull() = runUnitTest {
        assertNull(actual = decodeFromJson<Translation>(value = JsonPrimitive("not an object")))
    }
}
