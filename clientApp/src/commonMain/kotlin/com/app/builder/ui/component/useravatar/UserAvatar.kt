package com.app.builder.ui.component.useravatar

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.Preview
import com.app.builder.ui.component.Store
import com.app.builder.ui.component.image.Avatar
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen

/**
 * Renders the user avatar.
 *
 * @param store The store holding the user avatar state and handling its actions.
 */
@Composable
fun UserAvatar(store: Store<UserAvatarState, Unit>) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val router = LocalRouter.current

    Avatar(
        modifier = Modifier
            .testTag(tag = "action_avatar")
            .clickable { router.navigate(screen = Screen.UserProfile) },
        name = state.userName,
        image = state.userImage,
        size = 40.dp,
    )
}

@Preview
@Composable
private fun UserAvatarPreview() = Preview {
    UserAvatar(
        store = Store(
            initialState = UserAvatarState()
        )
    )
}
