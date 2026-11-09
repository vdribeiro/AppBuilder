package com.app.builder.ui.component.list

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.app.builder.core.security.uuid
import com.app.builder.ui.Preview
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.list.LazyColumn

/**
 * Task list.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param items An [ImmutableList] of [TaskItem]s.
 * @param onClick A callback invoked when an item is selected by the user.
 */
@Composable
fun TaskList(
    modifier: Modifier = Modifier,
    items: ImmutableList<TaskItem> = persistentListOf(),
    onClick: (TaskItem) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .testTag(tag = "task_list")
            .fillMaxSize(),
        horizontalAlignment = Alignment.Start,
    ) {
        itemsIndexed(items = items, key = { _, item -> item.uuid }) { index, item ->
            EntityRow(
                modifier = Modifier
                    .testTag(tag = "task_row_$index")
                    .clickable { onClick(item) },
                selected = item.selected,
                deleted = item.deleted,
                headline = item.title,
                supporting = item.description,
                metadata = listOf(item.uuid.takeIf { item.uuidVisible }, item.modifiedAt, item.deletedAt),
                leadingContent = { Icon(imageVector = if (item.completed) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked) }
            )
        }
    }
}

/**
 * Task item.
 *
 * @property uuid A unique identifier of the task. Defaults to a random UUID.
 * @property selected Whether the task is the one currently open.
 * @property deleted Whether the task is deleted.
 * @property uuidVisible Whether the UUID is shown on the row.
 * @property modifiedAt The timestamp indicating when this task was last updated.
 * @property deletedAt The timestamp indicating when this task was last deleted.
 * @property title The title of the task. Defaults to `null`.
 * @property description The description of the task. Defaults to `null`.
 * @property completed Whether the task is complete.
 */
@Stable
data class TaskItem(
    val uuid: String = uuid().toString(),
    val selected: Boolean = false,
    val deleted: Boolean = false,
    val uuidVisible: Boolean = false,
    val modifiedAt: String? = null,
    val deletedAt: String? = null,
    val title: String? = null,
    val description: String? = null,
    val completed: Boolean = false
)

@PreviewLightDark
@Composable
private fun TaskListPreview() = Preview {
    TaskList(
        items = persistentListOf(
            TaskItem(
                title = "Things"
            ),
            TaskItem(
                title = "More things",
                description = "Shiny",
                completed = true,
                selected = true
            ),
            TaskItem(
                title = "Gone",
                deleted = true
            )
        )
    )
}