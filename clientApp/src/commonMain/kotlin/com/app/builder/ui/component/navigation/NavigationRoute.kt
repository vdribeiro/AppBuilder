package com.app.builder.ui.component.navigation

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import com.app.builder.core.config.ClientFlags
import com.app.builder.domain.Account
import com.app.builder.domain.EntityType

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
    EQUIPMENT(section = NavigationSection.MAIN),
    MATERIAL(section = NavigationSection.MAIN),
    BUILDING(section = NavigationSection.MAIN),
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

/**
 * Resolves the routes the given account can open, from its permissions and the given client feature flags.
 *
 * @param account The current user's account, or `null` when no user is signed in.
 * @param flags The client feature flags.
 * @return The routes the account can open, in display order, or none without an account.
 */
fun permittedRoutes(account: Account?, flags: ClientFlags): ImmutableList<NavigationRoute> {
    val permissions = account?.permissions ?: return persistentListOf()
    return buildList {
        if (flags.home) add(NavigationRoute.HOME)
        if (flags.tasks && permissions[EntityType.TASK] != null) add(NavigationRoute.TASK)
        if (flags.buildings && permissions[EntityType.BUILDING] != null) add(NavigationRoute.BUILDING)
        if (flags.equipments && permissions[EntityType.EQUIPMENT] != null) add(NavigationRoute.EQUIPMENT)
        if (flags.materials && permissions[EntityType.MATERIAL] != null) add(NavigationRoute.MATERIAL)
        if (flags.users && permissions[EntityType.USER] != null) add(NavigationRoute.USER)
        if (flags.registries && permissions[EntityType.REGISTRY] != null) add(NavigationRoute.REGISTRY)
        if (listOf(
                EntityType.CLIENT_FLAG,
                EntityType.CLIENT_CONFIG,
                EntityType.SERVER_FLAG,
                EntityType.SERVER_CONFIG
            ).any { permissions[it] != null }
        ) add(NavigationRoute.CONFIG)
        if (flags.notifications && permissions[EntityType.NOTIFICATION] != null) add(NavigationRoute.NOTIFICATION)
        if (flags.locationCapture && permissions[EntityType.DEVICE_LOCATION] != null) add(NavigationRoute.DEVICE_LOCATION)
        if (flags.nfc && permissions[EntityType.NFC] != null) add(NavigationRoute.NFC)
        if (flags.camera) add(NavigationRoute.CAMERA)
        if (flags.music) add(NavigationRoute.AUDIO)
        if (flags.deviceFiles) add(NavigationRoute.DEVICE_FILE)
        if (permissions[EntityType.TRANSLATION] != null) add(NavigationRoute.TRANSLATION)
        if (flags.design) addAll(
            elements = listOf(
                NavigationRoute.DESIGN_COMPONENTS,
                NavigationRoute.DESIGN_COLORS,
                NavigationRoute.DESIGN_SHAPES,
                NavigationRoute.DESIGN_TYPOGRAPHY
            )
        )
    }.toPersistentList()
}
