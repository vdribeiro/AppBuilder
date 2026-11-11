package com.app.builder.ui.navigation.building

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.security.uuid
import com.app.builder.domain.EntityType
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.list.BuildingList
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The user-facing Building List Screen, showing the list of buildings.
 *
 * @param sessionStore The session store shared by every screen.
 * @param filtersStore The building filters shared by every building screen.
 * @param store Provides the building list state and receives its actions.
 */
@Composable
fun BuildingListScreen(
    sessionStore: Store<SessionState, Unit>,
    filtersStore: Store<FiltersState, FiltersAction>,
    store: Store<BuildingListScreenState, BuildingListScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = {
            ActionBar(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                entityType = EntityType.BUILDING,
                onAddClick = { router.navigate(screen = Screen.BuildingDetail(uuid = uuid().toString(), new = true), option = Router.NavOption.REPLACE_LAST) },
            )
        },
        bottomBar = { Navigation(sessionStore = sessionStore, selected = NavigationRoute.BUILDING) },
    ) {
        BuildingList(
            items = state.buildings,
            onClick = { store.send(action = BuildingListScreenAction.SelectBuilding(buildingUuid = it.uuid)) }
        )
    }
}

@PreviewLightDark
@Composable
private fun BuildingListScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "building_list" to "Buildings",
            "building_name" to "Name",
            "description" to "Description"
        )
    )
    BuildingListScreen(
        sessionStore = previewStore(initialState = SessionState()),
        filtersStore = previewStore(initialState = FiltersState()),
        store = previewStore(initialState = BuildingListScreenState())
    )
}
