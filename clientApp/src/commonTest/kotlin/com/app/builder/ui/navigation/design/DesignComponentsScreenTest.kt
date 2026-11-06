package com.app.builder.ui.navigation.design

import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.authenticatedScreen
import com.app.builder.ui.showcase.core.bar.TopBarShowcase
import com.app.builder.ui.showcase.core.card.CardShowcase
import com.app.builder.ui.showcase.core.divider.DividerShowcase
import com.app.builder.ui.showcase.core.image.ImageShowcase
import com.app.builder.ui.showcase.core.list.ListShowcase
import com.app.builder.ui.showcase.core.navigation.NavigationShowcase
import com.app.builder.ui.showcase.core.overlay.OverlayShowcase
import com.app.builder.ui.showcase.core.progress.ProgressShowcase
import com.app.builder.ui.showcase.core.text.TextShowcase

class DesignComponentsScreenTest: DesignScreenTestCase() {

    /** Verifies that the design screens show the action bar and the design section of the navigation bar, with every design route and the app item. */
    @Test
    fun designNavigationItems() = runUITest {
        setShowcase { DesignComponentsScreen(actionBarStore = actionBarStore, navigationStore = navigationStore) }
        waitForIdle()

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        listOf("app", "design_components", "design_colors", "design_shapes", "design_typography").forEach { text ->
            onNode(matcher = hasText(text = text) and hasAnyAncestor(matcher = hasTestTag(testTag = "navigation_bar"))).assertIsDisplayed()
        }
        onNode(matcher = isSelected() and hasText(text = "design_components")).assertIsDisplayed()
    }

    /** Verifies that selecting another design route in the navigation bar opens its screen. */
    @Test
    fun designNavigationSectionClick() = runUITest {
        setShowcase { DesignComponentsScreen(actionBarStore = actionBarStore, navigationStore = navigationStore) }
        waitForIdle()

        onNodeWithText(text = "design_colors").performClick()
        waitForIdle()

        assertEquals(expected = listOf<NavKey>(Screen.DesignColors), actual = router.backStack.toList())
    }

    /** Verifies that the navigation bar's app item leaves the design section. */
    @Test
    fun designNavigationAppClick() = runUITest {
        setShowcase { DesignComponentsScreen(actionBarStore = actionBarStore, navigationStore = navigationStore) }
        waitForIdle()

        onNodeWithText(text = "app").performClick()
        waitForIdle()

        assertEquals(expected = listOf<NavKey>(authenticatedScreen), actual = router.backStack.toList())
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
