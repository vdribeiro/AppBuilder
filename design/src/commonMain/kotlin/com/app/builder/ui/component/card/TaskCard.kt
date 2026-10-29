package com.app.builder.ui.component.card

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.component.text.InputField
import com.app.builder.ui.core.button.ButtonGroup
import com.app.builder.ui.core.button.ButtonGroupItem
import com.app.builder.ui.core.card.Card
import com.app.builder.ui.core.text.Text

/**
 * A task card.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param enabled Controls whether the input can accept user focus, selection gestures, or keystroke input updates.
 * @param selected The visual emphasis state of the card.
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
    selected: Boolean = false,
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
    val typography = LocalTypography.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 8.dp),
        selected = selected
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            title?.let {
                InputField(
                    enabled = enabled,
                    title = "task_title",
                    value = it,
                    onValueChange = onTitleChange
                )
            }
            description?.let {
                InputField(
                    enabled = enabled,
                    title = "task_description",
                    value = it,
                    onValueChange = onDescriptionChange
                )
            }
            state?.let {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.Top),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(text = "task_state", style = typography.titleLarge)
                    ButtonGroup(
                        modifier = Modifier.testTag(tag = "task_state"),
                        items = states.map { item ->
                            ButtonGroupItem(
                                enabled = enabled,
                                selected = item == it,
                                text = item,
                                onClick = { onStateChange(item) }
                            )
                        }.toPersistentList()
                    )
                }
            }
            modifiedAt?.let {
                InputField(
                    enabled = false,
                    title = "task_modified_at",
                    value = it,
                )
            }
            deletedAt?.let {
                InputField(
                    enabled = false,
                    title = "task_deleted_at",
                    value = it,
                )
            }
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
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        TaskCard(
            enabled = false,
            selected = false,
            title = "Task",
            description = "Noice!",
            state = "task_state_todo",
            states = persistentListOf("task_state_todo", "task_state_complete")
        )
        TaskCard(
            enabled = true,
            selected = false,
            title = "Task",
            description = "Noice!",
            state = "task_state_complete",
            states = persistentListOf("task_state_todo", "task_state_complete")
        )
    }
}

@PreviewLightDark
@Composable
private fun TaskCardSelectedPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "task_title" to "Title",
            "task_description" to "Description",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        TaskCard(
            enabled = true,
            selected = true,
            title = "Task",
            description = "Noice!"
        )
        TaskCard(
            enabled = false,
            selected = true,
            title = "Task",
            description = "Noice!"
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