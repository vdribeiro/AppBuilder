package com.app.builder.ui.component.actionbar

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.data.resource.ImageResource
import com.app.builder.domain.Account
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.User
import com.app.builder.ui.Preview
import com.app.builder.ui.component.bar.AvatarButton
import com.app.builder.ui.component.bar.EntityActions
import com.app.builder.ui.component.bar.Filters
import com.app.builder.ui.component.bar.FiltersButton
import com.app.builder.ui.component.bar.LoginButton
import com.app.builder.ui.component.bar.StatusIcons
import com.app.builder.ui.component.bar.TopActionBar
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.image.toImage
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * Renders a [TopActionBar] showing the current session, the screen's entity buttons and, when given, the list filters.
 * The add and delete buttons need write permission on [entityType], the edit and save buttons need it on [entityType] or any of [editEntityTypes].
 *
 * @param sessionStore Store holding the current user, their account and the application status.
 * @param filtersStore Store holding the list filters to show, or `null` to hide the filters.
 * @param entityType Entity the write permission is checked against, or `null` when the screen has no entity.
 * @param editEntityTypes Further entities whose write permission also allows editing.
 * @param deleted Whether the current domain is deleted, which turns the delete toggle into restore.
 * @param onAddClick Called when the add button is clicked, or `null` to hide it.
 * @param onEditClick Called when the edit button is clicked, or `null` to hide it.
 * @param onSaveClick Called when the save button is clicked, or `null` to hide it.
 * @param onCancelClick Called when the cancel button is clicked, or `null` to hide it.
 * @param onDeleteClick Called with `true` to delete and `false` to restore when the delete toggle is clicked, or `null` to hide it.
 */
@Composable
fun ActionBar(
    sessionStore: Store<SessionState, Unit>,
    filtersStore: Store<FiltersState, FiltersAction>? = null,
    entityType: EntityType? = null,
    editEntityTypes: Set<EntityType> = emptySet(),
    deleted: Boolean = false,
    onAddClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onSaveClick: (() -> Unit)? = null,
    onCancelClick: (() -> Unit)? = null,
    onDeleteClick: ((Boolean) -> Unit)? = null,
) {
    val router = LocalRouter.current
    val session by sessionStore.stateFlow.collectAsStateWithLifecycle()
    val filters = filtersStore?.stateFlow?.collectAsStateWithLifecycle()?.value ?: FiltersState()

    val account = session.account
    val writable = entityType != null && account?.hasPermission(entityType = entityType, permission = Permission.WRITE) == true
    val editable = writable || editEntityTypes.any { account?.hasPermission(entityType = it, permission = Permission.WRITE) == true }
    val user = session.user

    TopActionBar(
        modifier = Modifier
            .testTag(tag = "action_bar")
            .fillMaxWidth(),
        leading = {
            AvatarButton(
                name = user?.name,
                image = when {
                    !session.loaded -> null
                    user == null -> ImageResource.Kotlin.toImage()
                    else -> user.avatar?.let { Image(url = it) }
                },
                onClick = { router.navigate(screen = Screen.UserProfile) }
            )
            StatusIcons(synced = session.synced, clockTrusted = session.clockTrusted, sessionExpired = session.sessionExpired)
        },
        actions = {
            EntityActions(
                onAddClick = onAddClick?.takeIf { writable },
                onEditClick = onEditClick?.takeIf { editable },
                onSaveClick = onSaveClick?.takeIf { editable },
                onCancelClick = onCancelClick,
                deleted = deleted,
                onDeleteClick = onDeleteClick?.takeIf { writable },
            )
            if (filtersStore != null) FiltersButton(
                expanded = filters.filtersExpanded,
                onClick = { filtersStore.send(action = FiltersAction.ExpandFilters(expanded = it)) }
            )
            if (session.loaded && user == null) LoginButton(onClick = { router.navigate(screen = Screen.Login) })
        },
        secondaryActions = if (filtersStore != null && filters.filtersExpanded) {
            {
                Filters(
                    properties = filters.properties,
                    search = filters.search,
                    searchExpanded = filters.searchExpanded,
                    onSearchExpandClick = { filtersStore.send(action = FiltersAction.ExpandSearch(expanded = it)) },
                    onSearch = { filtersStore.send(action = FiltersAction.Search(search = it)) },
                    searchableProperties = filters.searchableProperties,
                    onSearchablePropertiesChange = { filtersStore.send(action = FiltersAction.ToggleSearchableProperty(property = it)) },
                    sortAscending = filters.sortAscending,
                    onSortAscendingClick = { filtersStore.send(action = FiltersAction.SelectSortOrder(ascending = it)) },
                    sortProperty = filters.sortProperty,
                    onSelectSortProperty = { filtersStore.send(action = FiltersAction.SelectSortProperty(property = it)) },
                    visibleProperties = filters.visibleProperties,
                    onVisiblePropertiesChange = { filtersStore.send(action = FiltersAction.ToggleVisibleProperty(property = it)) },
                    showDeleted = filters.showDeleted,
                    onShowDeletedClick = if (filters.showDeletedToggle) {
                        { filtersStore.send(action = FiltersAction.ShowDeleted(showDeleted = it)) }
                    } else null,
                )
            }
        } else null,
    )
}

@PreviewLightDark
@Composable
private fun ActionBarLoggedOutPreview() = Preview {
    ActionBar(sessionStore = previewStore(initialState = SessionState(loaded = true)))
}

@PreviewLightDark
@Composable
private fun ActionBarListPreview() = Preview {
    ActionBar(
        sessionStore = previewStore(initialState = previewSession(synced = false)),
        filtersStore = previewStore(
            initialState = FiltersState(
                properties = persistentMapOf("title" to "Title", "state" to "State"),
                filtersExpanded = true,
                sortProperty = "title",
                visibleProperties = persistentListOf("title", "state"),
            )
        ),
        entityType = EntityType.TASK,
        onAddClick = {},
    )
}

@PreviewLightDark
@Composable
private fun ActionBarDetailPreview() = Preview {
    ActionBar(
        sessionStore = previewStore(initialState = previewSession()),
        entityType = EntityType.TASK,
        onEditClick = {},
        onDeleteClick = {},
    )
}

@PreviewLightDark
@Composable
private fun ActionBarEditPreview() = Preview {
    ActionBar(
        sessionStore = previewStore(initialState = previewSession()),
        entityType = EntityType.TASK,
        onSaveClick = {},
        onCancelClick = {},
    )
}

/**
 * Builds a signed in session whose account can write every entity, for previews.
 *
 * @param synced Whether the session shows as synced.
 * @return The preview session.
 */
private fun previewSession(synced: Boolean = true): SessionState {
    val user = User(name = "V R")
    return SessionState(
        loaded = true,
        user = user,
        account = Account(
            userUuid = user.uuid,
            username = "preview",
            permissions = EntityType.entries.associateWith { Permission.WRITE },
        ),
        synced = synced,
    )
}
