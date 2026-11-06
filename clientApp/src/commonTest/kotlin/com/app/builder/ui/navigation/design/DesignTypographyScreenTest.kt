package com.app.builder.ui.navigation.design

import kotlin.test.Test
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText

class DesignTypographyScreenTest: DesignScreenTestCase() {

    /** Verifies that the typography section displays the text style scale. */
    @Test
    fun designTypographySection() = runUITest {
        setShowcase { DesignTypographyScreen(actionBarStore = actionBarStore, navigationStore = navigationStore) }
        waitForIdle()

        onNodeWithText(text = "Display large").assertIsDisplayed()
    }
}
