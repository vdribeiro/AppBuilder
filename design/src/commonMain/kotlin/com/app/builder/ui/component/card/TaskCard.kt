package com.app.builder.ui.component.card

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.field.ChoiceField
import com.app.builder.ui.component.field.MetadataSection
import com.app.builder.ui.component.field.TextField
import com.app.builder.ui.core.card.Card

/**
 * A task card, showing only the properties that are not `null`.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param enabled Whether the task's properties can be edited.
 * @param uuid The UUID of the task.
 * @param modifiedAt The modification timestamp of the task.
 * @param deletedAt The deletion timestamp of the task.
 * @param title The title of the task.
 * @param onTitleChange Callback for when the title changes.
 * @param description The description of the task.
 * @param onDescriptionChange Callback for when the description changes.
 * @param state The task's state, shown as selected among [states].
 * @param states Every state the task can be in.
 * @param onStateChange Callback for when a state is selected.
 */
@Composable
fun TaskCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    uuid: String? = null,
    modifiedAt: String? = null,
    deletedAt: String? = null,
    title: String? = null,
    onTitleChange: (String) -> Unit = {},
    description: String? = null,
    onDescriptionChange: (String) -> Unit = {},
    state: String? = null,
    states: ImmutableList<String> = persistentListOf(),
    onStateChange: (String) -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            title?.let {
                TextField(
                    label = "task_title",
                    value = it,
                    editing = enabled,
                    onValueChange = onTitleChange
                )
            }
            description?.let {
                TextField(
                    label = "task_description",
                    value = it,
                    editing = enabled,
                    onValueChange = onDescriptionChange,
                    maxLines = Int.MAX_VALUE
                )
            }
            state?.let {
                ChoiceField(
                    modifier = Modifier.testTag(tag = "task_state"),
                    label = "task_state",
                    selected = it,
                    options = states,
                    editing = enabled,
                    onSelect = onStateChange
                )
            }
            MetadataSection(
                entries = persistentListOf(
                    "task_uuid" to uuid,
                    "task_modified_at" to modifiedAt,
                    "task_deleted_at" to deletedAt,
                )
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TaskCardPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "task_title" to "Title",
            "task_description" to "Description",
            "task_state" to "State",
            "task_state_todo" to "To do",
            "task_state_complete" to "Complete",
            "task_modified_at" to "Modified At",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        TaskCard(
            enabled = false,
            modifiedAt = "2026-11-09T10:00:00Z",
            title = "Task",
            description = "Noice!",
            state = "task_state_todo",
            states = persistentListOf("task_state_todo", "task_state_complete")
        )
        TaskCard(
            enabled = true,
            title = "Task",
            description = "Noice!",
            state = "task_state_complete",
            states = persistentListOf("task_state_todo", "task_state_complete")
        )
    }
}

@PreviewLightDark
@Composable
private fun TaskCardNullablePreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "task_title" to "Title",
            "task_description" to "Description",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        TaskCard(
            title = "Task",
        )
        TaskCard(
            description = "Noice!"
        )
    }
}
