package com.app.builder.ui.screen.materialdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.domain.Material
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.card.MaterialCard
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The user-facing Material Detail Screen, showing a single material.
 *
 * @param actionBarStore The store for action bar.
 * @param navigationStore Drives the bottom navigation bar.
 * @param store Provides the material detail state and receives its actions.
 */
@Composable
fun MaterialDetailScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
    store: Store<MaterialDetailScreenState, MaterialDetailScreenAction>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val material = state.material

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.back() },
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore) },
    ) {
        MaterialCard(
            enabled = material != null && state.editMode,
            modifiedAt = material?.modifiedAt?.toString(),
            deletedAt = material?.deletedAt?.toString(),
            name = material?.name,
            onNameChange = { store.send(action = MaterialDetailScreenAction.ChangeName(name = it)) },
            code = material?.code,
            onCodeChange = { store.send(action = MaterialDetailScreenAction.ChangeCode(code = it)) },
        )
    }
}

@Preview
@Composable
private fun MaterialDetailScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "material_name" to "Name",
            "material_code" to "Code"
        )
    )
    MaterialDetailScreen(
        actionBarStore = Store(initialState = ActionBarState(avatarName = "Material")),
        navigationStore = Store(initialState = NavigationState()),
        store = Store(
            initialState = MaterialDetailScreenState(
                material = Material(
                    uuid = uuid(),
                    modifiedAt = now(),
                    deletedAt = null,
                    name = "Material Name",
                    code = "MATERIAL-001"
                )
            )
        )
    )
}
