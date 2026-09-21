package com.app.builder.ui.screen.register

import kotlinx.collections.immutable.persistentListOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.domain.EntityType
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.button.PermissionItem
import com.app.builder.ui.component.container.RegistrationForm
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.useravatar.UserAvatarState
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The user-facing Register Screen.
 *
 * @param actionBarStore The store for action bar.
 * @param navigationStore Drives the bottom navigation bar.
 * @param store Provides the registration screen state and receives its actions.
 */
@Composable
fun RegisterScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
    store: Store<RegistrationScreenState, RegistrationScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    Screen(
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore) }
    ) {
        RegistrationForm(
            loading = state.loading,
            name = state.name,
            onNameValueChange = { store.send(action = RegistrationScreenAction.UpdateName(name = it)) },
            username = state.username,
            onUsernameValueChange = { store.send(action = RegistrationScreenAction.UpdateUsername(username = it)) },
            password = state.password,
            onPasswordValueChange = { store.send(action = RegistrationScreenAction.UpdatePassword(password = it)) },
            permissions = state.permissions,
            onPermissionToggled = { store.send(action = RegistrationScreenAction.TogglePermission(permission = it)) },
            onSubmit = { store.send(action = RegistrationScreenAction.Submit) }
        )
    }
}

@Preview
@Composable
private fun RegisterScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "app_name" to "App Builder",
            "name" to "Name",
            "username" to "Username",
            "password" to "Password",
            "permissions" to "Permissions",
            "read" to "Read",
            "write" to "Write",
            "client_flags" to "Client Flag",
            "client_configs" to "Client Config",
            "register" to "Register",
            "login" to "Login"
        )
    )
    RegisterScreen(
        actionBarStore = Store(initialState = ActionBarState (avatarName = "Knuckles")),
        navigationStore = Store(initialState = NavigationState()),
        store = Store(
            initialState = RegistrationScreenState(
                permissions = persistentListOf(
                    PermissionItem(id = EntityType.CLIENT_FLAG.name, name = EntityType.CLIENT_FLAG.name.lowercase(), read = true, write = true),
                    PermissionItem(id = EntityType.CLIENT_CONFIG.name, name = EntityType.CLIENT_CONFIG.name.lowercase(), read = true)
                )
            )
        ),
    )
}
