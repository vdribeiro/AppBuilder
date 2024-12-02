package com.app.builder.ui.component.button

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Checkbox
import com.app.builder.ui.core.text.Text

/**
 * Permission item.
 *
 * @property id Identifier of the entity the permission applies to.
 * @property name The displayed name of the entity the permission applies to.
 * @property description The displayed description of the entity the permission applies to.
 * @property read Whether the entity can be read. Defaults to false.
 * @property write Whether the entity can be written. Defaults to false.
 */
@Stable
data class PermissionItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val read: Boolean = false,
    val write: Boolean = false,
)

/**
 * A list of entities with a read and a write toggle each.
 * Toggling write on also toggles read on, and toggling read off also toggles write off, since writing implies reading.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param enabled Controls the enabled state of the toggles. When `false`, user interactions are disabled.
 * @param permissions An [ImmutableList] of [PermissionItem]s, one per entity.
 * @param onPermissionToggled Callback invoked with the edited item when the user makes a change.
 */
@Composable
fun PermissionSelector(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    permissions: ImmutableList<PermissionItem> = persistentListOf(),
    onPermissionToggled: (PermissionItem) -> Unit = {},
) {
    val typography = LocalTypography.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(weight = 1f),
                text = "permissions",
                style = typography.labelMedium
            )
            Text(
                modifier = Modifier.weight(weight = 1f),
                text = "description",
                style = typography.labelMedium
            )
            Text(
                modifier = Modifier.width(width = TOGGLE_WIDTH),
                text = "read",
                textAlign = TextAlign.Center,
                style = typography.labelMedium
            )
            Text(
                modifier = Modifier.width(width = TOGGLE_WIDTH),
                text = "write",
                textAlign = TextAlign.Center,
                style = typography.labelMedium
            )
        }

        permissions.forEach { permission ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(weight = 1f),
                    text = permission.name,
                    maxLines = 1,
                    style = typography.bodyMedium
                )
                Text(
                    modifier = Modifier.weight(weight = 1f),
                    text = permission.description,
                    maxLines = 1,
                    style = typography.bodyMedium
                )
                Box(
                    modifier = Modifier.width(width = TOGGLE_WIDTH),
                    contentAlignment = Alignment.Center
                ) {
                    Checkbox(
                        modifier = Modifier.testTag(tag = "${permission.id}_read"),
                        enabled = enabled,
                        checked = permission.read,
                        onCheckedChange = { onPermissionToggled(permission.copy(read = it, write = it && permission.write)) }
                    )
                }
                Box(
                    modifier = Modifier.width(width = TOGGLE_WIDTH),
                    contentAlignment = Alignment.Center
                ) {
                    Checkbox(
                        modifier = Modifier.testTag(tag = "${permission.id}_write"),
                        enabled = enabled,
                        checked = permission.write,
                        onCheckedChange = { onPermissionToggled(permission.copy(read = it || permission.read, write = it)) }
                    )
                }
            }
        }
    }
}

/** The fixed width of the read/write toggle columns. */
private val TOGGLE_WIDTH = 64.dp

@Preview
@Composable
private fun PermissionSelectorPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "permissions" to "Permissions",
            "read" to "Read",
            "write" to "Write"
        )
    )
    PermissionSelector(
        permissions = persistentListOf(
            PermissionItem(id = "CLIENT_FLAG", name = "Client Flag", description = "Access to client flags", read = true, write = true),
            PermissionItem(id = "CLIENT_CONFIG", name = "Client Config", read = true),
            PermissionItem(id = "USER", name = "User")
        )
    )
}
