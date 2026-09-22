package com.app.builder.ui.component.actionbar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.Preview
import com.app.builder.ui.component.bar.TopActionBar
import com.app.builder.ui.store.Store

/**
 * Renders a [TopActionBar] driven by [store], forwarding every user interaction back to it as an [ActionBarAction].
 *
 * @param store Store holding the [ActionBarState] to render and receiving the dispatched actions.
 */
@Composable
fun ActionBar(store: Store<ActionBarState, ActionBarAction>) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    TopActionBar(
        modifier = Modifier
            .testTag(tag = "action_bar")
            .fillMaxWidth(),
        title = state.title,
        avatarName = state.avatarName,
        avatarImage = state.avatarImage,
        onAvatarClick = { store.send(action = ActionBarAction.OpenUser) },
        mode = state.mode,
        layout = state.layout,
        write = state.write,
        onModeChange = { store.send(action = ActionBarAction.ModeChange(mode = it)) },
        onOkClick = { store.send(action = ActionBarAction.Ok(mode = it)) },
        onCancelClick = { store.send(action = ActionBarAction.Cancel(mode = it)) },
        onSearch = { store.send(action = ActionBarAction.Search(search = it)) },
        sortAscending = state.sortAscending,
        sortProperty = state.sortProperty,
        onSelectSortProperty = { store.send(action = ActionBarAction.SelectSortProperty(property = it)) },
        onSortAscendingClick = { store.send(action = ActionBarAction.SelectSortOrder(ascending = it)) },
        properties = state.properties,
        visibleProperties = state.visibleProperties,
        onVisiblePropertiesChange = { store.send(action = ActionBarAction.ToggleVisibleProperty(property = it)) },
        searchableProperties = state.searchableProperties,
        onSearchablePropertiesChange = { store.send(action = ActionBarAction.ToggleSearchableProperty(property = it)) },
    )
}

@Preview
@Composable
private fun ActionBarPreview() = Preview {
    ActionBar(
        store = Store(
            initialState = ActionBarState()
        )
    )
}
