package com.app.builder.ui.modifier

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import com.app.builder.test.TestCase

class PointerListenerTest: TestCase() {

    /** Verifies that a matching mouse click triggers the callback. */
    @Test
    fun matchingMouseClickTriggersCallback() = runUITest {
        var clicked = false

        setUI {
            Box(
                modifier = Modifier
                    .size(size = 24.dp)
                    .testTag(tag = TAG)
                    .onMouseClick(mouseClicks = listOf(element = MouseClick.PRIMARY)) { clicked = true }
            )
        }

        onNodeWithTag(testTag = TAG).performMouseInput {
            press(button = MouseButton.Primary)
            release(button = MouseButton.Primary)
        }
        waitForIdle()

        assertTrue(actual = clicked)
    }

    /** Verifies that a non-matching mouse click does not trigger the callback. */
    @Test
    fun nonMatchingMouseClickDoesNotTriggerCallback() = runUITest {
        var clicked = false

        setUI {
            Box(
                modifier = Modifier
                    .size(size = 24.dp)
                    .testTag(tag = TAG)
                    .onMouseClick(mouseClicks = listOf(element = MouseClick.PRIMARY)) { clicked = true }
            )
        }

        onNodeWithTag(testTag = TAG).performMouseInput {
            press(button = MouseButton.Secondary)
            release(button = MouseButton.Secondary)
        }
        waitForIdle()

        assertFalse(actual = clicked)
    }

    /** Verifies that any button listed in [mouseClicks] can trigger the callback. */
    @Test
    fun anyListedButtonTriggersCallback() = runUITest {
        var clicked = false

        setUI {
            Box(
                modifier = Modifier
                    .size(size = 24.dp)
                    .testTag(tag = TAG)
                    .onMouseClick(mouseClicks = listOf(MouseClick.PRIMARY, MouseClick.MIDDLE)) { clicked = true }
            )
        }

        onNodeWithTag(testTag = TAG).performMouseInput {
            press(button = MouseButton.Tertiary)
            release(button = MouseButton.Tertiary)
        }
        waitForIdle()

        assertTrue(actual = clicked)
    }

    private companion object {
        /** The test tag used to locate the pointer target node. */
        private const val TAG = "pointer-target"
    }
}
