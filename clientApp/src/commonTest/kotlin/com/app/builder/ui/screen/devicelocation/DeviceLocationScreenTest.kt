package com.app.builder.ui.screen.devicelocation

import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.devicelocation.DeviceLocationProvider
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.devicelocation.LocalDeviceLocationProvider

/** Counts the platform stop requests, without reaching any platform location API. */
private class FakeDeviceLocationProvider: DeviceLocationProvider() {
    /** Number of times the platform was asked to stop updates. */
    var stops = 0

    override fun platformStopUpdate() {
        stops++
    }
}

class DeviceLocationScreenTest: TestCase() {

    /** Verifies that the device location screen renders its status, controls and empty history, and that Stop stops the location updates. */
    @Test
    fun deviceLocationScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val actionBarStore = ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val navigationStore = NavigationStore(state = NavigationState(), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val deviceLocationProvider = FakeDeviceLocationProvider()

        setUI {
            CompositionLocalProvider(LocalDeviceLocationProvider provides deviceLocationProvider) {
                DeviceLocationScreen(actionBarStore = actionBarStore, navigationStore = navigationStore)
            }
        }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        onNodeWithText(text = "Availability").assertIsDisplayed()
        onNodeWithText(text = "State").assertIsDisplayed()
        onNodeWithText(text = "Controls").assertIsDisplayed()
        onNodeWithText(text = "Location History").assertIsDisplayed()
        onNodeWithText(text = "No locations captured yet").assertIsDisplayed()

        // The screen reports updates as active, so only Stop is offered.
        onNodeWithText(text = "Start").assertIsNotEnabled()
        onNodeWithText(text = "Stop").assertIsEnabled()

        onNodeWithText(text = "Stop").performClick()
        waitForIdle()

        assertEquals(expected = 1, actual = deviceLocationProvider.stops)
    }
}
