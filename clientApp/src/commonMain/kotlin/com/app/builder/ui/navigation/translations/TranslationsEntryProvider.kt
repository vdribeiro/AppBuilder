package com.app.builder.ui.navigation.translations

import kotlinx.collections.immutable.toImmutableMap
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Translation
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.lifecycle.hostViewModel
import com.app.builder.ui.navigation.Screen

/**
 * The translations route.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.translationsProvider(useCases: UseCases) {
    entry<Screen.Translations> {
        TranslationsScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = filtersStore(),
            store = viewModel { TranslationsScreenStore(state = TranslationsScreenState(), translationUseCases = useCases.translationUseCases) },
        )
    }
}

/** The translation properties by key, labelled by their translation keys. */
private val properties = Translation.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap()

/**
 * Gets the translation filters store shared by the navigation host, creating it on first use.
 *
 * @return The translation filters store.
 */
@Composable
private fun filtersStore(): FiltersStore = hostViewModel(key = FiltersStore.key(storageFile = AppFile.TranslationPreferences)) { FiltersStore.get(properties = properties, storageFile = AppFile.TranslationPreferences, sortProperty = Translation.Property.KEY.name, sortAscending = true, showDeletedToggle = false) }
