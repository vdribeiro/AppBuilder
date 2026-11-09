package com.app.builder.ui.navigation.user

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import androidx.compose.runtime.snapshotFlow
import com.app.builder.core.locale.getLocalDateTime
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.User
import com.app.builder.domain.User.Property
import com.app.builder.domain.gateway.user.UserUseCases
import com.app.builder.ui.component.list.UserItem
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * Store backing the user list, combining users with the shared action bar state to filter, sort, and select them.
 *
 * @param state Initial user list state.
 * @property router The router used for navigation.
 * @property userUseCases Use cases used to observe users.
 */
class UserListScreenStore(
    state: UserListScreenState,
    private val router: Router,
    private val userUseCases: UserUseCases
): Store<UserListScreenState, UserListScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: UserListScreenState, action: UserListScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is UserListScreenAction.SelectUser -> selectUser(action = action)
        }
    }

    /**
     * Observes users and keeps the displayed list in sync.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val usersFlow = userUseCases.observeUsers().toStateFlow(initialValue = emptyList())
        val actionBarDataFlow = AppFile.UserPreferences.cache()
            .mapNotNull { it }
            .distinctUntilChanged()
        val selectedUuidFlow = snapshotFlow { (router.backStack.lastOrNull { it is Screen.UserDetail } as? Screen.UserDetail)?.uuid }
            .distinctUntilChanged()

        val usersActionBarFlow = combine(
            flow = usersFlow,
            flow2 = actionBarDataFlow,
        ) { users, actionBarData ->
            users to actionBarData
        }.mapLatest { (users, actionBarData) ->
            val comparator = userComparator(sortProperty = actionBarData.sortProperty, sortAscending = actionBarData.sortAscending)
            users
                .filter { actionBarData.showDeleted || it.deletedAt == null }
                .filter { it.matchesSearch(search = actionBarData.search, searchableProperties = actionBarData.searchableProperties) }
                .let { if (comparator == null) it else it.sortedWith(comparator = comparator) } to actionBarData
        }

        combine(
            flow = usersActionBarFlow,
            flow2 = selectedUuidFlow
        ) { (users, actionBar), selectedUuid ->
            users
                .map { it.toUserItem(visibilityProperties = actionBar.visibleProperties, selectedUuid = selectedUuid) }
                .toPersistentList()
        }
            .flowOn(context = Dispatcher.Default)
            .observe(id = "filterUsers") { users ->
                updateState { it.copy(users = users) }
            }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Navigates to the detail screen of the selected user.
     *
     * @param action Action carrying the UUID of the selected user.
     * @return The [Job] representing this execution.
     */
    private fun selectUser(action: UserListScreenAction.SelectUser): Job = launch(id = "selectUser") {
        router.navigate(screen = Screen.UserDetail(uuid = action.userUuid), option = Router.NavOption.REPLACE_LAST)
    }

    /**
     * Checks whether this user's searchable properties contain the given [search] text.
     *
     * @param search Search text to look for; an empty or blank value always matches.
     * @param searchableProperties Names of the [Property] values to search within.
     * @return `true` if any of the searchable properties contains [search], ignoring case.
     */
    private fun User.matchesSearch(search: String, searchableProperties: List<String>): Boolean {
        return search.isBlank() || searchableProperties.any { property ->
            when (property) {
                Property.UUID.name -> uuid.toString()
                Property.NAME.name -> name
                Property.MODIFIED_AT.name -> modifiedAt.toString()
                Property.DELETED_AT.name -> deletedAt?.toString()
                else -> null
            }?.contains(other = search, ignoreCase = true) == true
        }
    }

    /**
     * Builds a comparator for the given [sortProperty], reversed when [sortAscending] is `false`.
     *
     * @param sortProperty Name of the [User.Property] to sort by.
     * @param sortAscending Whether the comparator should sort in ascending order.
     * @return Comparator for the given property, or `null` if it doesn't match a known [User.Property].
     */
    private fun userComparator(sortProperty: String, sortAscending: Boolean): Comparator<User>? {
        val comparator = when (sortProperty) {
            Property.UUID.name -> compareBy<User> { it.uuid.toString() }
            Property.MODIFIED_AT.name -> compareBy { it.modifiedAt }
            Property.DELETED_AT.name -> compareBy { it.deletedAt }
            Property.NAME.name -> compareBy { it.name }
            else -> return null
        }
        return if (sortAscending) comparator else comparator.reversed()
    }

    /**
     * Converts this user into a [UserItem], including only the properties named in [visibilityProperties].
     *
     * @param visibilityProperties Names of the [Property] values that should be visible on the item.
     * @param selectedUuid UUID of the selected user.
     * @return User item with hidden properties set to `null`.
     */
    private fun User.toUserItem(visibilityProperties: List<String>, selectedUuid: String?): UserItem = UserItem(
        uuid = uuid.toString(),
        selected = uuid.toString() == selectedUuid,
        deleted = deletedAt != null,
        uuidVisible = Property.UUID.name in visibilityProperties,
        modifiedAt = modifiedAt.takeIf { Property.MODIFIED_AT.name in visibilityProperties }?.let { getLocalDateTime(utc = it.toString()) },
        deletedAt = deletedAt.takeIf { Property.DELETED_AT.name in visibilityProperties }?.let { getLocalDateTime(utc = it.toString()) },
        name = name.takeIf { Property.NAME.name in visibilityProperties },
        avatar = avatar?.let { Image(url = it) }
    )

    companion object {
        private const val TAG = "UserListScreenStore"
    }
}
