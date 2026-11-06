package com.app.builder.ui.navigation.devicefiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.list.DeviceFileList
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.core.overlay.Snackbar
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The Device Files Screen, listing the files the application holds on the device and opening them with the platform's own handler.
 *
 * @param sessionStore The session store shared by every screen.
 * @param store Provides the stored files state and receives its actions.
 */
@Composable
fun DeviceFilesScreen(
    sessionStore: Store<SessionState, Unit>,
    store: Store<DeviceFilesScreenState, DeviceFilesScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    val router = LocalRouter.current

    LaunchedEffect(key1 = Unit) { store.send(action = DeviceFilesScreenAction.Refresh) }

    Screen(
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = { ActionBar(sessionStore = sessionStore) },
        bottomBar = { Navigation(sessionStore = sessionStore, selected = NavigationRoute.DEVICE_FILE) },
        snackbarHost = {
            state.message?.let {
                Snackbar(message = it, onDismiss = { store.send(action = DeviceFilesScreenAction.DismissMessage) })
            }
        }
    ) {
        DeviceFileList(
            items = state.files,
            onClick = { store.send(action = DeviceFilesScreenAction.OpenFile(path = it.path)) }
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceFilesScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "device_file" to "Files"
        )
    )
    DeviceFilesScreen(
        sessionStore = previewStore(initialState = SessionState()),
        store = previewStore(initialState = DeviceFilesScreenState())
    )
}
