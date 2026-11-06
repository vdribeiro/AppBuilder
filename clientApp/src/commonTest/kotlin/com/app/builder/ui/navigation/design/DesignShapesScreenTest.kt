package com.app.builder.ui.navigation.design

import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.app.builder.data.theme.CustomTheme
import com.app.builder.data.theme.ThemeCache
import com.app.builder.ui.ShapeRole

class DesignShapesScreenTest: DesignScreenTestCase() {

    /** Verifies that the shapes section displays the corner shape scale. */
    @Test
    fun designShapesSection() = runUITest {
        val store = RecordingStore()
        setShowcase { DesignShapesScreen(navigationStore = navigationStore, store = store) }
        waitForIdle()

        onNodeWithText(text = "Extra small (4dp)").assertIsDisplayed()
    }

    /** Verifies that applying a corner radius from the shape picker updates the theme and reports it. */
    @Test
    fun designShapesApplyRadius() = runUITest {
        val store = RecordingStore()
        setShowcase { DesignShapesScreen(navigationStore = navigationStore, store = store) }
        waitForIdle()

        onNodeWithTag(testTag = "shape_swatch_${ShapeRole.MEDIUM.name}").performClick()
        mainClock.advanceTimeByFrame()
        onNodeWithTag(testTag = "shape_picker_dialog").assertIsDisplayed()

        onNodeWithTag(testTag = "shape_picker_radius").performSemanticsAction(key = SemanticsActions.SetProgress) { it(20f) }
        mainClock.advanceTimeByFrame()
        onNodeWithText(text = "20dp").assertIsDisplayed()
        onNodeWithText(text = "apply").performClick()
        mainClock.advanceTimeByFrame()

        val expected = CustomTheme(shapes = mapOf(ShapeRole.MEDIUM.name to 20f))
        assertEquals(expected = expected, actual = store.savedTheme)
        assertEquals(expected = expected, actual = ThemeCache.value)
        onNodeWithText(text = "Medium (20dp)").assertIsDisplayed()
    }

    /** Verifies that resetting a corner radius from the shape picker restores its default. */
    @Test
    fun designShapesResetRadius() = runUITest {
        ThemeCache.setShape(role = ShapeRole.MEDIUM.name, radius = 0f)
        val store = RecordingStore()
        setShowcase { DesignShapesScreen(navigationStore = navigationStore, store = store) }
        waitForIdle()

        onNodeWithTag(testTag = "shape_swatch_${ShapeRole.MEDIUM.name}").performClick()
        mainClock.advanceTimeByFrame()
        onNodeWithText(text = "reset").performClick()
        mainClock.advanceTimeByFrame()

        assertEquals(expected = CustomTheme(), actual = store.savedTheme)
        onNodeWithText(text = "Medium (12dp)").assertIsDisplayed()
    }
}
