package com.app.builder.ui.navigation.material

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
import com.app.builder.ui.component.list.MaterialList
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
 * The user-facing Material List Screen, showing the list of materials.
 *
 * @param sessionStore The session store shared by every screen.
 * @param filtersStore The material filters shared by every material screen.
 * @param store Provides the material list state and receives its actions.
 */
@Composable
fun MaterialListScreen(
    sessionStore: Store<SessionState, Unit>,
    filtersStore: Store<FiltersState, FiltersAction>,
    store: Store<MaterialListScreenState, MaterialListScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = {
            ActionBar(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                entityType = EntityType.MATERIAL,
                onAddClick = { router.navigate(screen = Screen.MaterialDetail(uuid = uuid().toString(), new = true), option = Router.NavOption.REPLACE_LAST) },
            )
        },
        bottomBar = { Navigation(sessionStore = sessionStore, selected = NavigationRoute.MATERIAL) },
    ) {
        MaterialList(
            items = state.materials,
            onClick = { store.send(action = MaterialListScreenAction.SelectMaterial(materialUuid = it.uuid)) }
        )
    }
}

@PreviewLightDark
@Composable
private fun MaterialListScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "material_name" to "Name",
            "material_description" to "Description"
        )
    )
    MaterialListScreen(
        sessionStore = previewStore(initialState = SessionState()),
        filtersStore = previewStore(initialState = FiltersState()),
        store = previewStore(initialState = MaterialListScreenState())
    )
}
