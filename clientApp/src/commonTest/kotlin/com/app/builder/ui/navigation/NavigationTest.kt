package com.app.builder.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.domain.EntityType
import com.app.builder.domain.deeplink.DeepLink
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

    /** Verifies that every route registered by authenticated navigation renders without falling back to the error screen. */
    @Test
    fun everyAuthenticatedRouteRenders() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.taskUseCases.upsertTask(task = FakeData.task)
        // Location capture is off by default, which leaves its route unregistered.
        ClientFlags.set { it.copy(locationCapture = true) }
        val screens = listOf(
            Screen.Configs,
            Screen.Home,
            Screen.DesignComponents,
            Screen.DesignColors,
            Screen.DesignShapes,
            Screen.DesignTypography,
            Screen.UserProfile,
            Screen.UserList,
            Screen.DeviceLocation,
            Screen.NFC,
            Screen.Camera,
            Screen.DeviceFiles,
            Screen.Audio,
            Screen.Push,
            Screen.TaskList,
            Screen.TaskDetail(uuid = FakeData.task.uuid.toString()),
        )
        router.navigate(screen = screens.first())

        setUI {
            AuthenticatedNavigation(useCases = authenticatedUseCases)
        }

        screens.forEach { screen ->
            router.navigate(screen = screen, option = Router.NavOption.CLEAR)
            waitForIdle()
            assertEquals(expected = listOf(screen), actual = router.backStack.toList())
        }
    }

    /** Verifies that the task list's add action creates a task and opens it, and that the task detail's delete action marks it deleted. */
    @Test
    fun taskRoutesAddAndDelete() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        router.navigate(screen = Screen.TaskList)

        setUI {
            AuthenticatedNavigation(useCases = authenticatedUseCases)
        }
        waitForIdle()

        onNodeWithTag(testTag = "action_add").performClick()
        waitUntil { router.backStack.lastOrNull() is Screen.TaskDetail }
        val taskUuid = Uuid.parse(uuidString = assertIs<Screen.TaskDetail>(value = router.backStack.last()).uuid)
        authenticatedUseCases.taskUseCases.observeTask(uuid = taskUuid).first { it != null }
        waitForIdle()

        onNodeWithTag(testTag = "action_delete").performClick()
        authenticatedUseCases.taskUseCases.observeTask(uuid = taskUuid).first { it?.deletedAt != null }
    }

    /** Verifies that the design routes navigate between their sections and back to the app's authenticated landing screen. */
    @Test
    fun designRoutesNavigate() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        router.navigate(screen = Screen.DesignComponents)

        setUI {
            AuthenticatedNavigation(useCases = authenticatedUseCases)
        }
        waitForIdle()

        onNodeWithText(text = "Colors").performClick()
        waitForIdle()
        assertEquals(expected = listOf(Screen.DesignColors), actual = router.backStack.toList())

        onNodeWithText(text = "Shapes").performClick()
        waitForIdle()
        assertEquals(expected = listOf(Screen.DesignShapes), actual = router.backStack.toList())

        onNodeWithText(text = "Typography").performClick()
        waitForIdle()
        assertEquals(expected = listOf(Screen.DesignTypography), actual = router.backStack.toList())

        onNodeWithText(text = "Components").performClick()
        waitForIdle()
        assertEquals(expected = listOf(Screen.DesignComponents), actual = router.backStack.toList())

        onNodeWithText(text = "App").performClick()
        waitForIdle()
        assertEquals(expected = listOf(authenticatedScreen), actual = router.backStack.toList())
    }

    /** Verifies that the unauthenticated landing screen prefers home, then login, then the error screen. */
    @Test
    fun unauthenticatedLandingScreen() = runUnitTest {
        assertEquals(expected = Screen.Home, actual = unauthenticatedScreen)
        ClientFlags.set { it.copy(home = false) }
        assertEquals(expected = Screen.Login, actual = unauthenticatedScreen)
        ClientFlags.set { it.copy(login = false) }
        assertEquals(expected = Screen.Error(error = true), actual = unauthenticatedScreen)
    }

    /** Verifies that the authenticated landing screen prefers home, then the user profile, then the error screen. */
    @Test
    fun authenticatedLandingScreen() = runUnitTest {
        assertEquals(expected = Screen.Home, actual = authenticatedScreen)
        ClientFlags.set { it.copy(home = false) }
        assertEquals(expected = Screen.UserProfile, actual = authenticatedScreen)
        ClientFlags.set { it.copy(userProfile = false) }
        assertEquals(expected = Screen.Error(error = true), actual = authenticatedScreen)
    }

    /** Verifies that unauthenticated navigation without use cases only offers the error screen. */
    @Test
    fun unauthenticatedWithoutUseCasesFallsBack() = runUITest {
        router.navigate(screen = Screen.Login)

        setUI {
            UnauthenticatedNavigation(useCases = null)
        }
        waitForIdle()

        assertEquals(expected = listOf(Screen.Error(error = true)), actual = router.backStack.toList())
    }

    /** Verifies that authenticated navigation without use cases only offers the error screen. */
    @Test
    fun authenticatedWithoutUseCasesFallsBack() = runUITest {
        router.navigate(screen = Screen.TaskList)

        setUI {
            AuthenticatedNavigation(useCases = null)
        }
        waitForIdle()

        assertEquals(expected = listOf(Screen.Error(error = true)), actual = router.backStack.toList())
    }

    /** Verifies that each deep link replaces the back stack with the screens leading to its entity. */
    @Test
    fun deepLinksSetTheBackStack() = runUITest {
        val uuid = Uuid.random()
        val expectations = listOf(
            DeepLink.Entity(entityUuid = null, entityType = EntityType.CLIENT_FLAG) to listOf(Screen.Configs),
            DeepLink.Entity(entityUuid = null, entityType = EntityType.CLIENT_CONFIG) to listOf(Screen.Configs),
            DeepLink.Entity(entityUuid = null, entityType = EntityType.SERVER_FLAG) to listOf(Screen.Configs),
            DeepLink.Entity(entityUuid = null, entityType = EntityType.SERVER_CONFIG) to listOf(Screen.Configs),
            DeepLink.Entity(entityUuid = null, entityType = EntityType.REGISTRY) to listOf(Screen.RegistryList),
            DeepLink.Entity(entityUuid = uuid, entityType = EntityType.REGISTRY) to listOf(Screen.RegistryList, Screen.RegistryDetail(uuid = uuid.toString())),
            DeepLink.Entity(entityUuid = null, entityType = EntityType.NOTIFICATION) to listOf(Screen.Push),
            DeepLink.Entity(entityUuid = null, entityType = EntityType.TRANSLATION) to listOf(Screen.Translations),
            DeepLink.Entity(entityUuid = null, entityType = EntityType.SESSION) to listOf(Screen.Login),
            DeepLink.Entity(entityUuid = null, entityType = EntityType.USER) to listOf(Screen.UserList),
            DeepLink.Entity(entityUuid = uuid, entityType = EntityType.USER) to listOf(Screen.UserList, Screen.UserDetail(uuid = uuid.toString())),
            DeepLink.Entity(entityUuid = null, entityType = EntityType.DEVICE_LOCATION) to listOf(Screen.DeviceLocation),
            DeepLink.Entity(entityUuid = null, entityType = EntityType.TASK) to listOf(Screen.TaskList),
            DeepLink.Entity(entityUuid = uuid, entityType = EntityType.TASK) to listOf(Screen.TaskList, Screen.TaskDetail(uuid = uuid.toString())),
        )
        router.navigate(screen = Screen.Home)

        setUI {
            DeepLinks()
        }

        expectations.forEach { (deepLink, backStack) ->
            router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR)
            waitForIdle()
            DeepLink.send(deepLink = deepLink)
            waitUntil { router.backStack.toList() == backStack }
        }
    }
}
