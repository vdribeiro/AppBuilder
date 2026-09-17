package com.app.builder.ui.screen.login

import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.container.Login
import com.app.builder.ui.modifier.onKeyPress
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The user-facing Login Screen.
 *
 * @param store Provides the login screen state and receives its actions.
 */
@Composable
fun LoginScreen(store: Store<LoginScreenState, LoginScreenAction>) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    Screen(
        modifier = Modifier.imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Login(
            modifier = Modifier.onKeyPress(
                keys = listOf(Key.Enter, Key.NumPadEnter),
                onKey = { store.send(action = LoginScreenAction.Submit) }
            ),
            loading = state.loading,
            username = state.username,
            onUsernameValueChange = { store.send(action = LoginScreenAction.UpdateUsername(username = it)) },
            password = state.password,
            onPasswordValueChange = { store.send(action = LoginScreenAction.UpdatePassword(password = it)) },
            onSubmit = { store.send(action = LoginScreenAction.Submit) }
        )
    }
}

@Preview
@Composable
private fun LoginScreenLoadingPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "app_name" to "App Builder",
            "username" to "Username",
            "password" to "Password",
            "login" to "Login"
        )
    )
    LoginScreen(
        store = Store(
            initialState = LoginScreenState(
                loading = true,
                username = "test@example.com",
                password = "password123"
            )
        )
    )
}

@Preview
@Composable
private fun LoginScreenAllPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "app_name" to "App Builder",
            "username" to "Username",
            "password" to "Password",
            "login" to "Login"
        )
    )
    LoginScreen(
        store = Store(
            initialState = LoginScreenState(
                loading = false,
                username = "",
                password = ""
            )
        )
    )
}