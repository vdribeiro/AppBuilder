package com.app.builder.ui.navigation.user

import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.User
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.lifecycle.hostViewModel
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.hostSessionStore
import com.app.builder.ui.navigation.scene.SplitSceneStrategy.Companion.split

/**
 * The user routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.userProvider(useCases: UseCases) {
    entry<Screen.UserList>(metadata = split()) {
        val router = LocalRouter.current
        UserListScreen(
            sessionStore = hostSessionStore(useCases = useCases),
            filtersStore = hostViewModel(key = FILTERS_KEY) { filtersStore() },
            store = viewModel { UserListScreenStore(router = router, state = UserListScreenState(), userUseCases = useCases.userUseCases) }
        )
    }
    entry<Screen.UserDetail>(metadata = split()) {
        val router = LocalRouter.current
        UserDetailScreen(
            sessionStore = hostSessionStore(useCases = useCases),
            filtersStore = hostViewModel(key = FILTERS_KEY) { filtersStore() },
            store = viewModel {
                UserDetailScreenStore(
                    state = UserDetailScreenState(),
                    router = router,
                    userUseCases = useCases.userUseCases,
                    accountUseCases = useCases.accountUseCases,
                    authenticationUseCases = useCases.authenticationUseCases,
                    userUuid = it.uuid,
                    new = it.new
                )
            }
        )
    }
}

/** The key of the user filters among the stores shared by the navigation host. */
private const val FILTERS_KEY = "user_filters"
/** The user properties by name. */
private val properties = User.Property.entries.map { it.name }.toPersistentList()
/** The default user filters. */
private val defaults = AppFile.ActionBarData(
    search = "",
    searchableProperties = properties,
    sortAscending = true,
    sortProperty = User.Property.NAME.name,
    visibleProperties = properties,
)

/**
 * Creates the user filters store.
 *
 * @return The user filters store.
 */
private fun filtersStore(): FiltersStore = FiltersStore(
    state = FiltersState(
        properties = User.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap(),
        searchableProperties = properties,
        sortAscending = defaults.sortAscending,
        sortProperty = defaults.sortProperty,
        visibleProperties = properties
    ),
    storageFile = AppFile.UserPreferences,
    defaults = defaults,
)
