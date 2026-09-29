package com.app.builder.ui.screen.materiallist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.list.MaterialList
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The user-facing Material List Screen, showing the list of materials.
 *
 * @param actionBarStore The store for action bar.
 * @param navigationStore Drives the bottom navigation bar.
 * @param store Provides the material list state and receives its actions.
 */
@Composable
fun MaterialListScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
    store: Store<MaterialListScreenState, MaterialListScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore) },
    ) {
        MaterialList(
            items = state.materials,
            onClick = { store.send(action = MaterialListScreenAction.SelectMaterial(materialUuid = it.uuid)) }
        )
    }
}

@Preview
@Composable
private fun MaterialListScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "materials" to "Materials",
            "material_name" to "Name",
            "material_code" to "Code"
        )
    )
    MaterialListScreen(
        actionBarStore = Store(initialState = ActionBarState(avatarName = "Materials")),
        navigationStore = Store(initialState = NavigationState()),
        store = Store(initialState = MaterialListScreenState())
    )
}
