package com.app.builder.ui

import kotlin.test.Test
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onNodeWithTag
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
}
