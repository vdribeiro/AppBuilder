package com.app.builder.ui.component.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.app.builder.core.config.ClientFlags
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class NavigationRouteTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(buildings = true, equipments = true, materials = true, users = true, tasks = true, timePunches = true, registries = true, home = true, design = true) }
    }

    /** Verifies that without an account there are no routes to open. */
    @Test
    fun noAccount() = runUnitTest {
        assertEquals(expected = emptyList(), actual = permittedRoutes(account = null, flags = ClientFlags.value))
    }

    /** Verifies that an account with every permission opens every domain route, in display order. */
    @Test
    fun everyPermission() = runUnitTest {
        val routes = permittedRoutes(account = FakeData.adminAccount, flags = ClientFlags.value)

        assertEquals(
            expected = listOf(NavigationRoute.BUILDING, NavigationRoute.EQUIPMENT, NavigationRoute.MATERIAL, NavigationRoute.USER, NavigationRoute.TASK, NavigationRoute.TIME_PUNCH, NavigationRoute.REGISTRY, NavigationRoute.HOME),
            actual = routes.filter { it.section == NavigationSection.MAIN })
        assertTrue(actual = NavigationRoute.CONFIG in routes)
        assertTrue(actual = NavigationRoute.TRANSLATION in routes)
    }

    /** Verifies that an account without any permission still opens the time punch route. */
    @Test
    fun timePunchNeedsNoPermission() = runUnitTest {
        val routes = permittedRoutes(account = FakeData.account.copy(permissions = emptyMap()), flags = ClientFlags.value)

        assertEquals(expected = listOf(NavigationRoute.TIME_PUNCH, NavigationRoute.HOME), actual = routes.filter { it.section == NavigationSection.MAIN })
    }

    /** Verifies that a route whose client feature flag is off is left out, even with the permission. */
    @Test
    fun featureFlagOff() = runUnitTest {
        ClientFlags.set { it.copy(tasks = false, timePunches = false, equipments = false, materials = false, buildings = false, design = false) }

        val routes = permittedRoutes(account = FakeData.adminAccount, flags = ClientFlags.value)

        assertFalse(actual = NavigationRoute.TASK in routes)
        assertFalse(actual = NavigationRoute.TIME_PUNCH in routes)
        assertFalse(actual = NavigationRoute.EQUIPMENT in routes)
        assertFalse(actual = NavigationRoute.MATERIAL in routes)
        assertFalse(actual = NavigationRoute.BUILDING in routes)
        assertTrue(actual = routes.none { it.section == NavigationSection.DESIGN })
    }
}
