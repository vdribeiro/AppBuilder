package com.app.builder.ui.navigation.timepunch

import kotlinx.collections.immutable.toImmutableMap
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.TimePunch
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.component.user.userFiltersStore
import com.app.builder.ui.component.user.userLinkPickerStore
import com.app.builder.ui.lifecycle.hostViewModel
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.scene.SplitSceneStrategy.Companion.split

/**
 * The time punch routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.timePunchProvider(useCases: UseCases) {
    entry<Screen.TimePunchList>(metadata = split()) {
        val router = LocalRouter.current
        TimePunchListScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = filtersStore(),
            store = viewModel {
                TimePunchListScreenStore(
                    state = TimePunchListScreenState(),
                    router = router,
                    timePunchUseCases = useCases.timePunchUseCases,
                    userUseCases = useCases.userUseCases,
                    authenticationUseCases = useCases.authenticationUseCases
                )
            }
        )
    }
    entry<Screen.TimePunchDetail>(metadata = split()) {
        val router = LocalRouter.current
        TimePunchDetailScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = filtersStore(),
            userFiltersStore = userFiltersStore(),
            userPickerStore = viewModel(key = "user_picker") { userLinkPickerStore(userUseCases = useCases.userUseCases, single = true) },
            store = viewModel {
                TimePunchDetailScreenStore(
                    state = TimePunchDetailScreenState(),
                    router = router,
                    timePunchUseCases = useCases.timePunchUseCases,
                    userUseCases = useCases.userUseCases,
                    authenticationUseCases = useCases.authenticationUseCases,
                    timePunchUuid = it.uuid,
                    new = it.new
                )
            }
        )
    }
}

/** The time punch properties by key, labeled by their translation keys. */
private val properties = TimePunch.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap()

/** The time punch properties shown by default. */
private val visibleProperties = listOf(
    TimePunch.Property.USER_NAME,
    TimePunch.Property.TIME,
    TimePunch.Property.SOURCE,
).map { it.name }

/**
 * Gets the time punch filters store shared by the navigation host, creating it on first use.
 *
 * @return The time punch filters store.
 */
@Composable
private fun filtersStore(): FiltersStore = hostViewModel(key = FiltersStore.key(storageFile = AppFile.TimePunchPreferences)) {
    FiltersStore.get(
        properties = properties,
        storageFile = AppFile.TimePunchPreferences,
        sortProperty = TimePunch.Property.TIME.name,
        sortAscending = false,
        visibleProperties = visibleProperties
    )
}
