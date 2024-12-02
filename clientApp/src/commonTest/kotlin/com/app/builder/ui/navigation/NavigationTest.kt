package com.app.builder.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class NavigationTest: TestCase() {

    /** Verifies that navigating to an unhandled screen within splash navigation falls back to the error screen. */
    @Test
    fun fallback() = runUITest {
        router.navigate(screen = Screen.Login)

        setUI {
            LoadingNavigation()
        }
        waitForIdle()
        assertEquals(expected = listOf(Screen.Error(error = true)), actual = router.backStack.toList())
    }

    /** Verifies that navigating to the splash screen keeps it on the back stack within splash navigation. */
    @Test
    fun splash() = runUITest {
        router.navigate(screen = Screen.Splash)

        setUI {
            LoadingNavigation()
        }
        waitForIdle()
        assertEquals(expected = listOf(Screen.Splash), actual = router.backStack.toList())
    }

    /** Verifies that navigating to the login screen keeps it on the back stack within unauthenticated navigation. */
    @Test
    fun unauthenticated() = runUITest {
        val useCases = dependency.get().userGraph.useCases
        router.navigate(screen = Screen.Login)

        setUI {
            UnauthenticatedNavigation(useCases = useCases)
        }
        waitForIdle()
        assertEquals(expected = listOf(Screen.Login), actual = router.backStack.toList())
    }

    /** Verifies that navigating to the task list screen keeps it on the back stack within authenticated navigation. */
    @Test
    fun authenticated() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        router.navigate(screen = Screen.TaskList)

        setUI {
            AuthenticatedNavigation(useCases = authenticatedUseCases)
        }
        waitForIdle()
        assertEquals(expected = listOf(Screen.TaskList), actual = router.backStack.toList())
    }

    /** Verifies that navigating to the login screen while the login client flag is disabled falls back to the error screen. */
    @Test
    fun fallBackToErrorScreen() = runUITest {
        val useCases = dependency.get().userGraph.useCases
        ClientFlags.set { it.copy(login = false) }
        router.navigate(screen = Screen.Login)

        setUI {
            UnauthenticatedNavigation(useCases = useCases)
        }
        waitForIdle()

        assertEquals(expected = listOf(Screen.Error(error = true)), actual = router.backStack.toList())
    }
}
