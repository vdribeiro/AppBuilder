package com.app.builder.ui.component.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.app.builder.core.config.ClientFlags
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class NavigationRouteTest: TestCase() {

    /** Verifies that without an account there are no routes to open. */
    @Test
    fun noAccount() = runUnitTest {
        assertEquals(expected = emptyList(), actual = permittedRoutes(account = null))
    }

    /** Verifies that an account with every permission opens every domain route, in display order. */
    @Test
    fun everyPermission() = runUnitTest {
        val routes = permittedRoutes(account = FakeData.adminAccount)

        assertEquals(expected = listOf(NavigationRoute.TASK, NavigationRoute.EQUIPMENT, NavigationRoute.USER, NavigationRoute.REGISTRY, NavigationRoute.HOME), actual = routes.filter { it.section == NavigationSection.MAIN })
        assertTrue(actual = NavigationRoute.CONFIG in routes)
        assertTrue(actual = NavigationRoute.TRANSLATION in routes)
    }

    /** Verifies that a route whose client feature flag is off is left out, even with the permission. */
    @Test
    fun featureFlagOff() = runUnitTest {
        ClientFlags.set { it.copy(tasks = false, equipments = false, design = false) }

        val routes = permittedRoutes(account = FakeData.adminAccount)

        assertFalse(actual = NavigationRoute.TASK in routes)
        assertFalse(actual = NavigationRoute.EQUIPMENT in routes)
        assertTrue(actual = routes.none { it.section == NavigationSection.DESIGN })
    }
}
