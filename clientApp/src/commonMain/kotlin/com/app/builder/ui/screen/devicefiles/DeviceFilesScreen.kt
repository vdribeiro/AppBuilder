package com.app.builder.ui.screen.devicefiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.list.FileList
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.core.overlay.Snackbar
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The Device Files Screen, listing the files the application holds on the device and opening them with the platform's own handler.
 *
 * @param actionBarStore The store for action bar.
 * @param navigationStore Drives the bottom navigation bar.
 * @param store Provides the stored files state and receives its actions.
 */
@Composable
fun DeviceFilesScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
    store: Store<DeviceFilesScreenState, DeviceFilesScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = Unit) { store.send(action = DeviceFilesScreenAction.Refresh) }

    Screen(
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore) },
        snackbarHost = {
            state.message?.let {
                Snackbar(message = it, onDismiss = { store.send(action = DeviceFilesScreenAction.DismissMessage) })
            }
        }
    ) {
        FileList(
            items = state.files,
            onClick = { store.send(action = DeviceFilesScreenAction.OpenFile(path = it.path)) }
        )
    }
}

@Preview
@Composable
private fun DeviceFilesScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "device_file" to "Files"
        )
    )
    DeviceFilesScreen(
        actionBarStore = Store(initialState = ActionBarState(avatarName = "Files")),
        navigationStore = Store(initialState = NavigationState()),
        store = Store(initialState = DeviceFilesScreenState())
    )
}
