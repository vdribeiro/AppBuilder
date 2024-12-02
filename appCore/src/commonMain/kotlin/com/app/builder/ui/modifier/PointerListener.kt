package com.app.builder.ui.modifier

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.isForwardPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * A [Modifier] that listens for a specific [mouseClicks] and triggers the [onClick] callback.
 * It intercepts low-level pointer interaction packets, match-checks active physical mouse button masks, consumes the associated layout updates, and safely dispatches an execution signal to the [onClick] callback function.
 *
 * @param mouseClicks A list containing target [MouseClick] trigger definitions allowed to invoke the callback.
 * @param onClick Callback invoked instantly when a matching mouse click interaction profile is detected.
 * @return An altered [Modifier] layer capable of capturing discrete hardware mouse events.
 */
fun Modifier.onMouseClick(
    mouseClicks: List<MouseClick>,
    onClick: () -> Unit
): Modifier = composed {
    val onClick by rememberUpdatedState(newValue = onClick)

    pointerInput(key1 = mouseClicks) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.changes.any { it.isConsumed }) continue
                if (event.type == PointerEventType.Press) {
                    val triggered = mouseClicks.any {
                        with(receiver = event.buttons) {
                            when (it) {
                                MouseClick.PRIMARY -> isPrimaryPressed
                                MouseClick.SECONDARY -> isSecondaryPressed
                                MouseClick.MIDDLE -> isTertiaryPressed
                                MouseClick.BACK -> isBackPressed
                                MouseClick.FORWARD -> isForwardPressed
                            }

                        }
                    }
                    if (triggered) {
                        event.changes.forEach { it.consume() }
                        onClick()
                    }
                }
            }
        }
    }
}

/** Identifies a physical mouse button that can be matched by [onMouseClick]. */
enum class MouseClick {
    /** The main mouse button, typically the left button. */
    PRIMARY,
    /** The contextual mouse button, typically the right button. */
    SECONDARY,
    /** The middle mouse button, typically the scroll wheel button. */
    MIDDLE,
    /** The side button used to navigate backwards. */
    BACK,
    /** The side button used to navigate forwards. */
    FORWARD
}
