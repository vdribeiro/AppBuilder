package com.app.builder.ui.screen.design

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import com.app.builder.data.theme.ThemeCache
import com.app.builder.data.theme.CustomTheme
import com.app.builder.data.theme.ThemeMode
import com.app.builder.test.TestCase
import com.app.builder.ui.ColorRole
import com.app.builder.ui.ShapeRole
import com.app.builder.ui.showcase.core.bar.TopBarShowcase
import com.app.builder.ui.showcase.core.card.CardShowcase
import com.app.builder.ui.showcase.core.divider.DividerShowcase
import com.app.builder.ui.showcase.core.image.ImageShowcase
import com.app.builder.ui.showcase.core.list.ListShowcase
import com.app.builder.ui.showcase.core.navigation.NavigationShowcase
import com.app.builder.ui.showcase.core.overlay.OverlayShowcase
import com.app.builder.ui.showcase.core.progress.ProgressShowcase
import com.app.builder.ui.showcase.core.text.TextShowcase
import com.app.builder.ui.store.Store

class DesignScreenTest: TestCase() {

    /** Store recording every action the screen sends, without acting on them. */
    private class RecordingStore: Store<Unit, DesignScreenAction>(initialState = Unit) {
        /** The actions received so far, in order. */
        val actions = mutableListOf<DesignScreenAction>()
        /** The theme of the last save received, or `null` when none was. */
        val savedTheme: CustomTheme? get() = actions.filterIsInstance<DesignScreenAction.SaveTheme>().lastOrNull()?.customTheme

        override fun reducer(state: Unit, action: DesignScreenAction) {
            actions.add(element = action)
        }
    }

    /**
     * Renders showcase content with the test clock held still.
     * The showcases render components that animate forever, like a loading button or an indeterminate progress indicator, which never let the composition go idle if the clock is left to advance on its own.
     *
     * @param content The Composable to render.
     */
    private fun ComposeUiTest.setShowcase(content: @Composable () -> Unit) {
        mainClock.autoAdvance = false
        setUI(content = content)
    }

    /** Verifies that every [DesignSection] is rendered in the design navigation bar. */
    @Test
    fun designNavigationItems() = runUITest {
        setShowcase { DesignScreen(section = DesignSection.COMPONENTS, store = RecordingStore()) }
        waitForIdle()

        onNodeWithTag(testTag = "design_navigation_bar").assertIsDisplayed()
        onNodeWithText(text = "App").assertIsDisplayed()
        DesignSection.entries.forEach { section ->
            onNodeWithText(text = section.text).assertIsDisplayed()
        }
    }

    /** Verifies that selecting another section in the design navigation bar reports it back. */
    @Test
    fun designNavigationSectionClick() = runUITest {
        var clicked: DesignSection? = null
        setShowcase { DesignScreen(section = DesignSection.COMPONENTS, onSectionClick = { clicked = it }, store = RecordingStore()) }
        waitForIdle()

        onNodeWithText(text = DesignSection.COLORS.text).performClick()
        waitForIdle()

        assertEquals(expected = DesignSection.COLORS, actual = clicked)
    }

    /** Verifies that the design navigation bar's app entry reports the user leaving the showcase. */
    @Test
    fun designNavigationAppClick() = runUITest {
        var clicked = false
        setShowcase { DesignScreen(section = DesignSection.COMPONENTS, onAppClick = { clicked = true }, store = RecordingStore()) }
        waitForIdle()

        onNodeWithText(text = "App").performClick()
        waitForIdle()

        assertTrue(actual = clicked)
    }

    /** Verifies that the colors section displays the color scheme's swatches. */
    @Test
    fun designColorsSection() = runUITest {
        setShowcase { DesignScreen(section = DesignSection.COLORS, store = RecordingStore()) }
        waitForIdle()

        onNodeWithText(text = "Primary").assertIsDisplayed()
    }

    /** Verifies that selecting a theme mode updates the theme and reports it. */
    @Test
    fun designColorsThemeMode() = runUITest {
        val store = RecordingStore()
        setShowcase { DesignScreen(section = DesignSection.COLORS, store = store) }
        waitForIdle()

        onNodeWithText(text = "Dark").performClick()
        mainClock.advanceTimeByFrame()

        val expected = CustomTheme(mode = ThemeMode.DARK)
        assertEquals(expected = expected, actual = store.savedTheme)
        assertEquals(expected = expected, actual = ThemeCache.cacheState.value)
    }

    /** Verifies that applying a color in dark mode replaces the dark color scheme's color. */
    @Test
    fun designColorsApplyDarkColor() = runUITest {
        ThemeCache.setMode(mode = ThemeMode.DARK)
        val store = RecordingStore()
        setShowcase { DesignScreen(section = DesignSection.COLORS, store = store) }
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
        setShowcase { DesignScreen(section = DesignSection.COLORS, store = store) }
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
        assertEquals(expected = expected, actual = ThemeCache.cacheState.value)
        onNodeWithText(text = "#123456").assertIsDisplayed()
    }

    /** Verifies that a malformed hexadecimal color cannot be applied. */
    @Test
    fun designColorsInvalidHex() = runUITest {
        setShowcase { DesignScreen(section = DesignSection.COLORS, store = RecordingStore()) }
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
        setShowcase { DesignScreen(section = DesignSection.COLORS, store = store) }
        waitForIdle()

        onNodeWithTag(testTag = "color_swatch_${ColorRole.PRIMARY.name}").performClick()
        mainClock.advanceTimeByFrame()
        onNodeWithText(text = "reset").performClick()
        mainClock.advanceTimeByFrame()

        assertEquals(expected = CustomTheme(mode = ThemeMode.LIGHT), actual = store.savedTheme)
        assertNull(actual = ThemeCache.cacheState.value.light[ColorRole.PRIMARY.name])
    }

    /** Verifies that the shapes section displays the corner shape scale. */
    @Test
    fun designShapesSection() = runUITest {
        setShowcase { DesignScreen(section = DesignSection.SHAPES, store = RecordingStore()) }
        waitForIdle()

        onNodeWithText(text = "Extra small (4dp)").assertIsDisplayed()
    }

    /** Verifies that applying a corner radius from the shape picker updates the theme and reports it. */
    @Test
    fun designShapesApplyRadius() = runUITest {
        val store = RecordingStore()
        setShowcase { DesignScreen(section = DesignSection.SHAPES, store = store) }
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
        assertEquals(expected = expected, actual = ThemeCache.cacheState.value)
        onNodeWithText(text = "Medium (20dp)").assertIsDisplayed()
    }

    /** Verifies that resetting a corner radius from the shape picker restores its default. */
    @Test
    fun designShapesResetRadius() = runUITest {
        ThemeCache.setShape(role = ShapeRole.MEDIUM.name, radius = 0f)
        val store = RecordingStore()
        setShowcase { DesignScreen(section = DesignSection.SHAPES, store = store) }
        waitForIdle()

        onNodeWithTag(testTag = "shape_swatch_${ShapeRole.MEDIUM.name}").performClick()
        mainClock.advanceTimeByFrame()
        onNodeWithText(text = "reset").performClick()
        mainClock.advanceTimeByFrame()

        assertEquals(expected = CustomTheme(), actual = store.savedTheme)
        onNodeWithText(text = "Medium (12dp)").assertIsDisplayed()
    }

    /** Verifies that the typography section displays the text style scale. */
    @Test
    fun designTypographySection() = runUITest {
        setShowcase { DesignScreen(section = DesignSection.TYPOGRAPHY, store = RecordingStore()) }
        waitForIdle()

        onNodeWithText(text = "Display large").assertIsDisplayed()
    }

    /** Verifies that the progress showcase renders its sections. */
    @Test
    fun progressShowcase() = runUITest {
        setShowcase { ProgressShowcase() }
        waitForIdle()

        onNodeWithText(text = "Circular").assertIsDisplayed()
    }

    /** Verifies that the text showcase renders its sections. */
    @Test
    fun textShowcase() = runUITest {
        setShowcase { TextShowcase() }
        waitForIdle()

        onNodeWithText(text = "Input").assertIsDisplayed()
    }

    /** Verifies that the image showcase renders its sections. */
    @Test
    fun imageShowcase() = runUITest {
        setShowcase { ImageShowcase() }
        waitForIdle()

        onNodeWithText(text = "Icon").assertIsDisplayed()
    }

    /** Verifies that the navigation showcase renders its sections. */
    @Test
    fun navigationShowcase() = runUITest {
        // The rails are laid out at fixed heights that add up past the test window, so they are given the unbounded height the scrolling list that renders them in the app gives them.
        setShowcase {
            Column(modifier = Modifier.verticalScroll(state = rememberScrollState())) { NavigationShowcase() }
        }
        waitForIdle()

        onNodeWithText(text = "Navigation Bar").assertIsDisplayed()
    }

    /** Verifies that the top bar showcase renders its sections. */
    @Test
    fun topBarShowcase() = runUITest {
        setShowcase { TopBarShowcase() }
        waitForIdle()

        onNodeWithText(text = "Top bar").assertIsDisplayed()
    }

    /** Verifies that the overlay showcase renders its sections. */
    @Test
    fun overlayShowcase() = runUITest {
        setShowcase { OverlayShowcase() }
        waitForIdle()

        onNodeWithText(text = "Static badges").assertIsDisplayed()
    }

    /** Verifies that the card showcase renders its sections. */
    @Test
    fun cardShowcase() = runUITest {
        setShowcase { CardShowcase() }
        waitForIdle()

        onNodeWithText(text = "Selectable Card").assertIsDisplayed()
    }

    /** Verifies that the list showcase renders its sections. */
    @Test
    fun listShowcase() = runUITest {
        setShowcase { ListShowcase() }
        waitForIdle()

        onNodeWithText(text = "Scrollable List with Scrollbar, List Items and Cards").assertIsDisplayed()
    }

    /** Verifies that the divider showcase renders its sections. */
    @Test
    fun dividerShowcase() = runUITest {
        setShowcase { DividerShowcase() }
        waitForIdle()

        onNodeWithText(text = "Horizontal").assertIsDisplayed()
    }
}
