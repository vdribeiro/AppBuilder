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
import com.app.builder.ui.core.button.Button

/**
 * A change password form of passwords that can be shown, with the submit button enabled once every password is filled and the confirmation matches the new password.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param loading Whether the password change is currently loading.
 * @param currentPassword The current password.
 * @param onCurrentPasswordValueChange Callback invoked with the edited current password when the user makes a change.
 * @param newPassword The new password.
 * @param onNewPasswordValueChange Callback invoked with the edited new password when the user makes a change.
 * @param confirmPassword The confirmation of the new password, in an error state while it is filled and differs from [newPassword].
 * @param onConfirmPasswordValueChange Callback invoked with the edited confirmation when the user makes a change.
 * @param onSubmit Callback invoked when the user submits the form.
 */
@Composable
fun ChangePassword(
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    currentPassword: String = "",
    onCurrentPasswordValueChange: (String) -> Unit = {},
    newPassword: String = "",
    onNewPasswordValueChange: (String) -> Unit = {},
    confirmPassword: String = "",
    onConfirmPasswordValueChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp)
    ) {
        TextField(
            modifier = Modifier.testTag(tag = "current_password"),
            value = currentPassword,
            editing = true,
            keyboardType = KeyboardType.Password,
            onValueChange = onCurrentPasswordValueChange,
            label = "current_password",
            enabled = !loading,
        )

        TextField(
            modifier = Modifier.testTag(tag = "new_password"),
            value = newPassword,
            editing = true,
            keyboardType = KeyboardType.Password,
            onValueChange = onNewPasswordValueChange,
            label = "new_password",
            enabled = !loading,
        )

        TextField(
            modifier = Modifier.testTag(tag = "confirm_password"),
            value = confirmPassword,
            editing = true,
            keyboardType = KeyboardType.Password,
            onValueChange = onConfirmPasswordValueChange,
            label = "confirm_password",
            enabled = !loading,
            isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
        )

        Spacer(modifier = Modifier.height(height = 16.dp))

        Button(
            modifier = Modifier
                .testTag(tag = "change_password")
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            text = "change_password",
            loading = loading,
            enabled = !loading && currentPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword == newPassword,
            onClick = { onSubmit() }
        )
    }
}

@PreviewLightDark
@Composable
private fun ChangePasswordPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "current_password" to "Current Password",
            "new_password" to "New Password",
            "confirm_password" to "Confirm Password",
            "change_password" to "Change Password"
        )
    )
    ChangePassword(
        currentPassword = "password123",
        newPassword = "password456",
        confirmPassword = "password45"
    )
}
