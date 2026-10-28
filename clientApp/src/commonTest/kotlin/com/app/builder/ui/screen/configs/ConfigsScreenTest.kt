package com.app.builder.ui.screen.configs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientConfigs
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.list.ConfigItem
import com.app.builder.ui.component.list.ConfigValue
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore

class ConfigsScreenTest: TestCase() {

    /**
     * Logs an admin in and renders the config screen with every store it needs, waiting until every section has loaded.
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
        val actionBarStore = ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI { ConfigsScreen(actionBarStore = actionBarStore, navigationStore = navigationStore, store = configScreenStore) }

        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        assertTrue(actual = navigationStore.state.items.first { it.text == "config" }.selected)
        onNodeWithTag(testTag = "config_navigation_rail").assertIsDisplayed()
        onNodeWithTag(testTag = "config_sections").assertIsDisplayed()

        waitUntil { configScreenStore.state.sections.size == ConfigType.entries.size }

        return configScreenStore
    }

    /**
     * Finds one entry of a section.
     *
     * @param configType The section holding the entry.
     * @param name The name of the entry.
     * @return The matching [ConfigItem].
     */
    private fun ConfigScreenStore.item(configType: ConfigType, name: String): ConfigItem =
        state.sections.first { it.configType == configType }.items.first { it.name == name }

    /** Verifies that every section an admin is allowed to browse is stacked on the page, in config type order. */
    @Test
    fun configSections() = runUITest {
        val configScreenStore = setUpScreen()

        assertEquals(expected = ConfigType.entries.toList(), actual = configScreenStore.state.sections.map { it.configType })
        assertTrue(actual = configScreenStore.state.sections.all { it.items.isNotEmpty() })
    }

    /** Verifies that the client flags section reflects a toggled value update without disturbing the other sections. */
    @Test
    fun clientFlags() = runUITest {
        val configScreenStore = setUpScreen()

        val item = configScreenStore.item(configType = ConfigType.CLIENT_FLAG, name = "http")
        assertEquals(expected = ConfigValue.Toggle(value = FakeData.clientFlags.http), actual = item.value)

        val others = configScreenStore.state.sections.filterNot { it.configType == ConfigType.CLIENT_FLAG }

        configScreenStore.send(action = ConfigScreenAction.UpdateValue(configType = ConfigType.CLIENT_FLAG, item = item, value = ConfigValue.Toggle(value = false)))
        waitForIdle()

        assertEquals(expected = ConfigValue.Toggle(value = false), actual = configScreenStore.item(configType = ConfigType.CLIENT_FLAG, name = "http").value)
        assertEquals(expected = others, actual = configScreenStore.state.sections.filterNot { it.configType == ConfigType.CLIENT_FLAG })
    }

    /** Verifies that the client configs section reflects a form value update. */
    @Test
    fun clientConfigs() = runUITest {
        val configScreenStore = setUpScreen()

        val item = configScreenStore.item(configType = ConfigType.CLIENT_CONFIG, name = "httpConnectTimeoutMillis")
        assertEquals(expected = ConfigValue.Form(value = FakeData.clientConfigs.httpConnectTimeoutMillis.toString()), actual = item.value)

        configScreenStore.send(action = ConfigScreenAction.UpdateValue(configType = ConfigType.CLIENT_CONFIG, item = item, value = ConfigValue.Form(value = "123")))
        waitForIdle()

        assertEquals(expected = ConfigValue.Form(value = "123"), actual = configScreenStore.item(configType = ConfigType.CLIENT_CONFIG, name = "httpConnectTimeoutMillis").value)
    }

    /** Verifies that the server flags section reflects a toggled value update. */
    @Test
    fun serverFlags() = runUITest {
        val configScreenStore = setUpScreen()

        val item = configScreenStore.item(configType = ConfigType.SERVER_FLAG, name = "probe")
        assertEquals(expected = ConfigValue.Toggle(value = FakeData.serverFlags.probe), actual = item.value)

        configScreenStore.send(action = ConfigScreenAction.UpdateValue(configType = ConfigType.SERVER_FLAG, item = item, value = ConfigValue.Toggle(value = false)))
        waitForIdle()

        assertEquals(expected = ConfigValue.Toggle(value = false), actual = configScreenStore.item(configType = ConfigType.SERVER_FLAG, name = "probe").value)
    }

    /** Verifies that the server configs section reflects a form value update. */
    @Test
    fun serverConfigs() = runUITest {
        val configScreenStore = setUpScreen()

        val item = configScreenStore.item(configType = ConfigType.SERVER_CONFIG, name = "accessTokenValidity")
        assertEquals(expected = ConfigValue.Form(value = FakeData.serverConfigs.accessTokenValidity.toString()), actual = item.value)

        configScreenStore.send(action = ConfigScreenAction.UpdateValue(configType = ConfigType.SERVER_CONFIG, item = item, value = ConfigValue.Form(value = "123")))
        waitForIdle()

        assertEquals(expected = ConfigValue.Form(value = "123"), actual = configScreenStore.item(configType = ConfigType.SERVER_CONFIG, name = "accessTokenValidity").value)
    }

    /** Verifies that a non numeric form value is displayed but neither marked pending nor persisted. */
    @Test
    fun nonNumericFormValue() = runUITest {
        val configScreenStore = setUpScreen()

        val item = configScreenStore.item(configType = ConfigType.CLIENT_CONFIG, name = "httpConnectTimeoutMillis")

        configScreenStore.send(action = ConfigScreenAction.UpdateValue(configType = ConfigType.CLIENT_CONFIG, item = item, value = ConfigValue.Form(value = "12a")))
        waitForIdle()

        val updated = configScreenStore.item(configType = ConfigType.CLIENT_CONFIG, name = "httpConnectTimeoutMillis")
        assertEquals(expected = ConfigValue.Form(value = "12a"), actual = updated.value)
        assertFalse(actual = updated.pending)
        assertEquals(expected = FakeData.clientConfigs, actual = ClientConfigs.configs)
    }

    /** Verifies that a user only gets the sections of the config types they hold a permission on. */
    @Test
    fun configSectionsByPermission() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.user)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.credentials)

        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.CONFIG), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val configScreenStore = ConfigScreenStore(state = ConfigScreenState(), configUseCases = authenticatedUseCases.configUseCases, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val actionBarStore = ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI { ConfigsScreen(actionBarStore = actionBarStore, navigationStore = navigationStore, store = configScreenStore) }

        waitUntil { configScreenStore.state.sections.isNotEmpty() }

        assertEquals(expected = listOf(ConfigType.CLIENT_FLAG, ConfigType.CLIENT_CONFIG), actual = configScreenStore.state.sections.map { it.configType })
    }
}
