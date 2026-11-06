package com.app.builder.ui.screen.translations

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.DEFAULT_LANGUAGE
import com.app.builder.core.locale.observeLocale
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Translation
import com.app.builder.domain.Translation.Property
import com.app.builder.domain.gateway.translation.TranslationUseCases
import com.app.builder.ui.component.list.TranslationItem
import com.app.builder.ui.store.Store

/**
 * Store backing the translations screen, listing the available languages and combining the translations of the selected one with the shared action bar state to filter and sort them.
 *
 * @param state Initial translations state.
 * @property translationUseCases Use cases used to observe languages and translations.
 * @property locale Emits the device's ISO 639-1 language code, selected first when it holds translations.
 */
class TranslationsScreenStore(
    state: TranslationsScreenState,
    private val translationUseCases: TranslationUseCases,
    private val locale: Flow<String> = observeLocale()
): Store<TranslationsScreenState, TranslationsScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: TranslationsScreenState, action: TranslationsScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is TranslationsScreenAction.SelectLanguage -> selectLanguage(action = action)
        }
    }

    /**
     * Observes the languages, keeping a language selected, and the translations of the selected language.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        combine(
            flow = translationUseCases.observeLanguages(),
            flow2 = locale
        ) { languages, locale ->
            languages to locale
        }.observe(id = "languages") { (languages, locale) ->
            updateState { state ->
                val selectedLanguage = state.selectedLanguage?.takeIf { it in languages }
                    ?: locale.takeIf { it in languages }
                    ?: DEFAULT_LANGUAGE.takeIf { it in languages }
                    ?: languages.firstOrNull()
                state.copy(languages = languages.toPersistentList(), selectedLanguage = selectedLanguage)
            }
        }

        val translationsFlow = stateFlow
            .map { it.selectedLanguage }
            .distinctUntilChanged()
            .flatMapLatest { languageIso ->
                languageIso?.let { translationUseCases.observeTranslations(languageIso = it) } ?: flowOf(value = emptyList())
            }
        val actionBarDataFlow = AppFile.TranslationPreferences.cache()
            .mapNotNull { it }
            .distinctUntilChanged()

        combine(
            flow = translationsFlow,
            flow2 = actionBarDataFlow,
        ) { translations, actionBarData ->
            val comparator = translationComparator(sortProperty = actionBarData.sortProperty, sortAscending = actionBarData.sortAscending)
            translations
                .filter { it.matchesSearch(search = actionBarData.search, searchableProperties = actionBarData.searchableProperties) }
                .let { if (comparator == null) it else it.sortedWith(comparator = comparator) }
                .map { it.toTranslationItem(visibilityProperties = actionBarData.visibleProperties) }
                .toPersistentList()
        }
            .flowOn(context = Dispatcher.Default)
            .observe(id = "filterTranslations") { translations ->
                updateState { it.copy(translations = translations) }
            }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Selects the language whose translations are listed.
     *
     * @param action Action carrying the ISO code of the selected language.
     * @return The [Job] representing this execution.
     */
    private fun selectLanguage(action: TranslationsScreenAction.SelectLanguage): Job = launch(id = "selectLanguage") {
        updateState { it.copy(selectedLanguage = action.languageIso) }
    }

    /**
     * Gets the displayed text of the given [property].
     *
     * @param property Name of the [Property] to read.
     * @return The property's text, or `null` if it doesn't match a known [Property].
     */
    private fun Translation.text(property: String): String? = when (property) {
        Property.KEY.name -> key
        Property.VALUE.name -> value
        else -> null
    }

    /**
     * Checks whether this translation's searchable properties contain the given [search] text.
     *
     * @param search Search text to look for; an empty or blank value always matches.
     * @param searchableProperties Names of the [Property] values to search within.
     * @return `true` if any of the searchable properties contains [search], ignoring case.
     */
    private fun Translation.matchesSearch(search: String, searchableProperties: List<String>): Boolean =
        search.isBlank() || searchableProperties.any { text(property = it)?.contains(other = search, ignoreCase = true) == true }

    /**
     * Builds a comparator for the given [sortProperty], reversed when [sortAscending] is `false`.
     *
     * @param sortProperty Name of the [Property] to sort by.
     * @param sortAscending Whether the comparator should sort in ascending order.
     * @return Comparator for the given property, or `null` if it doesn't match a known [Property].
     */
    private fun translationComparator(sortProperty: String, sortAscending: Boolean): Comparator<Translation>? {
        val comparator = when (sortProperty) {
            Property.KEY.name -> compareBy<Translation> { it.key }
            Property.VALUE.name -> compareBy { it.value }
            else -> return null
        }
        return if (sortAscending) comparator else comparator.reversed()
    }

    /**
     * Converts this translation into a [TranslationItem], including only the properties named in [visibilityProperties].
     *
     * @param visibilityProperties Names of the [Property] values that should be visible on the item.
     * @return Translation item with hidden properties set to `null`.
     */
    private fun Translation.toTranslationItem(visibilityProperties: List<String>): TranslationItem = TranslationItem(
        id = key,
        key = key.takeIf { Property.KEY.name in visibilityProperties },
        value = value.takeIf { Property.VALUE.name in visibilityProperties },
    )

    companion object {
        private const val TAG = "TranslationsScreenStore"
    }
}
