package com.app.builder.ui.component.container

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.field.TextField
import com.app.builder.ui.component.image.Avatar
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.getTranslation

/**
 * An edit profile form showing the user's avatar above their name and avatar URL, with the save button enabled once the name is filled.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param name The user's name.
 * @param onNameValueChange Callback invoked with the edited name when the user makes a change.
 * @param avatar The URL of the user's avatar image.
 * @param onAvatarValueChange Callback invoked with the edited avatar URL when the user makes a change.
 * @param image The user's avatar image.
 * @param onSubmit Callback invoked when the user submits the form.
 */
@Composable
fun EditProfile(
    modifier: Modifier = Modifier,
    name: String = "",
    onNameValueChange: (String) -> Unit = {},
    avatar: String = "",
    onAvatarValueChange: (String) -> Unit = {},
    image: Image? = null,
    onSubmit: () -> Unit = {},
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp)
    ) {
        Avatar(name = name, image = image, size = 96.dp, contentDescription = name)

        TextField(
            modifier = Modifier.testTag(tag = "user_name"),
            value = name,
            editing = true,
            onValueChange = onNameValueChange,
            label = "edit_profile_name",
        )

        TextField(
            modifier = Modifier.testTag(tag = "user_avatar"),
            value = avatar,
            editing = true,
            keyboardType = KeyboardType.Uri,
            onValueChange = onAvatarValueChange,
            label = "edit_profile_avatar",
        )

        Spacer(modifier = Modifier.height(height = 16.dp))

        Button(
            modifier = Modifier
                .testTag(tag = "save")
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            text = getTranslation(key = "edit_profile_save"),
            enabled = name.isNotBlank(),
            onClick = { onSubmit() }
        )
    }
}

@PreviewLightDark
@Composable
private fun EditProfilePreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "edit_profile_name" to "Name",
            "edit_profile_avatar" to "Avatar URL",
            "edit_profile_save" to "Save"
        )
    )
    EditProfile(name = "Vitor Ribeiro", avatar = "https://example.com/avatar.png")
}
