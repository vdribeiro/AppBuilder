package com.app.builder.ui.navigation.timepunch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.security.uuid
import com.app.builder.domain.EntityType
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.list.TimePunchItem
import com.app.builder.ui.component.list.TimePunchList
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.button.ButtonStyle
import com.app.builder.ui.getTranslation
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore
import kotlinx.collections.immutable.persistentListOf

/**
 * The user-facing Time Punch List Screen, showing the list of time punches and the button to punch the clock.
 *
 * @param sessionStore The session store shared by every screen.
 * @param filtersStore The time punch filters shared by every time punch screen.
 * @param store Provides the time punch list state and receives its actions.
 */
@Composable
fun TimePunchListScreen(
    sessionStore: Store<SessionState, Unit>,
    filtersStore: Store<FiltersState, FiltersAction>,
    store: Store<TimePunchListScreenState, TimePunchListScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = {
            ActionBar(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                entityType = EntityType.TIME_PUNCH,
                onAddClick = { router.navigate(screen = Screen.TimePunchDetail(uuid = uuid().toString(), new = true), option = Router.NavOption.REPLACE_LAST) },
            )
        },
        bottomBar = { Navigation(sessionStore = sessionStore, selected = NavigationRoute.TIME_PUNCH) },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Button(
                modifier = Modifier
                    .testTag(tag = "time_punch_punch")
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                style = ButtonStyle.FILLED,
                enabled = state.userUuid != null,
                text = getTranslation(key = "time_punch_punch"),
                onClick = { store.send(action = TimePunchListScreenAction.Punch) }
            )
            TimePunchList(
                items = state.timePunches,
                onClick = { store.send(action = TimePunchListScreenAction.SelectTimePunch(timePunchUuid = it.uuid)) }
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TimePunchListScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "time_punch_punch" to "Punch the clock"
        )
    )
    TimePunchListScreen(
        sessionStore = previewStore(initialState = SessionState()),
        filtersStore = previewStore(initialState = FiltersState()),
        store = previewStore(
            initialState = TimePunchListScreenState(
                timePunches = persistentListOf(TimePunchItem(time = "2026-11-09T09:00:00Z", userName = "Vitor Ribeiro", nfc = false)),
                userUuid = uuid()
            )
        )
    )
}
