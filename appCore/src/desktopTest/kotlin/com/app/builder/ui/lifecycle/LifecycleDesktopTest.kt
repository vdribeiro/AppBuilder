package com.app.builder.ui.lifecycle

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import androidx.compose.ui.window.WindowState
import com.app.builder.test.TestCase

/** Desktop-specific coverage for [Register] that requires the [WindowState]/[LocalWindowState] composition local, which only exists on this target. */
class LifecycleDesktopTest: TestCase() {

    /** Verifies that minimizing the window triggers the background callback, and restoring it triggers the foreground callback. */
    @Test
    fun minimizingAndRestoringTheWindowDrivesBackgroundAndForegroundCallbacks() = runUITest {
        val windowState = WindowState()
        var backgroundCalled = false
        var foregroundCalled = false

        setUI(compositionValues = listOf(LocalWindowState provides windowState)) {
            Register(
                onBackground = { backgroundCalled = true },
                onForeground = { foregroundCalled = true },
            )
        }
        assertFalse(actual = backgroundCalled)
        assertTrue(actual = foregroundCalled)
        foregroundCalled = false

        windowState.isMinimized = true
        waitForIdle()
        assertTrue(actual = backgroundCalled)

        backgroundCalled = false
        windowState.isMinimized = false
        waitForIdle()
        assertTrue(actual = foregroundCalled)
    }
}
