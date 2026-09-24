package com.app.builder.ui.navigation.provider

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.authenticatedScreen
import com.app.builder.ui.screen.design.DesignScreen
import com.app.builder.ui.screen.design.DesignSection

/** The design section routes, one per [DesignSection]. */
fun EntryProviderScope<NavKey>.designProvider() {
    entry<Screen.DesignComponents> { Design(section = DesignSection.COMPONENTS) }
    entry<Screen.DesignColors> { Design(section = DesignSection.COLORS) }
    entry<Screen.DesignShapes> { Design(section = DesignSection.SHAPES) }
    entry<Screen.DesignTypography> { Design(section = DesignSection.TYPOGRAPHY) }
}

/**
 * Renders a [DesignScreen], routing its navigation bar back into the app's router.
 *
 * @param section The section to display.
 */
@Composable
private fun Design(section: DesignSection) {
    val router = LocalRouter.current
    DesignScreen(
        section = section,
        onSectionClick = { router.navigate(screen = it.toScreen(), option = Router.NavOption.CLEAR) },
        onAppClick = { router.navigate(screen = authenticatedScreen, option = Router.NavOption.CLEAR) },
    )
}

/**
 * Converts a design section to the screen displaying it.
 *
 * @return The screen for this section.
 */
private fun DesignSection.toScreen(): Screen = when (this) {
    DesignSection.COMPONENTS -> Screen.DesignComponents
    DesignSection.COLORS -> Screen.DesignColors
    DesignSection.SHAPES -> Screen.DesignShapes
    DesignSection.TYPOGRAPHY -> Screen.DesignTypography
}
