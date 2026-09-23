package com.app.builder.ui.screen.design

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.app.builder.test.TestCase
import com.app.builder.ui.showcase.core.bar.TopBarShowcase
import com.app.builder.ui.showcase.core.card.CardShowcase
import com.app.builder.ui.showcase.core.divider.DividerShowcase
import com.app.builder.ui.showcase.core.image.ImageShowcase
import com.app.builder.ui.showcase.core.list.ListShowcase
import com.app.builder.ui.showcase.core.navigation.NavigationShowcase
import com.app.builder.ui.showcase.core.overlay.OverlayShowcase
import com.app.builder.ui.showcase.core.progress.ProgressShowcase
import com.app.builder.ui.showcase.core.text.TextShowcase

class DesignScreenTest: TestCase() {

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
        setShowcase { DesignScreen(section = DesignSection.COMPONENTS) }
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
        setShowcase { DesignScreen(section = DesignSection.COMPONENTS, onSectionClick = { clicked = it }) }
        waitForIdle()

        onNodeWithText(text = DesignSection.COLORS.text).performClick()
        waitForIdle()

        assertEquals(expected = DesignSection.COLORS, actual = clicked)
    }

    /** Verifies that the design navigation bar's app entry reports the user leaving the showcase. */
    @Test
    fun designNavigationAppClick() = runUITest {
        var clicked = false
        setShowcase { DesignScreen(section = DesignSection.COMPONENTS, onAppClick = { clicked = true }) }
        waitForIdle()

        onNodeWithText(text = "App").performClick()
        waitForIdle()

        assertTrue(actual = clicked)
    }

    /** Verifies that the colors section displays the color scheme's swatches. */
    @Test
    fun designColorsSection() = runUITest {
        setShowcase { DesignScreen(section = DesignSection.COLORS) }
        waitForIdle()

        onNodeWithText(text = "Primary").assertIsDisplayed()
    }

    /** Verifies that the shapes section displays the corner shape scale. */
    @Test
    fun designShapesSection() = runUITest {
        setShowcase { DesignScreen(section = DesignSection.SHAPES) }
        waitForIdle()

        onNodeWithText(text = "Extra small (4dp)").assertIsDisplayed()
    }

    /** Verifies that the typography section displays the text style scale. */
    @Test
    fun designTypographySection() = runUITest {
        setShowcase { DesignScreen(section = DesignSection.TYPOGRAPHY) }
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
