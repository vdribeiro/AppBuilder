package com.app.builder.ui.navigation.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
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
 * @param actionBarStore The store for action bar.
 * @param navigationStore Drives the bottom navigation bar.
 */
@Composable
fun DesignTypographyScreen(
    modifier: Modifier = Modifier,
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
) {
    val router = LocalRouter.current

    Screen(
        modifier = modifier,
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore, selected = NavigationRoute.DESIGN_TYPOGRAPHY) },
    ) {
        TypographyShowcase()
    }
}

@PreviewLightDark
@Composable
private fun DesignTypographyScreenPreview() = Preview {
    DesignTypographyScreen(
        actionBarStore = previewStore(initialState = ActionBarState(avatarName = "Tails")),
        navigationStore = previewStore(initialState = NavigationState())
    )
}
