package com.app.builder.ui.modifier

import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Intercepts hardware key events and triggers a callback when any of the specified [keys] are pressed.
 *
 * @param keys A list of [Key]s that should trigger the [onKey] callback.
 * @param onKey The action to perform when a matching [Key] is pressed (KeyDown).
 * @return The modified [Modifier].
 */
fun Modifier.onKeyPress(
    keys: List<Key>,
    onKey: () -> Unit = {},
): Modifier = onPreviewKeyEvent { event ->
    if (event.type == KeyEventType.KeyDown && keys.any { it == event.key }) {
        onKey()
        true
    } else false
}

/**
 * Listens for a specific [sequence] of [Key] stokes and triggers the [onSequenceComplete] callback upon completion.
 * The sequence progress will automatically reset if the user pauses for longer than the specified [delay].
 */
@Composable
fun rememberKeySequence(
    sequence: List<Key>,
    delay: Long = 2000L,
    onSequenceComplete: () -> Unit
): (KeyEvent) -> Boolean {
    var progress by remember { mutableStateOf(value = 0) }
    LaunchedEffect(key1 = progress) {
        if (progress > 0) {
            delay(timeMillis = delay)
            progress = 0
        }
    }
    return remember(key1 = sequence, key2 = onSequenceComplete) {
        { keyEvent ->
            progress = keyEvent.onSequence(
                sequence = sequence,
                progress = progress,
                onSequenceComplete = onSequenceComplete
            )
            false
        }
    }
}

/**
 * Processes a [KeyEvent] to check its [progress] against a given key [sequence]
 * and return the new progress value after processing the event.
 */
private fun KeyEvent.onSequence(
    sequence: List<Key>,
    progress: Int,
    onSequenceComplete: () -> Unit
): Int {
    if (type != KeyEventType.KeyDown) return progress
    val expectedKey = sequence.getOrNull(index = progress)
    val newProgress = when (key) {
        expectedKey -> progress + 1
        else -> if (key == sequence.firstOrNull()) 1 else 0
    }
    if (newProgress >= sequence.size) {
        onSequenceComplete()
        return 0
    }
    return newProgress
}
