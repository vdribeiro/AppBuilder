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
import com.app.builder.core.security.uuid
import com.app.builder.domain.EntityType
import com.app.builder.domain.TimePunch
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalSplitScreen
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.card.TimePunchCard
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The user-facing Time Punch Detail Screen, showing a single time punch.
 *
 * @param sessionStore The session store shared by every screen.
 * @param filtersStore The time punch filters shared by every time punch screen, shown when the time punch list is beside this screen.
 * @param store Provides the time punch detail state and receives its actions.
 */
@Composable
fun TimePunchDetailScreen(
    sessionStore: Store<SessionState, Unit>,
    filtersStore: Store<FiltersState, FiltersAction>,
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
                onDeleteClick = if (timePunch != null) {
                    { store.send(action = if (it) TimePunchDetailScreenAction.Delete else TimePunchDetailScreenAction.Restore) }
                } else null,
            )
        },
        bottomBar = { Navigation(sessionStore = sessionStore, selected = NavigationRoute.TIME_PUNCH) },
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState())) {
            TimePunchCard(
                uuid = timePunch?.uuid?.toString(),
                modifiedAt = timePunch?.modifiedAt?.toString(),
                deletedAt = timePunch?.deletedAt?.toString(),
                userName = state.userName ?: timePunch?.userUuid?.toString(),
                time = timePunch?.time?.toString(),
                source = timePunch?.source?.translationKey,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TimePunchDetailScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "time_punch_user" to "User",
            "time_punch_time" to "Time",
            "time_punch_source" to "Source",
            "time_punch_source_manual" to "Manual"
        )
    )
    TimePunchDetailScreen(
        sessionStore = previewStore(initialState = SessionState()),
        filtersStore = previewStore(initialState = FiltersState()),
        store = previewStore(
            initialState = TimePunchDetailScreenState(
                timePunch = TimePunch(userUuid = uuid()),
                userName = "Vitor Ribeiro"
            )
        )
    )
}
