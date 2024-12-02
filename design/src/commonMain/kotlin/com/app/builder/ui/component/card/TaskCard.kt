package com.app.builder.ui.component.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.text.InputField
import com.app.builder.ui.core.card.Card

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
) {
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

@Preview
@Composable
private fun TaskCardPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "task_title" to "Title",
            "task_description" to "Description",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        TaskCard(
            enabled = false,
            selected = false,
            title = "Task",
            description = "Noice!"
        )
        TaskCard(
            enabled = true,
            selected = false,
            title = "Task",
            description = "Noice!"
        )
    }
}

@Preview
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

@Preview
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