package com.app.builder.ui.component.container

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.component.button.PushPayloadType
import com.app.builder.ui.component.button.PushTypeSelector
import com.app.builder.ui.component.button.UserItem
import com.app.builder.ui.component.button.UserMultiSelector
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.button.Switch
import com.app.builder.ui.core.divider.Divider
import com.app.builder.ui.core.text.Input
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.getTranslation

/**
 * Renders a form for composing and sending a push payload:
 * a type selector, a broadcast toggle, notification title/description fields, and a user multi-selector (when not broadcasting), and a send button.
 *
 * @param modifier Modifier applied to the root layout.
 * @param type Currently selected push payload type.
 * @param onTypeSelected Called when the user selects a different payload type.
 * @param broadcast Whether the push is sent to all users or only to [selectedUsers].
 * @param onBroadcastChange Called when the broadcast toggle is switched.
 * @param title Notification title field value, shown when [type] is [PushPayloadType.NOTIFICATION].
 * @param onTitleChange Called when the title field changes.
 * @param description Notification description field value, shown when [type] is [PushPayloadType.NOTIFICATION].
 * @param onDescriptionChange Called when the description field changes.
 * @param users All users available to select as recipients.
 * @param selectedUsers Users currently selected as recipients, shown when [broadcast] is `false`.
 * @param onUserToggled Called when a user's selection is toggled.
 * @param sending Whether a send request is currently in flight; shows a loading state on the send button.
 * @param onSendClick Called when the send button is clicked.
 * @param sent Number of pushes successfully sent, shown as a confirmation message, or `null` if none have been sent yet.
 */
@Composable
fun PushForm(
    modifier: Modifier = Modifier,
    type: PushPayloadType = PushPayloadType.NOTIFICATION,
    onTypeSelected: (PushPayloadType) -> Unit = {},
    broadcast: Boolean = false,
    onBroadcastChange: (Boolean) -> Unit = {},
    title: String = "",
    onTitleChange: (String) -> Unit = {},
    description: String = "",
    onDescriptionChange: (String) -> Unit = {},
    users: ImmutableList<UserItem> = persistentListOf(),
    selectedUsers: ImmutableList<UserItem> = persistentListOf(),
    onUserToggled: (UserItem) -> Unit = {},
    sending: Boolean = false,
    onSendClick: () -> Unit = {},
    sent: Int? = null,
) {
    val typography = LocalTypography.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "push_field_type", style = typography.titleMedium)
                PushTypeSelector(
                    type = type,
                    onTypeSelected = onTypeSelected,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "push_field_broadcast", style = typography.titleMedium)
                Switch(
                    checked = broadcast,
                    onCheckedChange = onBroadcastChange,
                )
            }
        }

        if (type == PushPayloadType.NOTIFICATION) {
            Divider()

            Column {
                Text(text = "push_field_title", style = typography.titleMedium)
                Input(
                    modifier = Modifier.fillMaxWidth(),
                    value = title,
                    onValueChange = onTitleChange,
                    maxLines = 1,
                )
            }

            Column {
                Text(text = "push_field_description", style = typography.titleMedium)
                Input(
                    modifier = Modifier.fillMaxWidth(),
                    value = description,
                    onValueChange = onDescriptionChange,
                    maxLines = 4,
                )
            }
        }

        if (!broadcast) {
            UserMultiSelector(
                users = users,
                selectedUsers = selectedUsers,
                onUserToggled = onUserToggled
            )
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            loading = sending,
            text = "push_send",
            onClick = { onSendClick() },
        )

        sent?.let { Text(text = getTranslation(key = "push_sent", it.toString()), translate = false, style = typography.bodyMedium) }
    }
}

@Preview
@Composable
private fun PushFormPreview() = Preview {
    PushForm()
}

@Preview
@Composable
private fun PushFormNotificationPreview() = Preview {
    val user = UserItem(name = "Tolkien")
    PushForm(
        title = "New book is out!",
        description = "Check it in stores.",
        users = persistentListOf(user),
        selectedUsers = persistentListOf(user),
    )
}
