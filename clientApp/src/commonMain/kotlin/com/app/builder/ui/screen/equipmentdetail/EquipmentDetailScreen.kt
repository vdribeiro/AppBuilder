package com.app.builder.ui.screen.equipmentdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.domain.Equipment
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.card.EquipmentCard
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The user-facing Equipment Detail Screen, showing a single equipment.
 *
 * @param actionBarStore The store for action bar.
 * @param navigationStore Drives the bottom navigation bar.
 * @param store Provides the equipment detail state and receives its actions.
 */
@Composable
fun EquipmentDetailScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
    store: Store<EquipmentDetailScreenState, EquipmentDetailScreenAction>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val equipment = state.equipment

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.back() },
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore) },
    ) {
        EquipmentCard(
            enabled = equipment != null && state.editMode,
            modifiedAt = equipment?.modifiedAt?.toString(),
            deletedAt = equipment?.deletedAt?.toString(),
            name = equipment?.name,
            onNameChange = { store.send(action = EquipmentDetailScreenAction.ChangeName(name = it)) },
            code = equipment?.code,
            onCodeChange = { store.send(action = EquipmentDetailScreenAction.ChangeCode(code = it)) },
        )
    }
}

@Preview
@Composable
private fun EquipmentDetailScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "equipment_name" to "Name",
            "equipment_code" to "Code"
        )
    )
    EquipmentDetailScreen(
        actionBarStore = Store(initialState = ActionBarState(avatarName = "Equipment")),
        navigationStore = Store(initialState = NavigationState()),
        store = Store(
            initialState = EquipmentDetailScreenState(
                equipment = Equipment(
                    uuid = uuid(),
                    modifiedAt = now(),
                    deletedAt = null,
                    name = "Equipment Name",
                    code = "EQUIPMENT-001"
                )
            )
        )
    )
}
