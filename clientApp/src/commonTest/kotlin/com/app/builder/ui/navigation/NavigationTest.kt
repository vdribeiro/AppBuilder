package com.app.builder.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.deeplink.DeepLink
import com.app.builder.domain.gateway.registry.toRegistrySchema
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.test.count
import com.app.builder.ui.LocalSplitScreen

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
            .getUserDependency(account = FakeData.adminAccount)
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
            .getUserDependency(account = FakeData.adminAccount)
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

    /** Verifies that the task list's add action opens a new task in edit mode, that saving stores it, and that the task detail's delete action marks it deleted. */
    @Test
    fun taskRoutesAddAndDelete() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        router.navigate(screen = Screen.TaskList)

        setUI {
            AuthenticatedNavigation(useCases = authenticatedUseCases)
        }
        waitForIdle()

        onNodeWithTag(testTag = "action_add").performClick()
        waitUntil { router.backStack.lastOrNull() is Screen.TaskDetail }
        val screen = assertIs<Screen.TaskDetail>(value = router.backStack.last())
        assertTrue(actual = screen.new)
        val taskUuid = Uuid.parse(uuidString = screen.uuid)
        waitForIdle()

        onNodeWithTag(testTag = "action_save").performClick()
        authenticatedUseCases.taskUseCases.observeTask(uuid = taskUuid).first { it != null }
        waitForIdle()

        onNodeWithTag(testTag = "action_delete").performClick()
        authenticatedUseCases.taskUseCases.observeTask(uuid = taskUuid).first { it?.deletedAt != null }
    }

    /** Verifies that, on a split screen, the task detail's top bar keeps the list filters and that they apply to the list beside it. */
    @Test
    fun splitTaskDetailShowsListFilters() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        val deletedTask = FakeData.task.copy(uuid = Uuid.random(), title = "Deleted", deletedAt = FakeData.task.modifiedAt)
        authenticatedUseCases.taskUseCases.upsertTask(task = FakeData.task)
        authenticatedUseCases.taskUseCases.upsertTask(task = deletedTask)
        router.navigate(screen = Screen.TaskList)
        router.navigate(screen = Screen.TaskDetail(uuid = FakeData.task.uuid.toString()))

        setUI {
            CompositionLocalProvider(value = LocalSplitScreen provides true) {
                AuthenticatedNavigation(useCases = authenticatedUseCases)
            }
        }
        waitUntil { onAllNodesWithTag(testTag = "task_card_0").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag(testTag = "task_card_1").assertDoesNotExist()

        onNodeWithTag(testTag = "action_expand").performClick()
        onNodeWithTag(testTag = "action_search").assertIsDisplayed()
        onNodeWithTag(testTag = "action_show_deleted").assertIsDisplayed().performClick()
        waitUntil { onAllNodesWithTag(testTag = "task_card_1").fetchSemanticsNodes().isNotEmpty() }
    }

    /** Verifies that, on a split screen, expanding the filters on the task detail keeps them expanded on the list once the detail is closed, and the other way round. */
    @Test
    fun splitTaskFiltersStayExpanded() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.taskUseCases.upsertTask(task = FakeData.task)
        router.navigate(screen = Screen.TaskList)
        router.navigate(screen = Screen.TaskDetail(uuid = FakeData.task.uuid.toString()))

        setUI {
            CompositionLocalProvider(value = LocalSplitScreen provides true) {
                AuthenticatedNavigation(useCases = authenticatedUseCases)
            }
        }
        waitForIdle()
        onNodeWithTag(testTag = "action_search").assertDoesNotExist()

        onNodeWithTag(testTag = "action_expand").performClick()
        onNodeWithTag(testTag = "action_search").assertIsDisplayed()

        router.back()
        waitForIdle()
        assertEquals(expected = listOf(Screen.TaskList), actual = router.backStack.toList())
        onNodeWithTag(testTag = "action_search").assertIsDisplayed()

        onNodeWithTag(testTag = "action_expand").performClick()
        onNodeWithTag(testTag = "action_search").assertDoesNotExist()

        router.navigate(screen = Screen.TaskDetail(uuid = FakeData.task.uuid.toString()))
        waitForIdle()
        onNodeWithTag(testTag = "action_expand").assertIsDisplayed()
        onNodeWithTag(testTag = "action_search").assertDoesNotExist()
    }

    /** Verifies that, on a split screen, a search typed on the task detail stays open with its query on the list once the detail is closed. */
    @Test
    fun splitTaskSearchStaysOpen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.taskUseCases.upsertTask(task = FakeData.task)
        router.navigate(screen = Screen.TaskList)
        router.navigate(screen = Screen.TaskDetail(uuid = FakeData.task.uuid.toString()))
        val taskPreferences = AppFile.TaskPreferences.cache()

        setUI {
            CompositionLocalProvider(value = LocalSplitScreen provides true) {
                AuthenticatedNavigation(useCases = authenticatedUseCases)
            }
        }
        waitForIdle()

        onNodeWithTag(testTag = "action_expand").performClick()
        onNodeWithTag(testTag = "action_search").performClick()
        onNodeWithTag(testTag = "action_search_input").performTextInput(text = "query")
        waitUntil { taskPreferences.value?.search == "query" }

        router.back()
        waitForIdle()
        assertEquals(expected = listOf(Screen.TaskList), actual = router.backStack.toList())
        onNodeWithTag(testTag = "action_search_input").assertTextContains(value = "query")

        onNodeWithTag(testTag = "action_search_close").performClick()
        onNodeWithTag(testTag = "action_search").assertIsDisplayed()
        waitUntil { taskPreferences.value?.search == "" }
    }

    /** Verifies that, on a split screen, the registry detail's top bar keeps the list filters and that they apply to the list beside it. */
    @Test
    fun splitRegistryDetailShowsListFilters() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        val registryDao = dependency.get()
            .database
            .registryQueries
        val otherRegistry = FakeData.registry.copy(requestUuid = Uuid.random(), model = "Galaxy S24")
        registryDao.upsertRegistry(Registry = FakeData.registry.toRegistrySchema())
        registryDao.upsertRegistry(Registry = otherRegistry.toRegistrySchema())
        router.navigate(screen = Screen.RegistryList)
        router.navigate(screen = Screen.RegistryDetail(uuid = FakeData.registry.requestUuid.toString()))

        setUI {
            CompositionLocalProvider(value = LocalSplitScreen provides true) {
                AuthenticatedNavigation(useCases = authenticatedUseCases)
            }
        }
        // Registry cards are taller than the screen, so the list row count is checked rather than each card.
        waitUntil { runCatching { onNodeWithTag(testTag = "registry_list").count(count = 2) }.isSuccess }

        onNodeWithTag(testTag = "action_expand").performClick()
        onNodeWithTag(testTag = "action_show_deleted").assertDoesNotExist()
        onNodeWithTag(testTag = "action_search").assertIsDisplayed().performClick()
        onNodeWithTag(testTag = "action_search_input").performTextInput(text = otherRegistry.model)
        waitUntil { runCatching { onNodeWithTag(testTag = "registry_list").count(count = 1) }.isSuccess }
    }

    /** Verifies that the design routes navigate between their sections and back to the app's authenticated landing screen. */
    @Test
    fun designRoutesNavigate() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
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

    /** Verifies that a back stack holding duplicate and circular screens renders and pops back without crashing. */
    @Test
    fun duplicateScreensRenderAndPop() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.taskUseCases.upsertTask(task = FakeData.task)
        val detail = Screen.TaskDetail(uuid = FakeData.task.uuid.toString())
        val screens = listOf(Screen.Configs, Screen.TaskList, detail, Screen.TaskList, detail, Screen.Configs, Screen.Configs)
        router.navigate(screen = screens.first())

        setUI {
            AuthenticatedNavigation(useCases = authenticatedUseCases)
        }
        waitForIdle()

        screens.drop(n = 1).forEach { screen ->
            router.navigate(screen = screen)
            waitForIdle()
        }
        assertEquals(expected = screens, actual = router.backStack.toList())

        (screens.size - 1 downTo 1).forEach { size ->
            router.back()
            waitForIdle()
            assertEquals(expected = screens.take(n = size), actual = router.backStack.toList())
        }
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
            DeepLink.Entity(entityUuid = null, entityType = EntityType.ACCOUNT) to listOf(Screen.Login),
            DeepLink.Entity(entityUuid = null, entityType = EntityType.USER) to listOf(Screen.UserList),
            DeepLink.Entity(entityUuid = uuid, entityType = EntityType.USER) to listOf(Screen.UserList, Screen.UserDetail(uuid = uuid.toString())),
            DeepLink.Entity(entityUuid = null, entityType = EntityType.DEVICE_LOCATION) to listOf(Screen.DeviceLocation),
            DeepLink.Entity(entityUuid = null, entityType = EntityType.NFC) to listOf(Screen.NFC),
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
