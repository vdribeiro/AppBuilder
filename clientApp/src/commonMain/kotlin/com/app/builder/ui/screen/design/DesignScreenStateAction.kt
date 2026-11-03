package com.app.builder.ui.screen.design

import com.app.builder.data.theme.CustomTheme

/** Actions dispatched by the design UI. */
sealed interface DesignScreenAction {
    /**
     * Persists the theme customized in the showcases.
     *
     * @param customTheme The theme to persist.
     */
    data class SaveTheme(val customTheme: CustomTheme): DesignScreenAction
}
