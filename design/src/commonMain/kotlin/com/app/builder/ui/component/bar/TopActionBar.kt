package com.app.builder.ui.component.bar

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.Preview
import com.app.builder.ui.core.bar.TopBar
import com.app.builder.ui.core.bar.TopBarVariant
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.text.Text

/**
 * Renders the top action bar, a [TopBar] with a back button, a title and slots for what leads the title, the actions and a secondary row of actions.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param title Text shown as the action bar's title.
 * @param onBackClick Shows a back arrow when not null and executes this callback when clicked.
 * @param leading Content shown before the title, such as an [AvatarButton] and [StatusIcons].
 * @param actions Content shown at the end of the bar, such as [EntityActions], a [FiltersButton] and a [LoginButton].
 * @param secondaryActions Content shown in a row below the bar, such as [Filters], or `null` to hide the row.
 */
@Composable
fun TopActionBar(
    modifier: Modifier = Modifier,
    title: String = "",
    onBackClick: (() -> Unit)? = null,
    leading: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    secondaryActions: (@Composable RowScope.() -> Unit)? = null,
) {
    TopBar(
        modifier = modifier.testTag(tag = "action_bar"),
        variant = TopBarVariant.SMALL,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                leading()
                Text(text = title)
            }
        },
        navigationIcon = {
            if (onBackClick != null) Button(
                modifier = Modifier.testTag(tag = "action_back"),
                onClick = { onBackClick() },
                content = { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack) }
            )
        },
        actions = actions,
        secondaryActions = secondaryActions
    )
}

@PreviewLightDark
@Composable
private fun TopActionBarPreview() = Preview {
    TopActionBar(title = "None")
}

@PreviewLightDark
@Composable
private fun TopActionBarBackLoginPreview() = Preview {
    TopActionBar(
        title = "All",
        onBackClick = {},
        leading = { AvatarButton(name = "V R") },
        actions = { LoginButton(onClick = {}) },
    )
}

@PreviewLightDark
@Composable
private fun TopActionBarWritePreview() = Preview {
    TopActionBar(
        title = "All",
        leading = {
            AvatarButton(name = "V R")
            StatusIcons(synced = false, clockTrusted = false)
        },
        actions = {
            EntityActions(onAddClick = {}, onEditClick = {}) {}
            FiltersButton(expanded = true, onClick = {})
        },
        secondaryActions = {
            Filters(
                properties = persistentMapOf("name" to "Name", "type" to "Type"),
                onSearch = {},
                onSearchablePropertiesChange = {},
                onSortAscendingClick = {},
                sortProperty = "name",
                onSelectSortProperty = {},
                onVisiblePropertiesChange = {}
            ) {}
        }
    )
}

@PreviewLightDark
@Composable
private fun TopActionBarEditPreview() = Preview {
    TopActionBar(
        leading = {
            AvatarButton(name = "V R")
            StatusIcons(synced = false, clockTrusted = false)
        },
        actions = {
            EntityActions(onSaveClick = {}, onCancelClick = {})
            FiltersButton(expanded = true, onClick = {})
        },
        secondaryActions = {
            Filters(
                properties = persistentMapOf("name" to "Name", "type" to "Type"),
                search = "query",
                searchExpanded = true,
                onSearch = {},
                searchableProperties = persistentListOf("name"),
                onSearchablePropertiesChange = {},
            )
        }
    )
}
