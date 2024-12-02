package com.app.builder.ui.screen

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.app.builder.test.TestCase
import com.app.builder.ui.core.text.Text

class ScreenTest: TestCase() {

    /** Verifies that a default [ScaffoldState] provides a snackbar host. */
    @Test
    fun scaffoldState() = runUnitTest {
        val state = ScaffoldState()
        assertNotNull(actual = state.snackbarHost)
    }

    /** Verifies that [ScaffoldState] retains the provided top bar and bottom bar. */
    @Test
    fun scaffoldStateWithBars() = runUnitTest {
        val state = ScaffoldState(
            topBar = {},
            bottomBar = {},
        )
        assertNotNull(actual = state.topBar)
        assertNotNull(actual = state.bottomBar)
    }

    /** Verifies that [Screen] displays the top bar, bottom bar and content. */
    @Test
    fun screenWithTopBar() = runUITest {
        setUI {
            Screen(
                topBar = { Text(text = "Top Bar") },
                bottomBar = { Text(text = "Bottom Bar") },
            ) { Text(text = "Content") }
        }
        waitForIdle()

        onNodeWithText(text = "Top Bar").assertIsDisplayed()
        onNodeWithText(text = "Bottom Bar").assertIsDisplayed()
        onNodeWithText(text = "Content").assertIsDisplayed()
    }

    /** Verifies that [Screen] provides its scaffold state through [LocalScaffold]. */
    @Test
    fun screenWithLocalScaffold() = runUITest {
        var scaffoldState: ScaffoldState? = null

        setUI(
            compositionValues = listOf(
                LocalScaffold provides { state -> scaffoldState = state }
            )
        ) {
            Screen(topBar = {}) {}
        }

        waitForIdle()
        assertNotNull(actual = scaffoldState)
    }

    /** Verifies that [Screen] does not invoke the back click callback until it is triggered. */
    @Test
    fun screenWithBackClick() = runUITest {
        var backClicked = false
        setUI {
            Screen(onBackClick = { backClicked = true }) {}
        }
        waitForIdle()
        assertTrue(actual = !backClicked)
    }

    /** Verifies that [Screen] renders the provided snackbar host. */
    @Test
    fun screenRendersSnackbarHost() = runUITest {
        setUI {
            Screen(snackbarHost = { Text(text = "Snackbar") }) {}
        }
        waitForIdle()

        onNodeWithText(text = "Snackbar").assertIsDisplayed()
    }
}
