package com.app.builder.ui.component.navigation

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.app.builder.ui.core.navigation.NavigationItem

/**
 * State for the app's navigation bar, holding the items to display and which entity type is selected.
 *
 * @property items Navigation items to display.
 * @property selected Selected route of the current navigation item.
 */
data class NavigationState(
    val items: ImmutableList<NavigationItem> = persistentListOf(),
    val selected: NavigationRoute? = null
)

/** Navigation routes. */
enum class NavigationRoute {
    TASK,
    FILE,
    USER,
    REGISTRY,
    DEVICE_LOCATION,
    NFC,
    CAMERA,
    AUDIO,
    NOTIFICATION,
    CLIENT_FLAG,
    CLIENT_CONFIG,
    SERVER_FLAG,
    SERVER_CONFIG,
    TRANSLATION
}
