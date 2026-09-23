package com.app.builder.ui.screen.configs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.onNodeWithTag
import com.app.builder.Dependency.getUserDependency
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.list.ConfigValue
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore

class ConfigsScreenTest: TestCase() {

    /**
     * Logs an admin in and renders the config screen with every store it needs.
     *
     * @return The [ConfigScreenStore] driving the rendered screen.
     */
    private suspend fun ComposeUiTest.setUpScreen(): ConfigScreenStore {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.CONFIG), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val configScreenStore = ConfigScreenStore(state = ConfigScreenState(), configUseCases = authenticatedUseCases.configUseCases, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val actionBarStore = ActionBarStore(state = ActionBarState(title = "config"), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI { ConfigsScreen(actionBarStore = actionBarStore, navigationStore = navigationStore, store = configScreenStore) }

        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        assertTrue(actual = navigationStore.state.items.first { it.text == "config" }.selected)
        onNodeWithTag(testTag = "config_navigation_rail").assertIsDisplayed()
        onNodeWithTag(testTag = "config_list").assertIsDisplayed()

        return configScreenStore
    }

    /**
     * Selects [configType] on the side navigation rail and waits until its entries are loaded.
     *
     * @param store The store driving the screen.
     * @param configType The config type to select.
     */
    private fun ComposeUiTest.selectType(store: ConfigScreenStore, configType: ConfigType) {
        store.send(action = ConfigScreenAction.SelectType(configType = configType))
        waitUntil { store.state.configType == configType && store.state.items.isNotEmpty() }
    }

    /** Verifies that the screen offers every config type an admin is allowed to browse in its side navigation rail. */
    @Test
    fun configTypes() = runUITest {
        val configScreenStore = setUpScreen()

        assertEquals(expected = ConfigType.entries.toList(), actual = configScreenStore.state.types.toList())
        assertEquals(expected = ConfigType.CLIENT_FLAG, actual = configScreenStore.state.configType)
    }

    /** Verifies that the client flags section displays the flag list and reflects a toggled value update. */
    @Test
    fun clientFlags() = runUITest {
        val configScreenStore = setUpScreen()

        selectType(store = configScreenStore, configType = ConfigType.CLIENT_FLAG)

        val item = configScreenStore.state.items.first { it.name == "http" }
        assertEquals(expected = ConfigValue.Toggle(value = FakeData.clientFlags.http), actual = item.value)

        configScreenStore.send(action = ConfigScreenAction.UpdateValue(item = item, value = ConfigValue.Toggle(value = false)))
        waitForIdle()

        val updated = configScreenStore.state.items.first { it.name == "http" }
        assertEquals(expected = ConfigValue.Toggle(value = false), actual = updated.value)
    }

    /** Verifies that the client configs section displays the config list and reflects a form value update. */
    @Test
    fun clientConfigs() = runUITest {
        val configScreenStore = setUpScreen()

        selectType(store = configScreenStore, configType = ConfigType.CLIENT_CONFIG)

        val item = configScreenStore.state.items.first { it.name == "httpConnectTimeoutMillis" }
        assertEquals(expected = ConfigValue.Form(value = FakeData.clientConfigs.httpConnectTimeoutMillis.toString()), actual = item.value)

        configScreenStore.send(action = ConfigScreenAction.UpdateValue(item = item, value = ConfigValue.Form(value = "123")))
        waitForIdle()

        val updated = configScreenStore.state.items.first { it.name == "httpConnectTimeoutMillis" }
        assertEquals(expected = ConfigValue.Form(value = "123"), actual = updated.value)
    }

    /** Verifies that the server flags section displays the flag list and reflects a toggled value update. */
    @Test
    fun serverFlags() = runUITest {
        val configScreenStore = setUpScreen()

        selectType(store = configScreenStore, configType = ConfigType.SERVER_FLAG)

        val item = configScreenStore.state.items.first { it.name == "probe" }
        assertEquals(expected = ConfigValue.Toggle(value = FakeData.serverFlags.probe), actual = item.value)

        configScreenStore.send(action = ConfigScreenAction.UpdateValue(item = item, value = ConfigValue.Toggle(value = false)))
        waitForIdle()

        val updated = configScreenStore.state.items.first { it.name == "probe" }
        assertEquals(expected = ConfigValue.Toggle(value = false), actual = updated.value)
    }

    /** Verifies that the server configs section displays the config list and reflects a form value update. */
    @Test
    fun serverConfigs() = runUITest {
        val configScreenStore = setUpScreen()

        selectType(store = configScreenStore, configType = ConfigType.SERVER_CONFIG)

        val item = configScreenStore.state.items.first { it.name == "accessTokenValidity" }
        assertEquals(expected = ConfigValue.Form(value = FakeData.serverConfigs.accessTokenValidity.toString()), actual = item.value)

        configScreenStore.send(action = ConfigScreenAction.UpdateValue(item = item, value = ConfigValue.Form(value = "123")))
        waitForIdle()

        val updated = configScreenStore.state.items.first { it.name == "accessTokenValidity" }
        assertEquals(expected = ConfigValue.Form(value = "123"), actual = updated.value)
    }
}
