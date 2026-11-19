package com.app.builder.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.locale.SelectedLanguage
import com.app.builder.core.locale.getLocalDateTime

/**
 * Remembers a function formatting a UTC ISO-8601 string into a localized date-time string in the conventions of the [SelectedLanguage], replaced by a new one when the selected language changes.
 *
 * @return The formatting function.
 */
@Composable
fun rememberDateTimeFormatter(): (String) -> String {
    val selectedLanguage by SelectedLanguage.flow.collectAsStateWithLifecycle()
    return remember(key1 = selectedLanguage) { { utc -> getLocalDateTime(utc = utc, languageIso = selectedLanguage) } }
}
