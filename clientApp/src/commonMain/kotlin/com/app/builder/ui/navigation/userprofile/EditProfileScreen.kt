package com.app.builder.ui.navigation.userprofile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.domain.User
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.bar.TopActionBar
import com.app.builder.ui.component.container.EditProfile
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.modifier.onKeyPress
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The user-facing Edit Profile Screen.
 *
 * @param store Provides the edit profile screen state and receives its actions.
 */
@Composable
fun EditProfileScreen(store: Store<EditProfileScreenState, EditProfileScreenAction>) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.back() },
        topBar = {
            TopActionBar(
                title = "edit_profile",
                onBackClick = { router.back() }
            )
        },
        contentAlignment = Alignment.Center
    ) {
        EditProfile(
            modifier = Modifier.onKeyPress(
                keys = listOf(Key.Enter, Key.NumPadEnter),
                onKey = { store.send(action = EditProfileScreenAction.Submit) }
            ),
            name = state.name,
            onNameValueChange = { store.send(action = EditProfileScreenAction.UpdateName(name = it)) },
            avatar = state.avatar,
            onAvatarValueChange = { store.send(action = EditProfileScreenAction.UpdateAvatar(avatar = it)) },
            image = state.user?.avatar?.let { Image(url = it) },
            onSubmit = { store.send(action = EditProfileScreenAction.Submit) }
        )
    }
}

@PreviewLightDark
@Composable
private fun EditProfileScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "edit_profile" to "Edit Profile",
            "user_name" to "Name",
            "user_avatar" to "Avatar URL",
            "save" to "Save"
        )
    )
    val user = User(name = "Vitor Ribeiro")
    EditProfileScreen(
        store = previewStore(initialState = EditProfileScreenState(user = user, name = user.name))
    )
}
