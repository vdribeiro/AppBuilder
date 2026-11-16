package com.app.builder.ui.navigation.help

import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.app.builder.test.TestCase
import com.app.builder.ui.navigation.Screen

class HelpScreenTest: TestCase() {

    /** Verifies that the help screen explains every status icon. */
    @Test
    fun explainsEveryStatusIcon() = runUITest {
        setUI { HelpScreen() }

        onNodeWithText(text = "help_status_icons").assertIsDisplayed()
        listOf("help_session_expired", "help_not_synced", "help_clock_untrusted").forEach { tag ->
            onNodeWithTag(testTag = tag).assertIsDisplayed()
            onNodeWithText(text = "${tag}_description").assertIsDisplayed()
        }
    }

    /** Verifies that the back button pops the help screen off the back stack. */
    @Test
    fun backReturnsToThePreviousScreen() = runUITest {
        router.navigate(screen = Screen.UserProfile)
        router.navigate(screen = Screen.Help)

        setUI { HelpScreen() }

        onNodeWithTag(testTag = "action_back").assertIsDisplayed().performClick()
        waitForIdle()

        assertEquals(expected = listOf(Screen.UserProfile), actual = router.backStack.toList())
    }
}
