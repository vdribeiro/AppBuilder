package com.app.builder.ui.navigation.user

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.locale.getLocalDateTime
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.domain.Account
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.User
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalSplitScreen
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.button.PermissionItem
import com.app.builder.ui.component.card.UserCard
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The user-facing User Detail Screen, showing a single user.
 *
 * @param sessionStore The session store shared by every screen.
 * @param filtersStore The user filters shared by every user screen, shown when the user list is beside this screen.
 * @param store Provides the user detail state and receives its actions.
 */
@Composable
fun UserDetailScreen(
    sessionStore: Store<SessionState, Unit>,
    filtersStore: Store<FiltersState, FiltersAction>,
    store: Store<UserDetailScreenState, UserDetailScreenAction>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val user = state.user
    val account = state.account
    val savedAccount = state.savedAccount
    val deleted = user?.deletedAt != null
    val deletable = state.accountReadable && (savedAccount == null || savedAccount.deletedAt != null)
    val permissions = remember(key1 = account?.permissions) { account?.toPermissionItems() }

    val router = LocalRouter.current
    val splitScreen = LocalSplitScreen.current

    Screen(
        onBackClick = { router.back() },
        topBar = {
            ActionBar(
                sessionStore = sessionStore,
                filtersStore = filtersStore.takeIf { splitScreen },
                entityType = EntityType.USER,
                editEntityTypes = setOf(EntityType.ACCOUNT),
                deleted = deleted,
                onAddClick = if (splitScreen && !state.editing) {
                    { router.navigate(screen = Screen.UserDetail(uuid = uuid().toString(), new = true), option = Router.NavOption.REPLACE_LAST) }
                } else null,
                onEditClick = if (user != null && !state.editing && !deleted) {
                    { store.send(action = UserDetailScreenAction.Edit) }
                } else null,
                onSaveClick = if (state.editing) {
                    { store.send(action = UserDetailScreenAction.Save) }
                } else null,
                onCancelClick = if (state.editing) {
                    { store.send(action = UserDetailScreenAction.Cancel) }
                } else null,
                onDeleteClick = if (user != null && !state.editing && (deletable || deleted)) {
                    { store.send(action = if (it) UserDetailScreenAction.Delete else UserDetailScreenAction.Restore) }
                } else null,
            )
        },
        bottomBar = { Navigation(sessionStore = sessionStore, selected = NavigationRoute.USER) },
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState())) {
            UserCard(
                enabled = user != null && state.editing && state.userWritable,
                uuid = user?.uuid?.toString(),
                modifiedAt = user?.modifiedAt?.let { getLocalDateTime(utc = it.toString()) },
                deletedAt = user?.deletedAt?.let { getLocalDateTime(utc = it.toString()) },
                name = user?.name,
                onNameChange = { store.send(action = UserDetailScreenAction.ChangeName(name = it)) },
                avatar = user?.avatar?.let { Image(url = it) },
                accountEnabled = state.editing && state.accountWritable,
                username = account?.username,
                onUsernameChange = { store.send(action = UserDetailScreenAction.ChangeUsername(username = it)) },
                password = state.password.takeIf { account != null && savedAccount == null },
                onPasswordChange = { store.send(action = UserDetailScreenAction.ChangePassword(password = it)) },
                accountActive = account?.let { it.deletedAt == null }?.takeIf { savedAccount != null },
                onAccountActiveChange = { store.send(action = UserDetailScreenAction.ChangeAccountActive(active = it)) },
                permissions = permissions,
                onPermissionToggled = { store.send(action = UserDetailScreenAction.TogglePermission(permission = it)) },
            )
        }
    }
}

/**
 * Converts this account's permissions into one permission item per [EntityType].
 *
 * @return Permission items in [EntityType] order.
 */
private fun Account.toPermissionItems(): ImmutableList<PermissionItem> = EntityType.entries.map {
    PermissionItem(
        id = it.name,
        name = it.name.lowercase(),
        read = hasPermission(entityType = it, permission = Permission.READ),
        write = hasPermission(entityType = it, permission = Permission.WRITE)
    )
}.toPersistentList()

@PreviewLightDark
@Composable
private fun UserDetailScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "user_name" to "Name"
        )
    )
    val user = User(
        uuid = uuid(),
        modifiedAt = now(),
        deletedAt = null,
        name = "Vitor Ribeiro",
        avatar = null
    )
    UserDetailScreen(
        sessionStore = previewStore(initialState = SessionState()),
        filtersStore = previewStore(initialState = FiltersState()),
        store = previewStore(
            initialState = UserDetailScreenState(
                user = user
            )
        )
    )
}
