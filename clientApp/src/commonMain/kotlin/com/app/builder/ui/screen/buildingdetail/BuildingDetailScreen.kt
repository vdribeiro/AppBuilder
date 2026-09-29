package com.app.builder.ui.screen.buildingdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.domain.Building
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.card.BuildingCard
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The user-facing Building Detail Screen, showing a single building.
 *
 * @param actionBarStore The store for action bar.
 * @param navigationStore Drives the bottom navigation bar.
 * @param store Provides the building detail state and receives its actions.
 */
@Composable
fun BuildingDetailScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
    store: Store<BuildingDetailScreenState, BuildingDetailScreenAction>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val building = state.building

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.back() },
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore) },
    ) {
        BuildingCard(
            enabled = building != null && state.editMode,
            modifiedAt = building?.modifiedAt?.toString(),
            deletedAt = building?.deletedAt?.toString(),
            name = building?.name,
            onNameChange = { store.send(action = BuildingDetailScreenAction.ChangeName(name = it)) },
            code = building?.code,
            onCodeChange = { store.send(action = BuildingDetailScreenAction.ChangeCode(code = it)) },
        )
    }
}

@Preview
@Composable
private fun BuildingDetailScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "building_name" to "Name",
            "building_code" to "Code"
        )
    )
    BuildingDetailScreen(
        actionBarStore = Store(initialState = ActionBarState(avatarName = "Building")),
        navigationStore = Store(initialState = NavigationState()),
        store = Store(
            initialState = BuildingDetailScreenState(
                building = Building(
                    uuid = uuid(),
                    modifiedAt = now(),
                    deletedAt = null,
                    name = "Building Name",
                    code = "BUILDING-001"
                )
            )
        )
    )
}
