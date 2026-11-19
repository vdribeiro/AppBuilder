package com.app.builder.ui.locale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import com.app.builder.core.locale.SelectedLanguage
import com.app.builder.core.locale.getLocalDateTime
import com.app.builder.test.TestCase

class DateTimeFormatterTest: TestCase() {

    /** A UTC timestamp whose short date differs between English and German. */
    private val utc = "2020-01-31T12:00:00Z"

    /** Verifies that the formatter formats in the conventions of the selected language. */
    @Test
    fun formatsInSelectedLanguage() = runUITest {
        SelectedLanguage.set { "de" }
        var formatter: ((String) -> String)? = null

        setUI { formatter = rememberDateTimeFormatter() }

        assertEquals(expected = getLocalDateTime(utc = utc, languageIso = "de"), actual = formatter?.invoke(utc))
    }

    /** Verifies that the formatter is kept across recompositions and replaced by one formatting in the new language when the selected language changes. */
    @Test
    fun replacedWhenSelectedLanguageChanges() = runUITest {
        SelectedLanguage.set { "en" }
        val formatters = mutableListOf<(String) -> String>()

        setUI { formatters.add(element = rememberDateTimeFormatter()) }
        val english = formatters.last()
        assertEquals(expected = getLocalDateTime(utc = utc, languageIso = "en"), actual = english(utc))

        SelectedLanguage.set { "en" }
        waitForIdle()
        assertSame(expected = english, actual = formatters.last())

        SelectedLanguage.set { "de" }
        waitUntil { formatters.last() !== english }
        assertNotSame(illegal = english, actual = formatters.last())
        assertEquals(expected = getLocalDateTime(utc = utc, languageIso = "de"), actual = formatters.last()(utc))
    }
}
