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
import com.app.builder.ui.showcase.ShapesShowcase
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The design system's shape catalog, customizing the theme's shapes.
 *
 * @param modifier [Modifier] applied to the root container.
 * @param navigationStore Drives the bottom navigation bar.
 * @param store The store persisting the customized theme.
 */
@Composable
fun DesignShapesScreen(
    modifier: Modifier = Modifier,
    navigationStore: Store<NavigationState, Unit>,
    store: Store<Unit, DesignScreenAction>,
) {
    val router = LocalRouter.current

    Screen(
        modifier = modifier,
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = { TopBar(title = { Text(text = "design_shapes") }) },
        bottomBar = { Navigation(store = navigationStore, selected = NavigationRoute.DESIGN_SHAPES) },
    ) {
        ShapesShowcase(onThemeChange = { store.send(action = DesignScreenAction.SaveTheme(customTheme = it)) })
    }
}

@PreviewLightDark
@Composable
private fun DesignShapesScreenPreview() = Preview {
    DesignShapesScreen(navigationStore = previewStore(initialState = NavigationState()), store = previewStore(initialState = Unit))
}
