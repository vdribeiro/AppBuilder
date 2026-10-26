package com.app.builder.domain.gateway.translation

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class MappersTest: TestCase() {

    /** Verifies that every translation survives a round trip through its database schema. */
    @Test
    fun translationRoundTrip() = runUnitTest {
        FakeData.translations.forEach { translation ->
            val schema = translation.toTranslationSchema()

            assertEquals(expected = translation.value, actual = schema.value_)
            assertEquals(expected = translation, actual = schema.toTranslation())
        }
    }
}
