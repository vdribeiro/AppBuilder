package com.app.builder.ui.screen.buildinglist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.list.BuildingList
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The user-facing Building List Screen, showing the list of buildings.
 *
 * @param actionBarStore The store for action bar.
 * @param navigationStore Drives the bottom navigation bar.
 * @param store Provides the building list state and receives its actions.
 */
@Composable
fun BuildingListScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
    store: Store<BuildingListScreenState, BuildingListScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore) },
    ) {
        BuildingList(
            items = state.buildings,
            onClick = { store.send(action = BuildingListScreenAction.SelectBuilding(buildingUuid = it.uuid)) }
        )
    }
}

@Preview
@Composable
private fun BuildingListScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "buildings" to "Buildings",
            "building_name" to "Name",
            "building_code" to "Code"
        )
    )
    BuildingListScreen(
        actionBarStore = Store(initialState = ActionBarState(avatarName = "Buildings")),
        navigationStore = Store(initialState = NavigationState()),
        store = Store(initialState = BuildingListScreenState())
    )
}
