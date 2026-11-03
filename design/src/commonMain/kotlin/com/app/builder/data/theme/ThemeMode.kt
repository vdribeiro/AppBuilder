package com.app.builder.data.theme

import kotlinx.serialization.Serializable

/** Which color scheme the theme displays. */
@Serializable
enum class ThemeMode {
    /** Follows the device's light or dark setting. */
    SYSTEM,
    /** Always displays the light color scheme. */
    LIGHT,
    /** Always displays the dark color scheme. */
    DARK
}