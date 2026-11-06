package com.app.builder.ui.navigation.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.showcase.TypographyShowcase
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The design system's typography catalog.
 *
 * @param modifier [Modifier] applied to the root container.
 * @param sessionStore The session store shared by every screen.
 */
@Composable
fun DesignTypographyScreen(
    modifier: Modifier = Modifier,
    sessionStore: Store<SessionState, Unit>,
) {
    val router = LocalRouter.current

    Screen(
        modifier = modifier,
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = { ActionBar(sessionStore = sessionStore) },
        bottomBar = { Navigation(sessionStore = sessionStore, selected = NavigationRoute.DESIGN_TYPOGRAPHY) },
    ) {
        TypographyShowcase()
    }
}

@PreviewLightDark
@Composable
private fun DesignTypographyScreenPreview() = Preview {
    DesignTypographyScreen(
        sessionStore = previewStore(initialState = SessionState()),
    )
}
