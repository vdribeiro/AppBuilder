package com.app.builder.ui.lifecycle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.app.builder.test.TestCase

class LifecycleTest: TestCase() {

    /** Verifies that the foreground callback fires on start while the background callback does not. */
    @Test
    fun lifecycle() = runUITest {
        var foregroundCalled = false
        var backgroundCalled = false

        setUI {
            Register(
                onBackground = { backgroundCalled = true },
                onForeground = { foregroundCalled = true },
            )
        }

        assertTrue(actual = foregroundCalled)
        assertFalse(actual = backgroundCalled)
    }

    /** Verifies that onDispose fires once the composable leaves composition. */
    @Test
    fun onDisposeFiresWhenLeavingComposition() = runUITest {
        var disposed = false
        var showRegister by mutableStateOf(value = true)

        setUI {
            if (showRegister) Register(onDispose = { disposed = true })
        }
        assertFalse(actual = disposed)

        showRegister = false
        waitForIdle()

        assertTrue(actual = disposed)
    }

    /** Verifies that changing the given keys disposes and restarts the lifecycle observer. */
    @Test
    fun changingKeysRestartsTheObserver() = runUITest {
        var key by mutableStateOf(value = 1)
        var foregroundCount = 0
        var disposeCount = 0

        setUI {
            Register(key, onForeground = { foregroundCount++ }, onDispose = { disposeCount++ })
        }
        assertEquals(expected = 1, actual = foregroundCount)
        assertEquals(expected = 0, actual = disposeCount)

        key = 2
        waitForIdle()

        assertEquals(expected = 1, actual = disposeCount)
        assertEquals(expected = 2, actual = foregroundCount)
    }
}
