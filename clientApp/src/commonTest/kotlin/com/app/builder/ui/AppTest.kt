package com.app.builder.ui

import kotlin.test.Test
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.app.builder.Application
import com.app.builder.Dependency.getUserDependency
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class AppTest: TestCase() {

    /** Verifies that, before the application has initialized its dependencies, the app shows the splash screen's progress indicator. */
    @Test
    fun appShowsTheSplashScreenWhileLoading() = runUITest {
        setUI {
            App()
        }

        onNode(matcher = hasProgressBarRangeInfo(rangeInfo = ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
        onNodeWithTag(testTag = "action_bar").assertDoesNotExist()
    }

    /** Verifies that, once the application has started, the app leaves the splash screen for the signed out and then the signed in navigation. */
    @Test
    fun appFollowsTheApplicationState() = runUITest {
        Application.appGraph = dependency
        try {
            Application.start()
            Application.dependency.filterNotNull().first()
            setUI {
                App()
            }

            waitUntil { onAllNodesWithTag(testTag = "action_bar").fetchSemanticsNodes().isNotEmpty() }
            onNode(matcher = hasProgressBarRangeInfo(rangeInfo = ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()

            dependency.get()
                .getUserDependency(user = FakeData.adminUser)
                .useCases
                .authenticationUseCases
                .login(credentials = FakeData.adminCredentials)
            Application.userDependency.filterNotNull().first()

            waitUntil { onAllNodesWithTag(testTag = "action_bar").fetchSemanticsNodes().isNotEmpty() }
        } finally {
            Application.stop()
        }
    }
}
