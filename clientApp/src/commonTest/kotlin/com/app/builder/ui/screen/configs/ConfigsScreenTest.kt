package com.app.builder.ui.screen.configs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import com.app.builder.Dependency.getUserDependency
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.list.ConfigValue
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.component.useravatar.UserAvatarState
import com.app.builder.ui.component.useravatar.UserAvatarStore

class ConfigsScreenTest: TestCase() {

    /** Verifies that the client flags screen displays the flag list and reflects a toggled value update. */
    @Test
    fun clientFlagsScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.CLIENT_FLAG), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val configScreenStore = ConfigScreenStore(state = ConfigScreenState(configType = ConfigType.CLIENT_FLAG), configUseCases = authenticatedUseCases.configUseCases)

        val userAvatarStore = UserAvatarStore(state = UserAvatarState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI { ConfigsScreen(navigationStore = navigationStore, userAvatarStore = userAvatarStore, store = configScreenStore) }

        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        assertTrue(actual = navigationStore.state.items.first { it.text == "client_flag" }.selected)
        onNodeWithTag(testTag = "config_list").assertIsDisplayed()

        val item = configScreenStore.state.items.first { it.name == "http" }
        assertEquals(expected = ConfigValue.Toggle(value = FakeData.clientFlags.http), actual = item.value)

        configScreenStore.send(action = ConfigScreenAction.UpdateValue(item = item, value = ConfigValue.Toggle(value = false)))
        waitForIdle()

        val updated = configScreenStore.state.items.first { it.name == "http" }
        assertEquals(expected = ConfigValue.Toggle(value = false), actual = updated.value)
    }

    /** Verifies that the client configs screen displays the config list and reflects a form value update. */
    @Test
    fun clientConfigsScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.CLIENT_CONFIG), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val configScreenStore = ConfigScreenStore(state = ConfigScreenState(configType = ConfigType.CLIENT_CONFIG), configUseCases = authenticatedUseCases.configUseCases)

        val userAvatarStore = UserAvatarStore(state = UserAvatarState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI { ConfigsScreen(navigationStore = navigationStore, userAvatarStore = userAvatarStore, store = configScreenStore) }

        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        assertTrue(actual = navigationStore.state.items.first { it.text == "client_config" }.selected)
        onNodeWithTag(testTag = "config_list").assertIsDisplayed()

        val item = configScreenStore.state.items.first { it.name == "httpConnectTimeoutMillis" }
        assertEquals(expected = ConfigValue.Form(value = FakeData.clientConfigs.httpConnectTimeoutMillis.toString()), actual = item.value)

        configScreenStore.send(action = ConfigScreenAction.UpdateValue(item = item, value = ConfigValue.Form(value = "123")))
        waitForIdle()

        val updated = configScreenStore.state.items.first { it.name == "httpConnectTimeoutMillis" }
        assertEquals(expected = ConfigValue.Form(value = "123"), actual = updated.value)
    }

    /** Verifies that the server flags screen displays the flag list and reflects a toggled value update. */
    @Test
    fun serverFlagsScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.SERVER_FLAG), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val configScreenStore = ConfigScreenStore(state = ConfigScreenState(configType = ConfigType.SERVER_FLAG), configUseCases = authenticatedUseCases.configUseCases)

        val userAvatarStore = UserAvatarStore(state = UserAvatarState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI { ConfigsScreen(navigationStore = navigationStore, userAvatarStore = userAvatarStore, store = configScreenStore) }

        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        assertTrue(actual = navigationStore.state.items.first { it.text == "server_flag" }.selected)
        onNodeWithTag(testTag = "config_list").assertIsDisplayed()

        val item = configScreenStore.state.items.first { it.name == "probe" }
        assertEquals(expected = ConfigValue.Toggle(value = FakeData.serverFlags.probe), actual = item.value)

        configScreenStore.send(action = ConfigScreenAction.UpdateValue(item = item, value = ConfigValue.Toggle(value = false)))
        waitForIdle()

        val updated = configScreenStore.state.items.first { it.name == "probe" }
        assertEquals(expected = ConfigValue.Toggle(value = false), actual = updated.value)
    }

    /** Verifies that the server configs screen displays the config list and reflects a form value update. */
    @Test
    fun serverConfigsScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.SERVER_CONFIG), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val configScreenStore = ConfigScreenStore(state = ConfigScreenState(configType = ConfigType.SERVER_CONFIG), configUseCases = authenticatedUseCases.configUseCases)

        val userAvatarStore = UserAvatarStore(state = UserAvatarState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI { ConfigsScreen(navigationStore = navigationStore, userAvatarStore = userAvatarStore, store = configScreenStore) }

        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        assertTrue(actual = navigationStore.state.items.first { it.text == "server_config" }.selected)
        onNodeWithTag(testTag = "config_list").assertIsDisplayed()

        val item = configScreenStore.state.items.first { it.name == "accessTokenValidity" }
        assertEquals(expected = ConfigValue.Form(value = FakeData.serverConfigs.accessTokenValidity.toString()), actual = item.value)

        configScreenStore.send(action = ConfigScreenAction.UpdateValue(item = item, value = ConfigValue.Form(value = "123")))
        waitForIdle()

        val updated = configScreenStore.state.items.first { it.name == "accessTokenValidity" }
        assertEquals(expected = ConfigValue.Form(value = "123"), actual = updated.value)
    }
}
