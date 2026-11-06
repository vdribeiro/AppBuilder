package com.app.builder.ui.navigation.provider

import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.core.security.uuid
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.User
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.LocalSplitScreen
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.scene.SplitSceneStrategy.Companion.split
import com.app.builder.ui.screen.userdetail.UserDetailScreen
import com.app.builder.ui.screen.userdetail.UserDetailScreenAction
import com.app.builder.ui.screen.userdetail.UserDetailScreenState
import com.app.builder.ui.screen.userdetail.UserDetailScreenStore
import com.app.builder.ui.screen.userlist.UserListScreen
import com.app.builder.ui.screen.userlist.UserListScreenState
import com.app.builder.ui.screen.userlist.UserListScreenStore
import com.app.builder.ui.store.Store

/**
 * The user routes.
 *
 * @param useCases The use cases.
 * @param navigationStore The navigation bar store shared by every screen.
 */
fun EntryProviderScope<NavKey>.userProvider(useCases: UseCases, navigationStore: Store<NavigationState, Unit>) {
    entry<Screen.UserList>(metadata = split()) {
        val router = LocalRouter.current
        val store = viewModel { UserListScreenStore(router = router, state = UserListScreenState(), userUseCases = useCases.userUseCases) }
        UserListScreen(
            actionBarStore = viewModel { getActionBarStore(router = router, useCases = useCases) },
            navigationStore = navigationStore,
            store = store
        )
    }
    entry<Screen.UserDetail>(metadata = split()) {
        val router = LocalRouter.current
        val splitScreen = LocalSplitScreen.current
        val store = viewModel {
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
        UserDetailScreen(
            actionBarStore = viewModel {
                getActionBarStore(
                    router = router,
                    useCases = useCases,
                    showAdd = splitScreen,
                    showFilters = splitScreen,
                    editing = it.new,
                    editEntityTypes = setOf(EntityType.ACCOUNT),
                    onEditClick = { store.send(action = UserDetailScreenAction.Edit) },
                    onSaveClick = { store.send(action = UserDetailScreenAction.Save) },
                    onCancelClick = { store.send(action = UserDetailScreenAction.Cancel) },
                    onDeleteClick = { store.send(action = UserDetailScreenAction.Delete) },
                    onRestoreClick = { store.send(action = UserDetailScreenAction.Restore) },
                )
            },
            navigationStore = navigationStore,
            store = store
        )
    }
}

/** The user properties by name. */
private val properties = User.Property.entries.map { it.name }.toPersistentList()
/** The user property map of name and translation key. */
private val propertyMap = User.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap()
/** The default action bar data. */
private val defaults = AppFile.ActionBarData(
    search = "",
    searchableProperties = properties,
    sortAscending = true,
    sortProperty = User.Property.NAME.name,
    visibleProperties = properties,
)
/** The default action bar state. */
private val actionBarState = ActionBarState(
    properties = propertyMap,
    searchableProperties = properties,
    sortAscending = defaults.sortAscending,
    sortProperty = defaults.sortProperty,
    visibleProperties = properties
)

/**
 * Get the user action bar store.
 *
 * @param router The router.
 * @param useCases The use cases.
 * @param navigationStore The navigation bar store shared by every screen.
 * @param showAdd true to show add button, false otherwise.
 * @param showFilters true to show the filters button, false otherwise.
 * @param editing true to start in edit mode, false to start in view mode.
 * @param editEntityTypes Further entities whose write permission also allows editing a user.
 * @param onEditClick The edit click callback, or null to hide the edit action.
 * @param onSaveClick The save click callback, or null to hide the save action.
 * @param onCancelClick The cancel click callback, or null to hide the cancel action.
 * @param onDeleteClick The delete click callback, or null to hide the delete action.
 * @param onRestoreClick The restore click callback, or null to hide the delete action.
 * @return The action bar store.
 */
private fun getActionBarStore(
    router: Router,
    useCases: UseCases,
    showAdd: Boolean = true,
    showFilters: Boolean = true,
    editing: Boolean = false,
    editEntityTypes: Set<EntityType> = emptySet(),
    onEditClick: (suspend () -> Unit)? = null,
    onSaveClick: (suspend () -> Unit)? = null,
    onCancelClick: (suspend () -> Unit)? = null,
    onDeleteClick: (suspend () -> Unit)? = null,
    onRestoreClick: (suspend () -> Unit)? = null
): ActionBarStore = ActionBarStore(
    state = actionBarState.copy(editing = editing, showFilters = showFilters),
    router = router,
    authenticationUseCases = useCases.authenticationUseCases,
    statusUseCases = useCases.statusUseCases,
    storageFile = AppFile.UserPreferences,
    defaults = defaults,
    entityType = EntityType.USER,
    editEntityTypes = editEntityTypes,
    onAddClick = if (showAdd) {
        { router.navigate(screen = Screen.UserDetail(uuid = uuid().toString(), new = true), option = Router.NavOption.REPLACE_LAST) }
    } else null,
    onEditClick = onEditClick,
    onSaveClick = onSaveClick,
    onCancelClick = onCancelClick,
    onDeleteClick = onDeleteClick,
    onRestoreClick = onRestoreClick
)
