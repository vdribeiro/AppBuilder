package com.app.builder.ui.navigation.scene

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.app.builder.core.config.ClientConfigs
import com.app.builder.ui.LocalSplitScreen
import com.app.builder.ui.Preview
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.screen.LocalScaffold
import com.app.builder.ui.screen.ScaffoldState
import com.app.builder.ui.screen.Skeleton

/** A [SceneStrategy] that returns a [SplitScene]. */
class SplitSceneStrategy<T: Any>: SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val startEntry = entries.getOrNull(index = entries.lastIndex - 1) ?: return null
        val endEntry = entries.getOrNull(index = entries.lastIndex) ?: return null
        startEntry.metadata[SplitKey] ?: return null
        endEntry.metadata[SplitKey] ?: return null

        return SplitScene(
            key = startEntry.contentKey,
            previousEntries = entries.dropLast(n = 1),
            startEntry = startEntry,
            endEntry = endEntry,
        )
    }

    /** A [Scene] that displays 2 panes side-by-side. */
    private class SplitScene<T: Any>(
        override val key: Any,
        override val previousEntries: List<NavEntry<T>>,
        private val startEntry: NavEntry<T>,
        private val endEntry: NavEntry<T>,
    ): Scene<T> {
        override val entries: List<NavEntry<T>> = listOf(startEntry, endEntry)
        override val content: @Composable (() -> Unit) = {
            SplitContent(
                startContent = { startEntry.Content() },
                endContent = { endEntry.Content() },
            )
        }
    }

    companion object {
        /**
         * Mark this entry as something that should be displayed in a split pane.
         *
         * @return The metadata map to attach to the entry.
         */
        fun split(): Map<String, Any> = metadata { put(key = SplitKey, value = true) }
    }

    /** Metadata key used to flag a [NavEntry] as the split pane. */
    private object SplitKey: NavMetadataKey<Boolean>
}

/**
 * Renders a responsive layout that toggles between a split-screen view and a single view based on the [LocalSplitScreen] state.
 * In split-screen mode, both entries are displayed side-by-side in a [Row], sharing the available space according to the defined ratio in [ClientConfigs.splitPaneRatio].
 * In single-screen mode, only the [endContent] is displayed.
 *
 * @param startContent The composable content for the start side pane.
 * @param endContent The composable content for the end pane.
 */
@Composable
private fun SplitContent(
    startContent: @Composable () -> Unit,
    endContent: @Composable () -> Unit,
) {
    if (!LocalSplitScreen.current) {
        endContent()
        return
    }

    var scaffoldState by remember { mutableStateOf(value = ScaffoldState()) }
    val safeRatio = (ClientConfigs.configs.splitPaneRatio / 100f).coerceIn(minimumValue = 0.01f, maximumValue = 0.99f)
    Skeleton(
        onBackClick = scaffoldState.onBackClick,
        topBar = scaffoldState.topBar,
        bottomBar = scaffoldState.bottomBar,
        snackbarHost = scaffoldState.snackbarHost,
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(weight = 1f - safeRatio)) {
                CompositionLocalProvider(value = LocalScaffold provides {}) {
                    startContent()
                }
            }
            Box(modifier = Modifier.weight(weight = safeRatio)) {
                CompositionLocalProvider(value = LocalScaffold provides { scaffoldState = it }) {
                    endContent()
                }
            }
        }
    }
}

@Preview
@Composable
private fun SplitContentSplitPreview() = Preview {
    CompositionLocalProvider(value = LocalSplitScreen provides true) {
        SplitContent(
            startContent = {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Pane 1")
                }
            },
            endContent = {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Pane 2")
                }
            }
        )
    }
}

@Preview
@Composable
private fun SplitContentPreview() = Preview {
    CompositionLocalProvider(value = LocalSplitScreen provides false) {
        SplitContent(
            startContent = {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Pane 1")
                }
            },
            endContent = {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Pane 2")
                }
            }
        )
    }
}
