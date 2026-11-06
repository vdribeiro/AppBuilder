package com.app.builder.ui.navigation.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.app.builder.ui.Preview
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.core.bar.TopBar
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.showcase.ComponentsShowcase
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The design system's component catalog.
 *
 * @param modifier [Modifier] applied to the root container.
 * @param navigationStore Drives the bottom navigation bar.
 */
@Composable
fun DesignComponentsScreen(
    modifier: Modifier = Modifier,
    navigationStore: Store<NavigationState, Unit>,
) {
    val router = LocalRouter.current

    Screen(
        modifier = modifier,
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = { TopBar(title = { Text(text = "design_components") }) },
        bottomBar = { Navigation(store = navigationStore, selected = NavigationRoute.DESIGN_COMPONENTS) },
    ) {
        ComponentsShowcase()
    }
}

@PreviewLightDark
@Composable
private fun DesignComponentsScreenPreview() = Preview {
    DesignComponentsScreen(navigationStore = previewStore(initialState = NavigationState()))
}
