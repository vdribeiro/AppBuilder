package com.app.builder.ui.navigation.timepunch

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import androidx.compose.runtime.snapshotFlow
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.TimePunch
import com.app.builder.domain.TimePunch.Property
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.timepunch.TimePunchUseCases
import com.app.builder.domain.gateway.user.UserUseCases
import com.app.builder.ui.component.list.TimePunchItem
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * Store backing the time punch list, combining time punches with their users and the shared action bar state to filter, sort, and select them.
 *
 * @param state Initial time punch list state.
 * @property router The router used for navigation.
 * @property timePunchUseCases Use cases used to observe and create time punches.
 * @property userUseCases Use cases used to observe the users that punched the clock.
 * @property authenticationUseCases Use cases used to observe the current user.
 */
class TimePunchListScreenStore(
    state: TimePunchListScreenState,
    private val router: Router,
    private val timePunchUseCases: TimePunchUseCases,
    private val userUseCases: UserUseCases,
    private val authenticationUseCases: AuthenticationUseCases
): Store<TimePunchListScreenState, TimePunchListScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: TimePunchListScreenState, action: TimePunchListScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is TimePunchListScreenAction.SelectTimePunch -> selectTimePunch(action = action)
        }
    }

    /**
     * Observes the current user and the time punches, and keeps the displayed list in sync.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val currentUserFlow = authenticationUseCases.observeCurrentUser().toStateFlow(initialValue = null)
        currentUserFlow.observe(id = "currentUser") { user ->
            updateState { it.copy(userUuid = user?.uuid) }
        }

        val timePunchesFlow = timePunchUseCases.observeTimePunches().toStateFlow(initialValue = emptyList())
        val usersFlow = userUseCases.observeUsers().toStateFlow(initialValue = emptyList())
        val actionBarDataFlow = AppFile.TimePunchPreferences.cache()
            .mapNotNull { it }
            .distinctUntilChanged()
        val selectedUuidFlow = snapshotFlow { (router.backStack.lastOrNull { it is Screen.TimePunchDetail } as? Screen.TimePunchDetail)?.uuid }
            .distinctUntilChanged()

        val namedTimePunchesFlow = combine(
            flow = timePunchesFlow,
            flow2 = usersFlow,
            flow3 = currentUserFlow
        ) { timePunches, users, currentUser ->
            val names = (users + listOfNotNull(currentUser)).associate { it.uuid to it.name }
            timePunches.map { it to names[it.userUuid] }
        }

        val timePunchesActionBarFlow = combine(
            flow = namedTimePunchesFlow,
            flow2 = actionBarDataFlow,
        ) { timePunches, actionBarData ->
            timePunches to actionBarData
        }.mapLatest { (timePunches, actionBarData) ->
            val comparator = timePunchComparator(sortProperty = actionBarData.sortProperty, sortAscending = actionBarData.sortAscending)
            timePunches
                .filter { (timePunch, _) -> actionBarData.showDeleted || timePunch.deletedAt == null }
                .filter { (timePunch, userName) -> timePunch.matchesSearch(userName = userName, search = actionBarData.search, searchableProperties = actionBarData.searchableProperties) }
                .let { if (comparator == null) it else it.sortedWith(comparator = comparator) } to actionBarData
        }

        combine(
            flow = timePunchesActionBarFlow,
            flow2 = selectedUuidFlow
        ) { (timePunches, actionBar), selectedUuid ->
            timePunches
                .map { (timePunch, userName) -> timePunch.toTimePunchItem(userName = userName, visibilityProperties = actionBar.visibleProperties, selectedUuid = selectedUuid) }
                .toPersistentList()
        }
            .flowOn(context = Dispatcher.Default)
            .observe(id = "filterTimePunches") { timePunches ->
                updateState { it.copy(timePunches = timePunches) }
            }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Navigates to the detail screen of the selected time punch.
     *
     * @param action Action carrying the UUID of the selected time punch.
     * @return The [Job] representing this execution.
     */
    private fun selectTimePunch(action: TimePunchListScreenAction.SelectTimePunch): Job = launch(id = "selectTimePunch") {
        router.navigate(screen = Screen.TimePunchDetail(uuid = action.timePunchUuid), option = Router.NavOption.REPLACE_LAST)
    }

    /**
     * Checks whether this time punch's searchable properties contain the given [search] text.
     *
     * @param userName Name of the user that punched the clock, or `null` when it is unknown.
     * @param search Search text to look for; an empty or blank value always matches.
     * @param searchableProperties Names of the [Property] values to search within.
     * @return `true` if any of the searchable properties contains [search], ignoring case.
     */
    private fun TimePunch.matchesSearch(userName: String?, search: String, searchableProperties: List<String>): Boolean {
        return search.isBlank() || searchableProperties.any { property ->
            when (property) {
                Property.UUID.name -> uuid.toString()
                Property.USER_UUID.name -> userUuid.toString()
                Property.USER_NAME.name -> userName
                Property.TIME.name -> time.toString()
                Property.SOURCE.name -> source.name
                Property.MODIFIED_AT.name -> modifiedAt.toString()
                Property.DELETED_AT.name -> deletedAt?.toString()
                else -> null
            }?.contains(other = search, ignoreCase = true) == true
        }
    }

    /**
     * Builds a comparator for the given [sortProperty], reversed when [sortAscending] is `false`.
     *
     * @param sortProperty Name of the [TimePunch.Property] to sort by.
     * @param sortAscending Whether the comparator should sort in ascending order.
     * @return Comparator for the given property, or `null` if it doesn't match a known [TimePunch.Property].
     */
    private fun timePunchComparator(sortProperty: String, sortAscending: Boolean): Comparator<Pair<TimePunch, String?>>? {
        val comparator = when (sortProperty) {
            Property.UUID.name -> compareBy<Pair<TimePunch, String?>> { (timePunch, _) -> timePunch.uuid.toString() }
            Property.MODIFIED_AT.name -> compareBy { (timePunch, _) -> timePunch.modifiedAt }
            Property.DELETED_AT.name -> compareBy { (timePunch, _) -> timePunch.deletedAt }
            Property.USER_UUID.name -> compareBy { (timePunch, _) -> timePunch.userUuid.toString() }
            Property.USER_NAME.name -> compareBy { (_, userName) -> userName }
            Property.TIME.name -> compareBy { (timePunch, _) -> timePunch.time }
            Property.SOURCE.name -> compareBy { (timePunch, _) -> timePunch.source }
            else -> return null
        }
        return if (sortAscending) comparator else comparator.reversed()
    }

    /**
     * Converts this time punch into a [TimePunchItem], including only the properties named in [visibilityProperties].
     *
     * @param userName Name of the user that punched the clock, or `null` when it is unknown.
     * @param visibilityProperties Names of the [Property] values that should be visible on the item.
     * @param selectedUuid UUID of the selected time punch.
     * @return Time punch item with hidden properties set to `null`.
     */
    private fun TimePunch.toTimePunchItem(userName: String?, visibilityProperties: List<String>, selectedUuid: String?): TimePunchItem = TimePunchItem(
        uuid = uuid.toString(),
        selected = uuid.toString() == selectedUuid,
        deleted = deletedAt != null,
        uuidVisible = Property.UUID.name in visibilityProperties,
        modifiedAt = modifiedAt.takeIf { Property.MODIFIED_AT.name in visibilityProperties }?.toString(),
        deletedAt = deletedAt.takeIf { Property.DELETED_AT.name in visibilityProperties }?.toString(),
        time = time.takeIf { Property.TIME.name in visibilityProperties }?.toString(),
        userUuid = userUuid.takeIf { Property.USER_UUID.name in visibilityProperties }?.toString(),
        userName = userName.takeIf { Property.USER_NAME.name in visibilityProperties },
        nfc = (source == TimePunch.Source.NFC).takeIf { Property.SOURCE.name in visibilityProperties }
    )

    companion object {
        private const val TAG = "TimePunchListScreenStore"
    }
}
