package com.app.builder.ui.component.field

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.core.security.uuid
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.divider.Divider
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.text.Text

/**
 * An entity linked to the one being shown.
 *
 * @property uuid A unique identifier of the linked entity. Defaults to a random UUID.
 * @property name The name of the linked entity, shown as given.
 */
@Stable
data class LinkItem(
    val uuid: String = uuid().toString(),
    val name: String = "",
)

/**
 * The entities linked to the one being shown, set apart below a divider.
 * While [editing], each linked entity shows a remove button, and a link button calls [onLinkClick].
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param tag The prefix of the test tags of this section and its rows.
 * @param label The label of the section, translated.
 * @param items The linked entities.
 * @param editing Whether linked entities can be added and removed.
 * @param linkLabel The label of the link button, translated.
 * @param onLinkClick Callback for when the link button is clicked, or `null` to hide it.
 * @param onRemove Callback for when a linked entity is removed.
 * @param onClick Callback for when a linked entity is selected while not [editing], or `null` when they are not selectable.
 */
@Composable
fun LinkSection(
    modifier: Modifier = Modifier,
    tag: String = "link",
    label: String = "",
    items: ImmutableList<LinkItem> = persistentListOf(),
    editing: Boolean = false,
    linkLabel: String = "",
    onLinkClick: (() -> Unit)? = null,
    onRemove: (LinkItem) -> Unit = {},
    onClick: ((LinkItem) -> Unit)? = null,
) {
    val typography = LocalTypography.current

    Column(modifier = modifier.testTag(tag = tag)) {
        Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            text = label,
            maxLines = 1,
            style = typography.labelMedium
        )
        if (items.isEmpty()) FieldValue(value = null)
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .testTag(tag = "${tag}_${item.uuid}")
                    .fillMaxWidth()
                    .then(if (!editing && onClick != null) Modifier.clickable { onClick(item) } else Modifier)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier
                        .weight(weight = 1f)
                        .padding(vertical = 12.dp),
                    text = item.name,
                    translate = false,
                    style = typography.bodyLarge
                )
                if (editing) Button(
                    modifier = Modifier.testTag(tag = "${tag}_remove_${item.uuid}"),
                    content = { Icon(imageVector = Icons.Filled.Close, contentDescription = "remove") },
                    onClick = { onRemove(item) }
                )
            }
        }
        if (editing && onLinkClick != null) Button(
            modifier = Modifier
                .testTag(tag = "${tag}_link")
                .padding(horizontal = 16.dp, vertical = 4.dp),
            text = linkLabel,
            content = { Icon(imageVector = Icons.Filled.Add) },
            onClick = { onLinkClick() }
        )
    }
}

@PreviewLightDark
@Composable
private fun LinkSectionPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "building_equipments" to "Equipment",
            "building_link_equipments" to "Link equipments",
        )
    )
    Column {
        LinkSection(
            label = "building_equipments",
            items = persistentListOf(LinkItem(name = "Air Compressor"), LinkItem(name = "Freight Elevator")),
        )
        LinkSection(
            label = "building_equipments",
            items = persistentListOf(LinkItem(name = "Air Compressor")),
            editing = true,
            linkLabel = "building_link_equipments",
            onLinkClick = {},
        )
        LinkSection(label = "building_equipments")
    }
}
