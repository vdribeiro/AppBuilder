package com.app.builder.ui.navigation.scene

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.SceneStrategyScope
import com.app.builder.test.TestCase
import com.app.builder.ui.LocalSplitScreen
import com.app.builder.ui.core.text.Text

class SplitSceneTest: TestCase() {

    /** Verifies the default split metadata sizes. */
    @Test
    fun splitMetadata() = runUnitTest {
        val startPane = SplitSceneStrategy.split()
        val endPane = SplitSceneStrategy.split()
        assertEquals(expected = 1, actual = startPane.size)
        assertEquals(expected = 1, actual = endPane.size)
    }

    /** Verifies that calculating the scene with no entries returns null. */
    @Test
    fun splitCalculateSceneEmpty() = runUnitTest {
        val strategy = SplitSceneStrategy<String>()
        val scope = SceneStrategyScope<String>()
        val scene = with(receiver = strategy) { scope.calculateScene(entries = emptyList()) }
        assertNull(actual = scene)
    }

    /** Verifies that calculating the scene with no end pane split-key metadata returns null. */
    @Test
    fun splitCalculateSceneNoEndPaneKey() = runUnitTest {
        val strategy = SplitSceneStrategy<String>()
        val scope = SceneStrategyScope<String>()
        val startEntry = NavEntry(key = "startPane", metadata = SplitSceneStrategy.split()) {}
        val endEntry = NavEntry(key = "endPane", metadata = emptyMap()) {}
        val scene = with(receiver = strategy) { scope.calculateScene(entries = listOf(startEntry, endEntry)) }
        assertNull(actual = scene)
    }

    /** Verifies that calculating the scene with no start pane split-key metadata returns null. */
    @Test
    fun splitCalculateSceneNoStartPaneKey() = runUnitTest {
        val strategy = SplitSceneStrategy<String>()
        val scope = SceneStrategyScope<String>()
        val startEntry = NavEntry(key = "startPane", metadata = emptyMap()) {}
        val endEntry = NavEntry(key = "endPane", metadata = SplitSceneStrategy.split()) {}
        val scene = with(receiver = strategy) { scope.calculateScene(entries = listOf(startEntry, endEntry)) }
        assertNull(actual = scene)
    }

    /** Verifies that calculating the scene with start pane and end pane entries displays both side by side. */
    @Test
    fun splitCalculateScene() = runUITest {
        val strategy = SplitSceneStrategy<String>()
        val scope = SceneStrategyScope<String>()
        val startPaneEntry = NavEntry(key = "startPane", metadata = SplitSceneStrategy.split()) { Text(text = "Start Pane content") }
        val endPaneEntry = NavEntry(key = "endPane", metadata = SplitSceneStrategy.split()) { Text(text = "End Pane content") }
        val scene = with(receiver = strategy) { scope.calculateScene(entries = listOf(startPaneEntry, endPaneEntry)) }
        assertNotNull(actual = scene)
        assertEquals(expected = listOf(startPaneEntry, endPaneEntry), actual = scene.entries)
        assertEquals(expected = listOf(element = startPaneEntry), actual = scene.previousEntries)

        setUI {
            CompositionLocalProvider(value = LocalSplitScreen provides true) {
                scene.content()
            }
        }

        onNodeWithText("Start Pane content").assertIsDisplayed()
        onNodeWithText("End Pane content").assertIsDisplayed()
    }

    /** Verifies that only the end pane content is displayed when the split screen is disabled. */
    @Test
    fun splitContentSingleScreen() = runUITest {
        val strategy = SplitSceneStrategy<String>()
        val scope = SceneStrategyScope<String>()
        val startPaneEntry = NavEntry(key = "startPane", metadata = SplitSceneStrategy.split()) { Text(text = "Start Pane content") }
        val endPaneEntry = NavEntry(key = "endPane", metadata = SplitSceneStrategy.split()) { Text(text = "End Pane content") }
        val scene = with(receiver = strategy) { scope.calculateScene(entries = listOf(startPaneEntry, endPaneEntry)) }
        assertNotNull(actual = scene)
        assertEquals(expected = listOf(startPaneEntry, endPaneEntry), actual = scene.entries)
        assertEquals(expected = listOf(element = startPaneEntry), actual = scene.previousEntries)

        setUI {
            CompositionLocalProvider(value = LocalSplitScreen provides false) {
                scene.content()
            }
        }

        onAllNodesWithText("Start Pane content").assertCountEquals(0)
        onNodeWithText("End Pane content").assertIsDisplayed()
    }
}
