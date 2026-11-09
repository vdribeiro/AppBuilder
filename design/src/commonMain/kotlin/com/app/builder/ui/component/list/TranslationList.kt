package com.app.builder.ui.component.list

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.app.builder.ui.Preview
import com.app.builder.ui.core.list.LazyColumn

/**
 * Translation list.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param items An [ImmutableList] of [TranslationItem]s.
 */
@Composable
fun TranslationList(
    modifier: Modifier = Modifier,
    items: ImmutableList<TranslationItem> = persistentListOf(),
) {
    LazyColumn(
        modifier = modifier
            .testTag(tag = "translation_list")
            .fillMaxSize(),
        horizontalAlignment = Alignment.Start,
    ) {
        itemsIndexed(items = items, key = { _, item -> item.id }) { index, item ->
            EntityRow(
                modifier = Modifier.testTag(tag = "translation_row_$index"),
                headline = item.key,
                supporting = item.value,
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
 */
@Stable
data class TranslationItem(
    val id: String,
    val key: String? = null,
    val value: String? = null,
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
