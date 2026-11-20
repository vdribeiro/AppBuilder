package com.app.builder.ui.component.list

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.list.LazyColumn
import com.app.builder.ui.core.progress.ProgressIndicator
import com.app.builder.ui.core.text.Input
import com.app.builder.ui.core.text.Text

/**
 * Translation list, whose values are shown in inputs when [enabled].
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param items An [ImmutableList] of [TranslationItem]s.
 * @param enabled Whether the user can edit the values.
 * @param onValueChange Callback invoked with the edited item and value when the user makes a change.
 */
@Composable
fun TranslationList(
    modifier: Modifier = Modifier,
    items: ImmutableList<TranslationItem> = persistentListOf(),
    enabled: Boolean = false,
    onValueChange: (TranslationItem, String) -> Unit = { _, _ -> }
) {
    LazyColumn(
        modifier = modifier
            .testTag(tag = "translation_list")
            .fillMaxSize(),
        horizontalAlignment = Alignment.Start,
    ) {
        itemsIndexed(items = items, key = { _, item -> item.id }) { index, item ->
            if (enabled) EditableTranslationRow(
                modifier = Modifier.testTag(tag = "translation_row_$index"),
                index = index,
                item = item,
                onValueChange = { onValueChange(item, it) }
            ) else EntityRow(
                modifier = Modifier.testTag(tag = "translation_row_$index"),
                headline = item.key,
                supporting = item.value,
            )
        }
    }
}

/**
 * A translation row showing the key above an input holding the value, with a progress indicator at the end of the input while the item is pending.
 *
 * @param modifier The [Modifier] to be applied to the row's layout.
 * @param index The position of the row in the list, used in the test tag of its input.
 * @param item The translation shown by the row.
 * @param onValueChange Callback invoked with the new value on every change to the input.
 */
@Composable
private fun EditableTranslationRow(
    modifier: Modifier = Modifier,
    index: Int = 0,
    item: TranslationItem = TranslationItem(id = ""),
    onValueChange: (String) -> Unit = {}
) {
    val typography = LocalTypography.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp)
    ) {
        item.key?.let {
            Text(
                text = it,
                textAlign = TextAlign.Start,
                maxLines = 1,
                style = typography.bodyLarge
            )
        }
        item.value?.let {
            Input(
                modifier = Modifier
                    .testTag(tag = "translation_value_$index")
                    .fillMaxWidth(),
                value = it,
                onValueChange = onValueChange,
                trailingIcon = if (item.pending) {
                    { ProgressIndicator(modifier = Modifier.size(size = 16.dp)) }
                } else null
            )
        }
    }
}

/**
 * Translation item, where a `null` property is hidden.
 *
 * @property id A unique identifier of the translation within its language.
 * @property key The key of the translation.
 * @property value The localized value of the translation.
 * @property pending Whether an edit to the value is currently being persisted.
 */
@Stable
data class TranslationItem(
    val id: String,
    val key: String? = null,
    val value: String? = null,
    val pending: Boolean = false,
)

@PreviewLightDark
@Composable
private fun TranslationListPreview() = Preview {
    TranslationList(
        items = persistentListOf(
            TranslationItem(
                id = "app_name",
                key = "app_name",
                value = "App Builder"
            ),
            TranslationItem(
                id = "yes",
                key = "yes",
                value = "Yes"
            )
        )
    )
}

@PreviewLightDark
@Composable
private fun TranslationListEnabledPreview() = Preview {
    TranslationList(
        enabled = true,
        items = persistentListOf(
            TranslationItem(
                id = "app_name",
                key = "app_name",
                value = "App Builder"
            ),
            TranslationItem(
                id = "yes",
                key = "yes",
                value = "Yes",
                pending = true
            )
        )
    )
}
