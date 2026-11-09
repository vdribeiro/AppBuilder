package com.app.builder.ui.component.list

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.app.builder.core.security.uuid
import com.app.builder.ui.Preview
import com.app.builder.ui.component.image.Avatar
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.core.list.LazyColumn

/**
 * User list.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param items An [ImmutableList] of [UserItem]s.
 * @param onClick A callback invoked when an item is selected by the user.
 */
@Composable
fun UserList(
    modifier: Modifier = Modifier,
    items: ImmutableList<UserItem> = persistentListOf(),
    onClick: (UserItem) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .testTag(tag = "user_list")
            .fillMaxSize(),
        horizontalAlignment = Alignment.Start,
    ) {
        itemsIndexed(items = items, key = { _, item -> item.uuid }) { index, item ->
            EntityRow(
                modifier = Modifier
                    .testTag(tag = "user_row_$index")
                    .clickable { onClick(item) },
                selected = item.selected,
                deleted = item.deleted,
                headline = item.name,
                metadata = listOf(item.uuid.takeIf { item.uuidVisible }, item.modifiedAt, item.deletedAt),
                leadingContent = { Avatar(name = item.name, image = item.avatar, contentDescription = item.name) }
            )
        }
    }
}

/**
 * User item.
 *
 * @property uuid A unique identifier of the user. Defaults to a random UUID.
 * @property selected Whether the user is the one currently open.
 * @property deleted Whether the user is deleted.
 * @property uuidVisible Whether the UUID is shown on the row.
 * @property modifiedAt The timestamp indicating when this user was last updated.
 * @property deletedAt The timestamp indicating when this user was last deleted.
 * @property name The name of the user. Defaults to `null`.
 * @property avatar The avatar image of the user. Defaults to `null`.
 */
@Stable
data class UserItem(
    val uuid: String = uuid().toString(),
    val selected: Boolean = false,
    val deleted: Boolean = false,
    val uuidVisible: Boolean = false,
    val modifiedAt: String? = null,
    val deletedAt: String? = null,
    val name: String? = null,
    val avatar: Image? = null
)

@PreviewLightDark
@Composable
private fun UserListPreview() = Preview {
    UserList(
        items = persistentListOf(
            UserItem(
                name = "Vitor Ribeiro"
            ),
            UserItem(
                name = "Jane Doe",
                selected = true
            )
        )
    )
}
