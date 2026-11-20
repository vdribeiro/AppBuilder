package com.app.builder.ui.navigation.configs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithTag
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.list.ConfigItem
import com.app.builder.ui.component.list.ConfigValue
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.component.session.SessionStore

class ConfigsScreenTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(http = true) }
    }

    /**
     * Logs an admin in and renders the config screen with every store it needs, waiting until every section has loaded.
     *
     * @return The [ConfigScreenStore] driving the rendered screen.
     */
    private suspend fun ComposeUiTest.setUpScreen(): ConfigScreenStore {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val configScreenStore = ConfigScreenStore(state = ConfigScreenState(), configUseCases = authenticatedUseCases.configUseCases, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI { ConfigsScreen(sessionStore = sessionStore, store = configScreenStore) }

        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        onNode(matcher = isSelected() and hasText(text = "navigation_config")).assertIsDisplayed()
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
        assertEquals(expected = ConfigValue.Toggle(value = ClientFlags.value.http), actual = item.value)

        val others = configScreenStore.state.sections.filterNot { it.configType == ConfigType.CLIENT_FLAG }

        configScreenStore.send(action = ConfigScreenAction.UpdateValue(configType = ConfigType.CLIENT_FLAG, item = item, value = ConfigValue.Toggle(value = false)))
        waitForIdle()

        assertEquals(expected = ConfigValue.Toggle(value = false), actual = configScreenStore.item(configType = ConfigType.CLIENT_FLAG, name = "http").value)
        assertEquals(expected = others, actual = configScreenStore.state.sections.filterNot { it.configType == ConfigType.CLIENT_FLAG })
    }

    /** Verifies that the client flags section follows a flag changed outside the screen, keeping the entry's identity. */
    @Test
    fun clientFlagsFollowOutsideChanges() = runUITest {
        val configScreenStore = setUpScreen()
        val item = configScreenStore.item(configType = ConfigType.CLIENT_FLAG, name = "http")

        val http = ClientFlags.value.http

        ClientFlags.set { it.copy(http = !http) }

        waitUntil { configScreenStore.item(configType = ConfigType.CLIENT_FLAG, name = "http").value == ConfigValue.Toggle(value = !http) }
        assertEquals(expected = item.uuid, actual = configScreenStore.item(configType = ConfigType.CLIENT_FLAG, name = "http").uuid)
    }

    /** Verifies that the client configs section reflects a form value update. */
    @Test
    fun clientConfigs() = runUITest {
        val configScreenStore = setUpScreen()

        val item = configScreenStore.item(configType = ConfigType.CLIENT_CONFIG, name = "httpConnectTimeoutMillis")
        assertEquals(expected = ConfigValue.Form(value = ClientConfigs.value.httpConnectTimeoutMillis.toString()), actual = item.value)

        configScreenStore.send(action = ConfigScreenAction.UpdateValue(configType = ConfigType.CLIENT_CONFIG, item = item, value = ConfigValue.Form(value = "123")))
        waitForIdle()

        assertEquals(expected = ConfigValue.Form(value = "123"), actual = configScreenStore.item(configType = ConfigType.CLIENT_CONFIG, name = "httpConnectTimeoutMillis").value)
    }

    /** Verifies that the server flags section reflects a toggled value update. */
    @Test
    fun serverFlags() = runUITest {
        val configScreenStore = setUpScreen()

        val item = configScreenStore.item(configType = ConfigType.SERVER_FLAG, name = "probe")
        assertEquals(expected = ConfigValue.Toggle(value = ServerFlags.value.probe), actual = item.value)

        configScreenStore.send(action = ConfigScreenAction.UpdateValue(configType = ConfigType.SERVER_FLAG, item = item, value = ConfigValue.Toggle(value = true)))
        waitForIdle()

        assertEquals(expected = ConfigValue.Toggle(value = true), actual = configScreenStore.item(configType = ConfigType.SERVER_FLAG, name = "probe").value)
    }

    /** Verifies that the server configs section reflects a form value update. */
    @Test
    fun serverConfigs() = runUITest {
        val configScreenStore = setUpScreen()

        val item = configScreenStore.item(configType = ConfigType.SERVER_CONFIG, name = "accessTokenValidity")
        assertEquals(expected = ConfigValue.Form(value = ServerConfigs.value.accessTokenValidity.toString()), actual = item.value)

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
        assertEquals(expected = ClientConfigs.initialValue, actual = ClientConfigs.value)
    }

    /** Verifies that a user only gets the sections they can read, and that read-only sections are disabled and ignore edits. */
    @Test
    fun configSectionsByPermission() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.account)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.credentials)

        val configScreenStore = ConfigScreenStore(state = ConfigScreenState(), configUseCases = authenticatedUseCases.configUseCases, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI { ConfigsScreen(sessionStore = sessionStore, store = configScreenStore) }

        waitUntil { configScreenStore.state.sections.isNotEmpty() }

        assertEquals(expected = listOf(ConfigType.CLIENT_FLAG, ConfigType.CLIENT_CONFIG), actual = configScreenStore.state.sections.map { it.configType })
        assertTrue(actual = configScreenStore.state.sections.none { it.editable })
        onAllNodes(matcher = isToggleable()).assertAll(matcher = isNotEnabled())

        val item = configScreenStore.item(configType = ConfigType.CLIENT_FLAG, name = "http")
        configScreenStore.send(action = ConfigScreenAction.UpdateValue(configType = ConfigType.CLIENT_FLAG, item = item, value = ConfigValue.Toggle(value = !ClientFlags.value.http)))
        waitForIdle()

        assertEquals(expected = item, actual = configScreenStore.item(configType = ConfigType.CLIENT_FLAG, name = "http"))
    }

    /** Verifies that an admin with write permission gets every section editable. */
    @Test
    fun configSectionsEditable() = runUITest {
        val configScreenStore = setUpScreen()

        assertTrue(actual = configScreenStore.state.sections.all { it.editable })
    }
}
