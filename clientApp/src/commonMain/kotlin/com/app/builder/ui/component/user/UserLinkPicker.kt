package com.app.builder.ui.component.user

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.User
import com.app.builder.domain.gateway.user.UserUseCases
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.link.Link
import com.app.builder.ui.component.link.LinkPicker
import com.app.builder.ui.component.link.LinkPickerAction
import com.app.builder.ui.component.link.LinkPickerState
import com.app.builder.ui.component.link.LinkPickerStore
import com.app.builder.ui.component.list.UserItem
import com.app.builder.ui.component.list.UserList
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * Creates the store of a user picker, picking from every user that is not deleted, with the user list filters.
 *
 * @param userUseCases Use cases used to observe every user.
 * @return The user picker store.
 */
internal fun userLinkPickerStore(userUseCases: UserUseCases): LinkPickerStore<User, UserItem> = LinkPickerStore(
    state = LinkPickerState(filters = userFilterDefaults),
    candidates = userUseCases.observeUsers().map { users -> users.filter { it.deletedAt == null } },
    filters = flow { emitAll(flow = AppFile.UserPreferences.cache().map { it ?: userFilterDefaults }) },
    uuid = { it.uuid },
    toItems = { candidates, filters, picked ->
        candidates.filtered(actionBarData = filters.copy(showDeleted = false)).map { user ->
            user.toUserItem(visibilityProperties = filters.visibleProperties, selectedUuid = null).copy(checked = user.uuid in picked)
        }
    }
)

/**
 * A dialog to tick the users to link, shown while the picker is open.
 *
 * @param store Provides the user picker state and receives its actions.
 * @param filtersStore The user list filters.
 * @param title The title of the dialog, translated.
 * @param onConfirm Callback for when the dialog is saved, with the ticked users as links.
 */
@Composable
fun UserLinkPicker(
    store: Store<LinkPickerState<User, UserItem>, LinkPickerAction>,
    filtersStore: Store<FiltersState, FiltersAction>,
    title: String = "",
    onConfirm: (ImmutableList<Link>) -> Unit = {},
) {
    LinkPicker(
        store = store,
        filtersStore = filtersStore,
        tag = "user_picker",
        title = title,
        onConfirm = { users -> onConfirm(users.map { Link(uuid = it.uuid, name = it.name) }.toImmutableList()) },
    ) { items, onClick ->
        UserList(items = items, onClick = { onClick(it.uuid) })
    }
}

@PreviewLightDark
@Composable
private fun UserLinkPickerPreview() = Preview {
    InjectTranslations(translations = mapOf("building_link_users" to "Link users"))
    UserLinkPicker(
        store = previewStore(
            initialState = LinkPickerState(
                open = true,
                items = persistentListOf(UserItem(name = "Vitor Ribeiro", checked = true), UserItem(name = "Jane Doe", checked = false)),
            )
        ),
        filtersStore = previewStore(initialState = FiltersState()),
        title = "building_link_users",
    )
}
