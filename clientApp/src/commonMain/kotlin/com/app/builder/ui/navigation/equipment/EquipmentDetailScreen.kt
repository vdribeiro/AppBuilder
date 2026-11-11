package com.app.builder.ui.navigation.equipment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.locale.getLocalDateTime
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.domain.EntityType
import com.app.builder.domain.Equipment
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalSplitScreen
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.card.EquipmentCard
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
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
 * The user-facing Equipment Detail Screen, showing a single equipment.
 *
 * @param sessionStore The session store shared by every screen.
 * @param filtersStore The equipment filters shared by every equipment screen, shown when the equipment list is beside this screen.
 * @param store Provides the equipment detail state and receives its actions.
 */
@Composable
fun EquipmentDetailScreen(
    sessionStore: Store<SessionState, Unit>,
    filtersStore: Store<FiltersState, FiltersAction>,
    store: Store<EquipmentDetailScreenState, EquipmentDetailScreenAction>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val equipment = state.equipment
    val deleted = equipment?.deletedAt != null

    val router = LocalRouter.current
    val splitScreen = LocalSplitScreen.current

    Screen(
        onBackClick = { router.back() },
        topBar = {
            ActionBar(
                sessionStore = sessionStore,
                filtersStore = filtersStore.takeIf { splitScreen },
                entityType = EntityType.EQUIPMENT,
                deleted = deleted,
                onAddClick = if (splitScreen && !state.editing) {
                    { router.navigate(screen = Screen.EquipmentDetail(uuid = uuid().toString(), new = true), option = Router.NavOption.REPLACE_LAST) }
                } else null,
                onEditClick = if (equipment != null && !state.editing && !deleted) {
                    { store.send(action = EquipmentDetailScreenAction.Edit) }
                } else null,
                onSaveClick = if (state.editing) {
                    { store.send(action = EquipmentDetailScreenAction.Save) }
                } else null,
                onCancelClick = if (state.editing) {
                    { store.send(action = EquipmentDetailScreenAction.Cancel) }
                } else null,
                onDeleteClick = if (equipment != null && !state.editing) {
                    { store.send(action = if (it) EquipmentDetailScreenAction.Delete else EquipmentDetailScreenAction.Restore) }
                } else null,
            )
        },
        bottomBar = { Navigation(sessionStore = sessionStore, selected = NavigationRoute.EQUIPMENT) },
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState())) {
            EquipmentCard(
                enabled = equipment != null && state.editing,
                uuid = equipment?.uuid?.toString(),
                modifiedAt = equipment?.modifiedAt?.let { getLocalDateTime(utc = it.toString()) },
                deletedAt = equipment?.deletedAt?.let { getLocalDateTime(utc = it.toString()) },
                name = equipment?.name,
                onNameChange = { store.send(action = EquipmentDetailScreenAction.ChangeName(name = it)) },
                description = equipment?.description,
                onDescriptionChange = { store.send(action = EquipmentDetailScreenAction.ChangeDescription(description = it)) },
                status = equipment?.status?.translationKey,
                statuses = state.statuses,
                onStatusChange = { key -> Equipment.Status.entries.firstOrNull { it.translationKey == key }?.let { store.send(action = EquipmentDetailScreenAction.ChangeStatus(status = it)) } },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun EquipmentDetailScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "equipment_name" to "Name"
        )
    )
    val equipment = Equipment(
        uuid = uuid(),
        modifiedAt = now(),
        deletedAt = null,
        name = "Big Compressor",
        description = "A description lalala",
        status = Equipment.Status.WORKING
    )
    EquipmentDetailScreen(
        sessionStore = previewStore(initialState = SessionState()),
        filtersStore = previewStore(initialState = FiltersState()),
        store = previewStore(
            initialState = EquipmentDetailScreenState(
                equipment = equipment
            )
        )
    )
}
