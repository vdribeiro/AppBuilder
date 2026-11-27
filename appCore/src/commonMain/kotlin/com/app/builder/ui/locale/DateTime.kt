package com.app.builder.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.locale.SelectedLanguage
import com.app.builder.core.locale.getLocalDate
import com.app.builder.core.locale.getLocalDateTime
import com.app.builder.core.locale.getLocalTime
import com.app.builder.ui.DateTimeMode

/**
 * Remembers a function formatting the parts of a UTC ISO-8601 string named by a [DateTimeMode] into a localized string in the conventions of the [SelectedLanguage], replaced by a new one when the selected language changes.
 *
 * @return The formatting function.
 */
@Composable
fun rememberDateTimeFormatter(): (String, DateTimeMode) -> String {
    val selectedLanguage by SelectedLanguage.flow.collectAsStateWithLifecycle()
    return remember(key1 = selectedLanguage) {
        { utc, mode ->
            when (mode) {
                DateTimeMode.DATE_TIME -> getLocalDateTime(utc = utc, languageIso = selectedLanguage)
                DateTimeMode.DATE -> getLocalDate(utc = utc, languageIso = selectedLanguage)
                DateTimeMode.TIME -> getLocalTime(utc = utc, languageIso = selectedLanguage)
            }
        }
    }
}
