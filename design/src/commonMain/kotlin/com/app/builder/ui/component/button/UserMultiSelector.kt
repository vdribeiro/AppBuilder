package com.app.builder.ui.component.button

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.core.security.uuid
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Checkbox
import com.app.builder.ui.core.card.Card
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.getTranslation

/**
 * User item.
 *
 * @property uuid A unique identifier of the user. Defaults to a random UUID.
 * @property name The name of the user. Defaults to empty.
 */
@Stable
data class UserItem(
    val uuid: String = uuid().toString(),
    val name: String = "",
)

/**
 * A scrollable list of users.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param users An [ImmutableList] of [UserItem]s.
 * @param selectedUsers An [ImmutableList] of [UserItem]s that are currently selected.
 * @param onUserToggled Callback invoked with the edited item when the user makes a change.
 */
@Composable
fun UserMultiSelector(
    modifier: Modifier = Modifier,
    users: ImmutableList<UserItem> = persistentListOf(),
    selectedUsers: ImmutableList<UserItem> = persistentListOf(),
    onUserToggled: (UserItem) -> Unit = {},
) {
    val typography = LocalTypography.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Text(
            text = getTranslation(key = "users", selectedUsers.size.toString()),
            translate = false,
            style = typography.titleMedium,
        )
        users.forEach { user ->
            val selected = selectedUsers.any { it.uuid == user.uuid }
            Card(
                modifier = Modifier.fillMaxWidth(),
                selected = selected,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = user.name, style = typography.bodyMedium)
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onUserToggled(user) },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun UserMultiSelectorPreview() = Preview {
    UserMultiSelector(
        users = persistentListOf(
            UserItem(name = "Tolkien"),
            UserItem(name = "Bob"),
            UserItem(name = "Alice"),
            UserItem(name = "Eve"),
        ),
        selectedUsers = persistentListOf(
            UserItem(name = "Tolkien"),
            UserItem(name = "Bob"),
        ),
        onUserToggled = {},
    )
}
