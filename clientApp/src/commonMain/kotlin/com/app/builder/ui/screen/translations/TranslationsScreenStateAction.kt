package com.app.builder.ui.screen.translations

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.app.builder.ui.component.list.TranslationItem

/** Actions that can be dispatched to a translations store. */
sealed interface TranslationsScreenAction {
    /**
     * Selects the language whose translations are listed.
     *
     * @param languageIso ISO code of the selected language.
     */
    data class SelectLanguage(val languageIso: String): TranslationsScreenAction
}

/**
 * State of the translations screen, holding the available languages and the translations of the selected one.
 *
 * @property languages ISO codes of the languages holding translations, in tab order.
 * @property selectedLanguage ISO code of the language whose translations are listed, or `null` while there are no languages.
 * @property translations Translations of [selectedLanguage] currently displayed.
 */
data class TranslationsScreenState(
    val languages: ImmutableList<String> = persistentListOf(),
    val selectedLanguage: String? = null,
    val translations: ImmutableList<TranslationItem> = persistentListOf(),
)
