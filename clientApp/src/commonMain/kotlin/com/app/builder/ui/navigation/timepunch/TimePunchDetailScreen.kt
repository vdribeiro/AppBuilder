package com.app.builder.ui.navigation.timepunch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.locale.toInstant
import com.app.builder.core.security.uuid
import com.app.builder.domain.EntityType
import com.app.builder.domain.TimePunch
import com.app.builder.domain.User
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalSplitScreen
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.card.TimePunchCard
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.link.LinkPickerAction
import com.app.builder.ui.component.link.LinkPickerState
import com.app.builder.ui.component.list.UserItem
import com.app.builder.ui.component.user.UserLinkPicker
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore
import kotlinx.collections.immutable.persistentMapOf

/**
 * The user-facing Time Punch Detail Screen, showing a single time punch and editing its time, and its user while it is new.
 *
 * @param sessionStore The session store shared by every screen.
 * @param filtersStore The time punch filters shared by every time punch screen, shown when the time punch list is beside this screen.
 * @param userFiltersStore The user list filters, shown in the user picker.
 * @param userPickerStore Provides the user picker state and receives its actions.
 * @param store Provides the time punch detail state and receives its actions.
 */
@Composable
fun TimePunchDetailScreen(
    sessionStore: Store<SessionState, Unit>,
    filtersStore: Store<FiltersState, FiltersAction>,
    userFiltersStore: Store<FiltersState, FiltersAction>,
    userPickerStore: Store<LinkPickerState<User, UserItem>, LinkPickerAction>,
    store: Store<TimePunchDetailScreenState, TimePunchDetailScreenAction>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val timePunch = state.timePunch
    val deleted = timePunch?.deletedAt != null

    val router = LocalRouter.current
    val splitScreen = LocalSplitScreen.current

    Screen(
        onBackClick = { router.back() },
        topBar = {
            ActionBar(
                sessionStore = sessionStore,
                filtersStore = filtersStore.takeIf { splitScreen },
                entityType = EntityType.TIME_PUNCH,
                deleted = deleted,
                onAddClick = if (splitScreen && !state.editing) {
                    { router.navigate(screen = Screen.TimePunchDetail(uuid = uuid().toString(), new = true), option = Router.NavOption.REPLACE_LAST) }
                } else null,
                onEditClick = if (timePunch != null && !state.editing && !deleted) {
                    { store.send(action = TimePunchDetailScreenAction.Edit) }
                } else null,
                onSaveClick = if (state.editing) {
                    { store.send(action = TimePunchDetailScreenAction.Save) }
                } else null,
                onCancelClick = if (state.editing) {
                    { store.send(action = TimePunchDetailScreenAction.Cancel) }
                } else null,
                onDeleteClick = if (timePunch != null && !state.editing) {
                    { store.send(action = if (it) TimePunchDetailScreenAction.Delete else TimePunchDetailScreenAction.Restore) }
                } else null,
            )
        },
        bottomBar = { Navigation(sessionStore = sessionStore, selected = NavigationRoute.TIME_PUNCH) },
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState())) {
            TimePunchCard(
                enabled = timePunch != null && state.editing,
                uuid = timePunch?.uuid?.toString(),
                modifiedAt = timePunch?.modifiedAt?.toString(),
                deletedAt = timePunch?.deletedAt?.toString(),
                userUuid = timePunch?.userUuid?.toString(),
                userName = timePunch?.let { state.userNames[it.userUuid] },
                onUserClick = if (timePunch != null && state.new) {
                    { userPickerStore.send(action = LinkPickerAction.Open(picked = setOf(element = timePunch.userUuid))) }
                } else null,
                time = timePunch?.time?.toString(),
                onTimeChange = { utc -> utc.toInstant()?.let { store.send(action = TimePunchDetailScreenAction.ChangeTime(time = it)) } },
                source = timePunch?.source?.translationKey,
            )
        }
    }

    UserLinkPicker(
        store = userPickerStore,
        filtersStore = userFiltersStore,
        title = "time_punch_pick_user",
        onConfirm = { users -> users.firstOrNull()?.let { store.send(action = TimePunchDetailScreenAction.ChangeUser(userUuid = it.uuid)) } }
    )
}

@PreviewLightDark
@Composable
private fun TimePunchDetailScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "time_punch_user_name" to "User Name",
            "time_punch_time" to "Time",
            "time_punch_source" to "Source",
            "time_punch_source_manual" to "Manual"
        )
    )
    val timePunch = TimePunch(userUuid = uuid())
    TimePunchDetailScreen(
        sessionStore = previewStore(initialState = SessionState()),
        filtersStore = previewStore(initialState = FiltersState()),
        userFiltersStore = previewStore(initialState = FiltersState()),
        userPickerStore = previewStore(initialState = LinkPickerState()),
        store = previewStore(
            initialState = TimePunchDetailScreenState(
                timePunch = timePunch,
                userNames = persistentMapOf(timePunch.userUuid to "Vitor Ribeiro")
            )
        )
    )
}
