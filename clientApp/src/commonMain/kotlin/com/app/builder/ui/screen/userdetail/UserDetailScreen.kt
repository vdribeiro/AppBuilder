package com.app.builder.ui.screen.userdetail

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.domain.Account
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.User
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.button.PermissionItem
import com.app.builder.ui.component.card.UserCard
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The user-facing User Detail Screen, showing a single user.
 *
 * @param actionBarStore The store for action bar.
 * @param navigationStore Drives the bottom navigation bar.
 * @param store Provides the user detail state and receives its actions.
 */
@Composable
fun UserDetailScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
    store: Store<UserDetailScreenState, UserDetailScreenAction>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val user = state.user
    val account = state.account
    val savedAccount = state.savedAccount
    val permissions = remember(key1 = account?.permissions) { account?.toPermissionItems() }

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.back() },
        topBar = {
            ActionBar(
                store = actionBarStore,
                deleted = user?.let { it.deletedAt != null },
                deletable = state.accountReadable && (savedAccount == null || savedAccount.deletedAt != null)
            )
        },
        bottomBar = { Navigation(store = navigationStore) },
    ) {
        UserCard(
            enabled = user != null && state.editing && state.userWritable,
            uuid = user?.uuid?.toString(),
            modifiedAt = user?.modifiedAt?.toString(),
            deletedAt = user?.deletedAt?.toString(),
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
        actionBarStore = previewStore(initialState = ActionBarState(avatarName = "User")),
        navigationStore = previewStore(initialState = NavigationState()),
        store = previewStore(
            initialState = UserDetailScreenState(
                user = user
            )
        )
    )
}
