package com.app.builder.ui.component.actionbar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.Preview
import com.app.builder.ui.component.bar.TopActionBar
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

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
        properties = state.properties,
        avatarName = state.avatarName,
        avatarImage = state.avatarImage,
        onAvatarClick = { store.send(action = ActionBarAction.OpenUser) },
        synced = state.synced,
        clockTrusted = state.clockTrusted,
        onAddClick = if (state.showAdd) {
            { store.send(action = ActionBarAction.Add) }
        } else null,
        onEditClick = if (state.showEdit) {
            { store.send(action = ActionBarAction.Edit) }
        } else null,
        onSaveClick = if (state.showSave) {
            { store.send(action = ActionBarAction.Save) }
        } else null,
        onCancelClick = if (state.showCancel) {
            { store.send(action = ActionBarAction.Cancel) }
        } else null,
        onDeleteClick = if (state.showDelete) {
            { store.send(action = ActionBarAction.Delete) }
        } else null,
        onLoginClick = if (state.showLogin) {
            { store.send(action = ActionBarAction.Login) }
        } else null,
        onSearch = if (state.showFilters) {
            { store.send(action = ActionBarAction.Search(search = it)) }
        } else null,
        searchableProperties = state.searchableProperties,
        onSearchablePropertiesChange = if (state.showFilters) {
            { store.send(action = ActionBarAction.ToggleSearchableProperty(property = it)) }
        } else null,
        sortAscending = state.sortAscending,
        onSortAscendingClick = if (state.showFilters) {
            { store.send(action = ActionBarAction.SelectSortOrder(ascending = it)) }
        } else null,
        sortProperty = state.sortProperty,
        onSelectSortProperty = if (state.showFilters) {
            { store.send(action = ActionBarAction.SelectSortProperty(property = it)) }
        } else null,
        visibleProperties = state.visibleProperties,
        onVisiblePropertiesChange = if (state.showFilters) {
            { store.send(action = ActionBarAction.ToggleVisibleProperty(property = it)) }
        } else null,
        showDeleted = state.showDeleted,
        onShowDeletedClick = if (state.showFilters && state.showDeletedToggle) {
            { store.send(action = ActionBarAction.ShowDeleted(showDeleted = it)) }
        } else null,
    )
}

@PreviewLightDark
@Composable
private fun ActionBarPreview() = Preview {
    ActionBar(
        store = previewStore(
            initialState = ActionBarState(),
        )
    )
}
