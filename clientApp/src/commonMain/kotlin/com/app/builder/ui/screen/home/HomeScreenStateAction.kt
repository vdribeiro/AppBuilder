package com.app.builder.ui.screen.home

import com.app.builder.ui.core.image.Image

/**
 * State for the home screen, holding whether the session is authenticated and the current user's avatar.
 *
 * @property guest true if there is no authenticated user, false otherwise.
 * @property image Image of the current user's avatar.
 */
data class HomeScreenState(
    val guest: Boolean = true,
    val image: Image? = null,
)
