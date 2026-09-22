package com.app.builder.ui.component.list

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.core.security.uuid
import com.app.builder.ui.Preview
import com.app.builder.ui.component.card.TaskCard
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
        verticalArrangement = Arrangement.spacedBy(space = 2.dp)
    ) {
        itemsIndexed(items = items, key = { _, item -> item.uuid }) { index, item ->
            TaskCard(
                modifier = Modifier
                    .testTag(tag = "task_card_$index")
                    .clickable { onClick(item) },
                selected = item.selected,
                modifiedAt = item.modifiedAt,
                deletedAt = item.deletedAt,
                title = item.title,
                description = item.description,
            )
        }
    }
}

/**
 * Task item.
 *
 * @property uuid A unique identifier of the task. Defaults to a random UUID.
 * @property selected The visual emphasis state of the card.
 * @property modifiedAt The timestamp indicating when this task was last updated.
 * @property deletedAt The timestamp indicating when this task was last deleted.
 * @property title The title of the task. Defaults to `null`.
 * @property description The description of the task. Defaults to `null`.
 */
@Stable
data class TaskItem(
    val uuid: String = uuid().toString(),
    val selected: Boolean = false,
    val modifiedAt: String? = null,
    val deletedAt: String? = null,
    val title: String? = null,
    val description: String? = null,
    val state: String? = null
)

@Preview
@Composable
private fun TaskListPreview() = Preview {
    TaskList(
        items = persistentListOf(
            TaskItem(
                title = "Things"
            ),
            TaskItem(
                title = "More things",
                description = "Shiny"
            )
        )
    )
}