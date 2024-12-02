package com.app.builder.ui.component.useravatar

import com.app.builder.ui.core.image.Image

/**
 * The user avatar state.
 *
 * @property userName Name of the current user, used to derive the leading avatar's initials.
 * @property userImage Image of the current user's avatar.
 */
data class UserAvatarState(
    val userName: String? = null,
    val userImage: Image? = null,
)
