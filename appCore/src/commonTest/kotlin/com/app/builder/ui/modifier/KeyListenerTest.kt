package com.app.builder.ui.modifier

import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import com.app.builder.test.TestCase

class KeyListenerTest: TestCase() {

    /**
     * Presses each of [keys] in order on the focused key target.
     *
     * @param keys The keys to press.
     */
    private fun ComposeUiTest.press(keys: List<Key>) {
        onNodeWithTag(testTag = TAG).requestFocus()
        onNodeWithTag(testTag = TAG).performKeyInput { keys.forEach { pressKey(key = it) } }
        waitForIdle()
    }

    /** Verifies that [onKeyPress] fires once per key down of a listed key and ignores every other key. */
    @Test
    fun onKeyPressOnlyFiresForListedKeys() = runUITest {
        var presses = 0

        setUI {
            Box(
                modifier = Modifier
                    .size(size = 24.dp)
                    .testTag(tag = TAG)
                    .onKeyPress(keys = listOf(Key.Enter, Key.Spacebar)) { presses++ }
                    .focusable()
            )
        }

        press(keys = listOf(Key.Enter, Key.A, Key.Spacebar))

        assertEquals(expected = 2, actual = presses)
    }

    /** Verifies that [rememberKeySequence] completes once the whole sequence is pressed in order, and restarts after a wrong key. */
    @Test
    fun keySequenceCompletesOnlyWhenPressedInOrder() = runUITest {
        var completions = 0

        setUI {
            val onKeyEvent = rememberKeySequence(sequence = listOf(Key.A, Key.B, Key.C)) { completions++ }
            Box(
                modifier = Modifier
                    .size(size = 24.dp)
                    .testTag(tag = TAG)
                    .onPreviewKeyEvent(onPreviewKeyEvent = onKeyEvent)
                    .focusable()
            )
        }

        press(keys = listOf(Key.A, Key.B, Key.D, Key.C))
        assertEquals(expected = 0, actual = completions)

        press(keys = listOf(Key.A, Key.A, Key.B, Key.C))
        assertEquals(expected = 1, actual = completions)
    }

    /** Verifies that [rememberKeySequence] resets its progress once the user pauses longer than the delay. */
    @Test
    fun keySequenceResetsAfterThePauseDelay() = runUITest {
        var completions = 0

        setUI {
            val onKeyEvent = rememberKeySequence(sequence = listOf(Key.A, Key.B), delay = DELAY_MILLIS) { completions++ }
            Box(
                modifier = Modifier
                    .size(size = 24.dp)
                    .testTag(tag = TAG)
                    .onPreviewKeyEvent(onPreviewKeyEvent = onKeyEvent)
                    .focusable()
            )
        }

        press(keys = listOf(Key.A))
        mainClock.advanceTimeBy(milliseconds = DELAY_MILLIS * 2)
        press(keys = listOf(Key.B))

        assertEquals(expected = 0, actual = completions)
    }

    private companion object {
        /** The test tag used to locate the key target node. */
        private const val TAG = "key-target"
        /** The sequence pause delay fixture, in milliseconds. */
        private const val DELAY_MILLIS = 500L
    }
}
