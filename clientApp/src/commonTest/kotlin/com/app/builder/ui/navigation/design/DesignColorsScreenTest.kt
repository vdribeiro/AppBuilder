package com.app.builder.ui.navigation.design

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.app.builder.data.theme.CustomTheme
import com.app.builder.data.theme.ThemeCache
import com.app.builder.data.theme.ThemeMode
import com.app.builder.ui.ColorRole

class DesignColorsScreenTest: DesignScreenTestCase() {

    /** Verifies that the colors section displays the color scheme's swatches. */
    @Test
    fun designColorsSection() = runUITest {
        val store = RecordingStore()
        setShowcase { DesignColorsScreen(sessionStore = sessionStore, store = store) }
        waitForIdle()

        onNodeWithText(text = "Primary").assertIsDisplayed()
    }

    /** Verifies that selecting a theme mode updates the theme and reports it. */
    @Test
    fun designColorsThemeMode() = runUITest {
        val store = RecordingStore()
        setShowcase { DesignColorsScreen(sessionStore = sessionStore, store = store) }
        waitForIdle()

        onNodeWithText(text = "Dark").performClick()
        mainClock.advanceTimeByFrame()

        val expected = CustomTheme(mode = ThemeMode.DARK)
        assertEquals(expected = expected, actual = store.savedTheme)
        assertEquals(expected = expected, actual = ThemeCache.value)
    }

    /** Verifies that applying a color in dark mode replaces the dark color scheme's color. */
    @Test
    fun designColorsApplyDarkColor() = runUITest {
        ThemeCache.setMode(mode = ThemeMode.DARK)
        val store = RecordingStore()
        setShowcase { DesignColorsScreen(sessionStore = sessionStore, store = store) }
        waitForIdle()

        onNodeWithTag(testTag = "color_swatch_${ColorRole.PRIMARY.name}").performClick()
        mainClock.advanceTimeByFrame()
        onNodeWithTag(testTag = "color_picker_hex").performTextReplacement(text = "#123456")
        mainClock.advanceTimeByFrame()
        onNodeWithText(text = "apply").performClick()
        mainClock.advanceTimeByFrame()

        assertEquals(expected = CustomTheme(mode = ThemeMode.DARK, dark = mapOf(ColorRole.PRIMARY.name to "#123456")), actual = store.savedTheme)
    }

    /** Verifies that applying a typed color from the color picker updates the theme and reports it. */
    @Test
    fun designColorsApplyColor() = runUITest {
        ThemeCache.setMode(mode = ThemeMode.LIGHT)
        val store = RecordingStore()
        setShowcase { DesignColorsScreen(sessionStore = sessionStore, store = store) }
        waitForIdle()

        onNodeWithTag(testTag = "color_swatch_${ColorRole.PRIMARY.name}").performClick()
        mainClock.advanceTimeByFrame()
        onNodeWithTag(testTag = "color_picker_dialog").assertIsDisplayed()

        onNodeWithTag(testTag = "color_picker_hex").performTextReplacement(text = "#123456")
        mainClock.advanceTimeByFrame()
        onNodeWithText(text = "apply").performClick()
        mainClock.advanceTimeByFrame()

        val expected = CustomTheme(mode = ThemeMode.LIGHT, light = mapOf(ColorRole.PRIMARY.name to "#123456"))
        assertEquals(expected = expected, actual = store.savedTheme)
        assertEquals(expected = expected, actual = ThemeCache.value)
        onNodeWithText(text = "#123456").assertIsDisplayed()
    }

    /** Verifies that a malformed hexadecimal color cannot be applied. */
    @Test
    fun designColorsInvalidHex() = runUITest {
        val store = RecordingStore()
        setShowcase { DesignColorsScreen(sessionStore = sessionStore, store = store) }
        waitForIdle()

        onNodeWithTag(testTag = "color_swatch_${ColorRole.PRIMARY.name}").performClick()
        mainClock.advanceTimeByFrame()
        onNodeWithTag(testTag = "color_picker_hex").performTextReplacement(text = "#12")
        mainClock.advanceTimeByFrame()

        onNodeWithText(text = "apply").assertIsNotEnabled()
    }

    /** Verifies that resetting a color from the color picker restores its default. */
    @Test
    fun designColorsResetColor() = runUITest {
        ThemeCache.setMode(mode = ThemeMode.LIGHT)
        ThemeCache.setColor(dark = false, role = ColorRole.PRIMARY.name, hex = "#FF0000")
        val store = RecordingStore()
        setShowcase { DesignColorsScreen(sessionStore = sessionStore, store = store) }
        waitForIdle()

        onNodeWithTag(testTag = "color_swatch_${ColorRole.PRIMARY.name}").performClick()
        mainClock.advanceTimeByFrame()
        onNodeWithText(text = "reset").performClick()
        mainClock.advanceTimeByFrame()

        assertEquals(expected = CustomTheme(mode = ThemeMode.LIGHT), actual = store.savedTheme)
        assertNull(actual = ThemeCache.value.light[ColorRole.PRIMARY.name])
    }
}
