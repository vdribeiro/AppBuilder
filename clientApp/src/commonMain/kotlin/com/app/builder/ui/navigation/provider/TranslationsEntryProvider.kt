package com.app.builder.ui.navigation.provider

import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.Translation
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.translations.TranslationsScreen
import com.app.builder.ui.screen.translations.TranslationsScreenState
import com.app.builder.ui.screen.translations.TranslationsScreenStore
import com.app.builder.ui.store.Store

/**
 * The translations route.
 *
 * @param useCases The use cases.
 * @param navigationStore The navigation bar store shared by every screen.
 */
fun EntryProviderScope<NavKey>.translationsProvider(useCases: UseCases, navigationStore: Store<NavigationState, Unit>) {
    entry<Screen.Translations> {
        val router = LocalRouter.current
        TranslationsScreen(
            actionBarStore = viewModel {
                ActionBarStore(
                    state = actionBarState,
                    router = router,
                    authenticationUseCases = useCases.authenticationUseCases,
                    statusUseCases = useCases.statusUseCases,
                    storageFile = AppFile.TranslationPreferences,
                    defaults = defaults,
                    entityType = EntityType.TRANSLATION,
                )
            },
            navigationStore = navigationStore,
            store = viewModel { TranslationsScreenStore(state = TranslationsScreenState(), translationUseCases = useCases.translationUseCases) },
        )
    }
}

/** The translation properties by name. */
private val properties = Translation.Property.entries.map { it.name }.toPersistentList()
/** The translation property map of name and translation key. */
private val propertyMap = Translation.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap()
/** The default action bar data. */
private val defaults = AppFile.ActionBarData(
    search = "",
    searchableProperties = properties,
    sortAscending = true,
    sortProperty = Translation.Property.KEY.name,
    visibleProperties = properties,
)
/** The default action bar state. */
private val actionBarState = ActionBarState(
    properties = propertyMap,
    searchableProperties = properties,
    sortAscending = defaults.sortAscending,
    sortProperty = defaults.sortProperty,
    visibleProperties = properties,
    showFilters = true,
    showDeletedToggle = false
)
