package com.app.builder.ui.screen.equipmentlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.list.EquipmentList
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The user-facing Equipment List Screen, showing the list of equipments.
 *
 * @param actionBarStore The store for action bar.
 * @param navigationStore Drives the bottom navigation bar.
 * @param store Provides the equipment list state and receives its actions.
 */
@Composable
fun EquipmentListScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
    store: Store<EquipmentListScreenState, EquipmentListScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore) },
    ) {
        EquipmentList(
            items = state.equipments,
            onClick = { store.send(action = EquipmentListScreenAction.SelectEquipment(equipmentUuid = it.uuid)) }
        )
    }
}

@Preview
@Composable
private fun EquipmentListScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "equipments" to "Equipments",
            "equipment_name" to "Name",
            "equipment_code" to "Code"
        )
    )
    EquipmentListScreen(
        actionBarStore = Store(initialState = ActionBarState(avatarName = "Equipments")),
        navigationStore = Store(initialState = NavigationState()),
        store = Store(initialState = EquipmentListScreenState())
    )
}
