package com.app.builder.ui.navigation.user

import kotlinx.collections.immutable.toImmutableMap
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.User
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.lifecycle.hostViewModel
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
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
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = filtersStore(),
            store = viewModel { UserListScreenStore(router = router, state = UserListScreenState(), userUseCases = useCases.userUseCases) }
        )
    }
    entry<Screen.UserDetail>(metadata = split()) {
        val router = LocalRouter.current
        UserDetailScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = filtersStore(),
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

/** The user properties by key, labelled by their translation keys. */
private val properties = User.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap()

/**
 * Gets the user filters store shared by the navigation host, creating it on first use.
 *
 * @return The user filters store.
 */
@Composable
private fun filtersStore(): FiltersStore = hostViewModel(key = FiltersStore.key(storageFile = AppFile.UserPreferences)) { FiltersStore.get(properties = properties, storageFile = AppFile.UserPreferences, sortProperty = User.Property.NAME.name, sortAscending = true) }
