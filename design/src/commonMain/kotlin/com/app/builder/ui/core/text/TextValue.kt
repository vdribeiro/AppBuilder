package com.app.builder.ui.core.text

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.app.builder.ui.getDateTime
import com.app.builder.ui.getTranslation

/** A text that is turned into the string to display when it is drawn. */
@Stable
sealed interface TextValue {

    /** Whether there is nothing to display. */
    val isBlank: Boolean

    /**
     * A text displayed as given.
     *
     * @property text The text to display.
     */
    data class Plain(val text: String): TextValue {
        override val isBlank: Boolean get() = text.isBlank()
    }

    /**
     * A translation key, displayed as its translation in the app language.
     *
     * @property key The key mapping to a localized string, displayed as given when it has no translation.
     * @property args Arguments used to populate the positional placeholders of the translation.
     */
    data class Translation(val key: String, val args: ImmutableList<String> = persistentListOf()): TextValue {
        override val isBlank: Boolean get() = key.isBlank()
    }

    /**
     * A point in time, displayed as a localized date-time.
     *
     * @property utc The timestamp string in UTC ISO-8601 format.
     */
    data class DateTime(val utc: String): TextValue {
        override val isBlank: Boolean get() = utc.isBlank()
    }
}

/**
 * Resolves this text into the string to display.
 *
 * @receiver The text to resolve.
 * @return The text as given, its translation or its localized date-time.
 */
@Composable
fun TextValue.resolve(): String = when (this) {
    is TextValue.Plain -> text
    is TextValue.Translation -> getTranslation(key = key, args = args.toTypedArray())
    is TextValue.DateTime -> getDateTime(utc = utc)
}
