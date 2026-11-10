package com.app.builder.ui.component.bar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalColorScheme
import com.app.builder.ui.Preview
import com.app.builder.ui.component.image.Avatar
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.core.overlay.Tooltip

/**
 * Renders the current user's avatar for the [TopActionBar] leading slot.
 *
 * @param name The name to fit in the avatar. Only the first letter of the first and last name will show capitalized if no image is provided.
 * @param image The image resource.
 * @param onClick Callback for the avatar click action.
 */
@Composable
fun AvatarButton(name: String? = null, image: Image? = null, onClick: (() -> Unit)? = null) {
    Avatar(
        modifier = Modifier
            .testTag(tag = "action_avatar")
            .clickable { onClick?.invoke() },
        name = name,
        image = image,
        size = 40.dp,
    )
}

/**
 * Renders the application status icons for the [TopActionBar] leading slot, in the error color, showing only what is wrong.
 * Each icon explains itself in a tooltip.
 *
 * @param synced Shows a sync disabled icon when `false`, unless the session expired, since nothing can sync until the user logs in again.
 * @param clockTrusted Shows a clock icon when `false`.
 * @param sessionExpired Shows a warning icon when `true`, asking the user to log out and log in again.
 */
@Composable
fun StatusIcons(synced: Boolean = true, clockTrusted: Boolean = true, sessionExpired: Boolean = false) {
    val color = LocalColorScheme.current
    if (sessionExpired) Tooltip(text = "status_session_expired") {
        Icon(
            modifier = Modifier.testTag(tag = "status_session_expired"),
            imageVector = Icons.Default.Warning,
            tint = color.error
        )
    }
    if (!synced && !sessionExpired) Tooltip(text = "status_not_synced") {
        Icon(
            modifier = Modifier.testTag(tag = "status_not_synced"),
            imageVector = Icons.Default.SyncDisabled,
            tint = color.error
        )
    }
    if (!clockTrusted) Tooltip(text = "status_clock_untrusted") {
        Icon(
            modifier = Modifier.testTag(tag = "status_clock_untrusted"),
            imageVector = Icons.Default.Schedule,
            tint = color.error
        )
    }
}

/**
 * Renders the buttons acting on the current domain for the [TopActionBar] actions slot.
 *
 * @param onAddClick Shows an add button when not null and executes this callback when clicked.
 * @param onEditClick Shows an edit button when not null and executes this callback when clicked.
 * @param onSaveClick Shows a save button when not null and executes this callback when clicked.
 * @param onCancelClick Shows a cancel button when not null and executes this callback when clicked.
 * @param deleted Whether the current domain is deleted.
 * @param onDeleteClick Shows a delete toggle button when not null and is called with the new deletion value when it is clicked.
 */
@Composable
fun EntityActions(
    onAddClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onSaveClick: (() -> Unit)? = null,
    onCancelClick: (() -> Unit)? = null,
    deleted: Boolean = false,
    onDeleteClick: ((Boolean) -> Unit)? = null,
) {
    if (onAddClick != null) Button(
        modifier = Modifier.testTag(tag = "action_add"),
        onClick = { onAddClick() },
        content = { Icon(imageVector = Icons.Default.Add) }
    )

    if (onEditClick != null) Button(
        modifier = Modifier.testTag(tag = "action_edit"),
        onClick = { onEditClick() },
        content = { Icon(imageVector = Icons.Default.Edit) }
    )

    if (onSaveClick != null) Button(
        modifier = Modifier.testTag(tag = "action_save"),
        onClick = { onSaveClick() },
        content = { Icon(imageVector = Icons.Default.Save) }
    )

    if (onCancelClick != null) Button(
        modifier = Modifier.testTag(tag = "action_cancel"),
        onClick = { onCancelClick() },
        content = { Icon(imageVector = Icons.Default.Close) }
    )

    if (onDeleteClick != null) Button(
        modifier = Modifier.testTag(tag = "action_delete"),
        onClick = { onDeleteClick(!deleted) },
        content = { Icon(imageVector = if (deleted) Icons.Default.Restore else Icons.Default.Delete) }
    )
}

/**
 * Renders the login button for the [TopActionBar] actions slot.
 *
 * @param onClick Callback for the login click action.
 */
@Composable
fun LoginButton(onClick: () -> Unit) {
    Button(
        modifier = Modifier.testTag(tag = "action_login"),
        onClick = { onClick() },
        content = { Icon(imageVector = Icons.AutoMirrored.Filled.Login) }
    )
}

@PreviewLightDark
@Composable
private fun AvatarButtonPreview() = Preview {
    AvatarButton(name = "V R")
}

@PreviewLightDark
@Composable
private fun StatusIconsPreview() = Preview {
    Column {
        Row { StatusIcons(synced = false, clockTrusted = false) }
        Row { StatusIcons(synced = false, clockTrusted = false, sessionExpired = true) }
    }
}

@PreviewLightDark
@Composable
private fun EntityActionsViewPreview() = Preview {
    Row { EntityActions(onAddClick = {}, onEditClick = {}) {} }
}

@PreviewLightDark
@Composable
private fun EntityActionsEditPreview() = Preview {
    Row { EntityActions(onSaveClick = {}, onCancelClick = {}) }
}

@PreviewLightDark
@Composable
private fun EntityActionsDeletedPreview() = Preview {
    Row { EntityActions(deleted = true) {} }
}

@PreviewLightDark
@Composable
private fun LoginButtonPreview() = Preview {
    LoginButton(onClick = {})
}
