package com.app.builder.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.app.builder.data.serializer.decode
import com.app.builder.test.TestCase

class ResourceTest: TestCase() {

    /** Verifies that reading an existing resource returns its text. */
    @Test
    fun readResource() = runServerTest {
        assertTrue(actual = readResource(path = "static/translations.json").orEmpty().isNotBlank())
    }

    /** Verifies that every key of the translations resource starts with the name of its group, belongs to one group only and has a value in every language. */
    @Test
    fun translationsResource() = runServerTest {
        val groups = assertNotNull(
            actual = decode<Map<String, Map<String, Map<String, String>>>>(value = readResource(path = "static/translations.json").orEmpty())
        )
        val languages = groups.values.flatMap { keys -> keys.values.flatMap { it.keys } }.toSet()
        assertTrue(actual = languages.isNotEmpty())

        groups.forEach { (group, keys) ->
            keys.forEach { (key, values) ->
                assertTrue(actual = key.startsWith(prefix = "${group}_"), message = key)
                assertEquals(expected = languages, actual = values.keys, message = key)
            }
        }
        val keys = groups.values.flatMap { it.keys }
        assertEquals(expected = keys.distinct(), actual = keys)
    }

    /** Verifies that reading a missing resource returns null. */
    @Test
    fun readMissingResource() = runServerTest {
        assertNull(actual = readResource(path = "static/missing.json"))
    }
}
