package com.app.builder.ui.navigation.translations

import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Translation
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.lifecycle.hostViewModel
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.hostSessionStore

/**
 * The translations route.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.translationsProvider(useCases: UseCases) {
    entry<Screen.Translations> {
        TranslationsScreen(
            sessionStore = hostSessionStore(useCases = useCases),
            filtersStore = hostViewModel(key = FILTERS_KEY) { filtersStore() },
            store = viewModel { TranslationsScreenStore(state = TranslationsScreenState(), translationUseCases = useCases.translationUseCases) },
        )
    }
}

/** The key of the translation filters among the stores shared by the navigation host. */
private const val FILTERS_KEY = "translation_filters"
/** The translation properties by name. */
private val properties = Translation.Property.entries.map { it.name }.toPersistentList()
/** The default translation filters. */
private val defaults = AppFile.ActionBarData(
    search = "",
    searchableProperties = properties,
    sortAscending = true,
    sortProperty = Translation.Property.KEY.name,
    visibleProperties = properties,
)

/**
 * Creates the translation filters store.
 *
 * @return The translation filters store.
 */
private fun filtersStore(): FiltersStore = FiltersStore(
    state = FiltersState(
        properties = Translation.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap(),
        searchableProperties = properties,
        sortAscending = defaults.sortAscending,
        sortProperty = defaults.sortProperty,
        visibleProperties = properties,
        showDeletedToggle = false
    ),
    storageFile = AppFile.TranslationPreferences,
    defaults = defaults,
)
