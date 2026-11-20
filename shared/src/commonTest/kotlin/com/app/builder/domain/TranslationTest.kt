package com.app.builder.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.app.builder.test.TestCase

class TranslationTest: TestCase() {

    /** Verifies a translations file decodes into one entry per key and language, in file order. */
    @Test
    fun decodeTranslations() = runUnitTest {
        val value = """
            {
              "common": {
                "yes": { "en": "Yes", "pt": "Sim" },
                "no": { "en": "No" }
              },
              "task": {
                "task": { "en": "Task", "pt": "Tarefa" }
              }
            }
        """.trimIndent()
        assertEquals(
            expected = listOf(
                Translation(languageIso = "en", key = "yes", value = "Yes"),
                Translation(languageIso = "pt", key = "yes", value = "Sim"),
                Translation(languageIso = "en", key = "no", value = "No"),
                Translation(languageIso = "en", key = "task", value = "Task"),
                Translation(languageIso = "pt", key = "task", value = "Tarefa")
            ),
            actual = decodeTranslations(value = value)
        )
    }

    /** Verifies a blank translations file decodes into no entries. */
    @Test
    fun decodeBlankTranslations() = runUnitTest {
        assertEquals(expected = emptyList(), actual = decodeTranslations(value = ""))
    }

    /** Verifies a translations file with an unexpected structure fails to decode. */
    @Test
    fun decodeMalformedTranslations() = runUnitTest {
        assertNull(actual = decodeTranslations(value = """[{"languageIso":"en","key":"yes","value":"Yes"}]"""))
    }
}
