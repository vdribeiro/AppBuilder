package com.app.builder.ui.screen.registrylist

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
import com.app.builder.domain.Registry
import com.app.builder.domain.Registry.Property
import com.app.builder.domain.gateway.registry.RegistryUseCases
import com.app.builder.ui.component.list.RegistryItem
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * Store backing the registry list, combining registries with the shared action bar state to filter, sort, and select them.
 *
 * @param state Initial registry list state.
 * @property router The router used for navigation.
 * @property registryUseCases Use cases used to observe registries.
 */
class RegistryListScreenStore(
    state: RegistryListScreenState,
    private val router: Router,
    private val registryUseCases: RegistryUseCases
): Store<RegistryListScreenState, RegistryListScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: RegistryListScreenState, action: RegistryListScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is RegistryListScreenAction.SelectRegistry -> selectRegistry(action = action)
        }
    }

    /**
     * Observes the registries that are not deleted and keeps the displayed list in sync, selecting the registry whose detail screen is in the backstack.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val registriesFlow = registryUseCases.observeRegistries().toStateFlow(initialValue = emptyList())
        val actionBarDataFlow = AppFile.RegistryPreferences.cache()
            .mapNotNull { it }
            .distinctUntilChanged()
        val selectedUuidFlow = snapshotFlow { (router.backStack.lastOrNull { it is Screen.RegistryDetail } as? Screen.RegistryDetail)?.uuid }
            .distinctUntilChanged()

        val registriesActionBarFlow = combine(
            flow = registriesFlow,
            flow2 = actionBarDataFlow,
        ) { registries, actionBarData ->
            registries to actionBarData
        }.mapLatest { (registries, actionBarData) ->
            val comparator = registryComparator(sortProperty = actionBarData.sortProperty, sortAscending = actionBarData.sortAscending)
            registries
                .filter { it.deletedAt == null }
                .filter { it.matchesSearch(search = actionBarData.search, searchableProperties = actionBarData.searchableProperties) }
                .let { if (comparator == null) it else it.sortedWith(comparator = comparator) } to actionBarData
        }

        combine(
            flow = registriesActionBarFlow,
            flow2 = selectedUuidFlow
        ) { (registries, actionBar), selectedUuid ->
            registries
                .map { it.toRegistryItem(visibilityProperties = actionBar.visibleProperties, selectedUuid = selectedUuid) }
                .toPersistentList()
        }
            .flowOn(context = Dispatcher.Default)
            .observe(id = "filterRegistries") { registries ->
                updateState { it.copy(registries = registries) }
            }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Navigates to the detail screen of the selected registry.
     *
     * @param action Action carrying the request UUID of the selected registry.
     * @return The [Job] representing this execution.
     */
    private fun selectRegistry(action: RegistryListScreenAction.SelectRegistry): Job = launch(id = "selectRegistry") {
        router.navigate(screen = Screen.RegistryDetail(uuid = action.registryUuid), option = Router.NavOption.REPLACE_LAST)
    }

    /**
     * Gets the displayed text of the given [property].
     *
     * @param property Name of the [Property] to read.
     * @return The property's text, or `null` if it's unset or doesn't match a known [Property].
     */
    private fun Registry.text(property: String): String? = when (property) {
        Property.REQUEST_TYPE.name -> requestType
        Property.REQUEST_UTC.name -> requestUtc.toString()
        Property.REQUEST_SENT_UTC.name -> requestSentUtc.toString()
        Property.DELETED_AT.name -> deletedAt?.toString()
        Property.USER_UUID.name -> userUuid.toString()
        Property.APP_VERSION.name -> appVersion
        Property.OS.name -> os.toString()
        Property.OS_VERSION.name -> osVersion
        Property.BRAND.name -> brand
        Property.MODEL.name -> model
        Property.DEVICE_UUID.name -> deviceUuid.toString()
        Property.ENTITY_UUID.name -> entityUuid?.toString()
        Property.ENTITY_TYPE.name -> entityType?.name
        Property.PAYLOAD.name -> payload
        else -> null
    }

    /**
     * Checks whether this registry's searchable properties contain the given [search] text.
     *
     * @param search Search text to look for; an empty or blank value always matches.
     * @param searchableProperties Names of the [Property] values to search within.
     * @return `true` if any of the searchable properties contains [search], ignoring case.
     */
    private fun Registry.matchesSearch(search: String, searchableProperties: List<String>): Boolean {
        return search.isBlank() || searchableProperties.any { property ->
            text(property = property)?.contains(other = search, ignoreCase = true) == true
        }
    }

    /**
     * Builds a comparator for the given [sortProperty], reversed when [sortAscending] is `false`.
     *
     * @param sortProperty Name of the [Registry.Property] to sort by.
     * @param sortAscending Whether the comparator should sort in ascending order.
     * @return Comparator for the given property, or `null` if it doesn't match a known [Registry.Property].
     */
    private fun registryComparator(sortProperty: String, sortAscending: Boolean): Comparator<Registry>? {
        val comparator = when (sortProperty) {
            Property.REQUEST_UTC.name -> compareBy<Registry> { it.requestUtc }
            Property.REQUEST_SENT_UTC.name -> compareBy { it.requestSentUtc }
            Property.DELETED_AT.name -> compareBy { it.deletedAt }
            in Property.entries.map { it.name } -> compareBy { it.text(property = sortProperty) }
            else -> return null
        }
        return if (sortAscending) comparator else comparator.reversed()
    }

    /**
     * Converts this registry into a [RegistryItem], including only the properties named in [visibilityProperties].
     *
     * @param visibilityProperties Names of the [Property] values that should be visible on the item.
     * @param selectedUuid Request UUID of the selected registry.
     * @return Registry item with hidden properties set to `null`.
     */
    private fun Registry.toRegistryItem(visibilityProperties: List<String>, selectedUuid: String?): RegistryItem {
        fun visible(property: Property): String? = text(property = property.name).takeIf { property.name in visibilityProperties }
        return RegistryItem(
            uuid = requestUuid.toString(),
            selected = requestUuid.toString() == selectedUuid,
            requestType = visible(property = Property.REQUEST_TYPE),
            requestUtc = visible(property = Property.REQUEST_UTC),
            requestSentUtc = visible(property = Property.REQUEST_SENT_UTC),
            deletedAt = visible(property = Property.DELETED_AT),
            userUuid = visible(property = Property.USER_UUID),
            appVersion = visible(property = Property.APP_VERSION),
            os = visible(property = Property.OS),
            osVersion = visible(property = Property.OS_VERSION),
            brand = visible(property = Property.BRAND),
            model = visible(property = Property.MODEL),
            deviceUuid = visible(property = Property.DEVICE_UUID),
            entityUuid = visible(property = Property.ENTITY_UUID),
            entityType = visible(property = Property.ENTITY_TYPE),
            payload = visible(property = Property.PAYLOAD),
        )
    }

    companion object {
        private const val TAG = "RegistryListScreenStore"
    }
}
