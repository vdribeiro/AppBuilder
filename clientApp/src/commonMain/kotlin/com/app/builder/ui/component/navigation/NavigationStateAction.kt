package com.app.builder.ui.component.navigation

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * State for the app's navigation bar, holding the routes the current user can open.
 *
 * @property routes The routes the current user can open, in display order.
 */
data class NavigationState(
    val routes: ImmutableList<NavigationRoute> = persistentListOf(),
)

/** The sections of the navigation bar, each showing its own routes. */
enum class NavigationSection {
    /** The app's domain features. */
    MAIN,
    /** The app's capabilities and configurations. */
    SYSTEM,
    /** The design system's catalog. */
    DESIGN
}

/**
 * Navigation routes.
 *
 * @property section The section of the navigation bar showing the route.
 */
enum class NavigationRoute(val section: NavigationSection) {
    HOME(section = NavigationSection.MAIN),
    TASK(section = NavigationSection.MAIN),
    USER(section = NavigationSection.MAIN),
    REGISTRY(section = NavigationSection.MAIN),
    DEVICE_LOCATION(section = NavigationSection.SYSTEM),
    NFC(section = NavigationSection.SYSTEM),
    CAMERA(section = NavigationSection.SYSTEM),
    DEVICE_FILE(section = NavigationSection.SYSTEM),
    AUDIO(section = NavigationSection.SYSTEM),
    NOTIFICATION(section = NavigationSection.SYSTEM),
    CONFIG(section = NavigationSection.SYSTEM),
    TRANSLATION(section = NavigationSection.SYSTEM),
    DESIGN_COMPONENTS(section = NavigationSection.DESIGN),
    DESIGN_COLORS(section = NavigationSection.DESIGN),
    DESIGN_SHAPES(section = NavigationSection.DESIGN),
    DESIGN_TYPOGRAPHY(section = NavigationSection.DESIGN)
}
