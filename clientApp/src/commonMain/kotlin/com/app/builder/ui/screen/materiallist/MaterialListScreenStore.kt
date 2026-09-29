package com.app.builder.ui.screen.materiallist

import kotlin.uuid.Uuid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.now
import com.app.builder.core.security.toUuid
import com.app.builder.core.security.uuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Material
import com.app.builder.domain.Material.Property
import com.app.builder.domain.gateway.material.MaterialUseCases
import com.app.builder.plusOrMinus
import com.app.builder.toEnumOrNull
import com.app.builder.ui.component.bar.ActionBarMode
import com.app.builder.ui.component.list.MaterialItem
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * Store backing the material list, combining materials with the shared action bar state to filter, sort, and select them.
 *
 * @param state Initial material list state.
 * @property router The router used for navigation.
 * @property materialUseCases Use cases used to observe and delete materials.
 */
class MaterialListScreenStore(
    state: MaterialListScreenState,
    private val router: Router,
    private val materialUseCases: MaterialUseCases
): Store<MaterialListScreenState, MaterialListScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: MaterialListScreenState, action: MaterialListScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is MaterialListScreenAction.SelectMaterial -> selectMaterial(state = state, action = action)
            is MaterialListScreenAction.Ok -> ok(state = state, action = action)
        }
    }

    /**
     * Observes materials and keeps the displayed list in sync.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val materialsFlow = materialUseCases.observeMaterials().toStateFlow(initialValue = emptyList())
        val actionBarDataFlow = AppFile.MaterialPreferences.cache()
            .mapNotNull { it }
            .distinctUntilChanged()
        val criteriaFlow = stateFlow
            .map { it.toMaterialListFilterCriteria() }
            .distinctUntilChanged()

        actionBarDataFlow
            .map { it.mode }
            .distinctUntilChanged()
            .observe(id = "syncMode") { newMode ->
                val mode = newMode.toEnumOrNull<ActionBarMode>() ?: return@observe

                if (mode == ActionBarMode.ADD) router.navigate(screen = Screen.MaterialDetail(uuid = uuid().toString()), option = Router.NavOption.REPLACE_LAST)

                updateState { state ->
                    state.copy(
                        mode = mode,
                        selectedUuids = when {
                            state.mode != mode -> persistentListOf()
                            else -> state.selectedUuids
                        }
                    )
                }
            }

        val materialsActionBarFlow = combine(
            flow = materialsFlow,
            flow2 = actionBarDataFlow,
        ) { materials, actionBarData ->
            materials to actionBarData
        }.mapLatest { (materials, actionBarData) ->
            val comparator = materialComparator(sortProperty = actionBarData.sortProperty, sortAscending = actionBarData.sortAscending)
            materials
                .filter { it.matchesSearch(search = actionBarData.search, searchableProperties = actionBarData.searchableProperties) }
                .let { if (comparator == null) it else it.sortedWith(comparator = comparator) } to actionBarData
        }

        combine(
            flow = materialsActionBarFlow,
            flow2 = criteriaFlow
        ) { (materials, actionBar), criteria ->
            materials.toPersistentList() to materials
                .map { it.toMaterialItem(visibilityProperties = actionBar.visibleProperties, selectedUuids = criteria.selectedUuids) }
                .toPersistentList()
        }
            .flowOn(context = Dispatcher.Default)
            .observe(id = "filterMaterials") { (entities, materials) ->
                updateState { it.copy(materials = materials, entities = entities) }
            }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Toggles the selection of the tapped material when in delete mode, otherwise navigates to its detail screen.
     *
     * @param state Current material list state.
     * @param action Action carrying the UUID of the selected material.
     * @return The [Job] representing this execution.
     */
    private fun selectMaterial(state: MaterialListScreenState, action: MaterialListScreenAction.SelectMaterial): Job = launch(id = "selectMaterial") {
        when (state.mode) {
            ActionBarMode.DEFAULT,
            ActionBarMode.SEARCH,
            ActionBarMode.ADD,
            ActionBarMode.EDIT,
            ActionBarMode.DELETE -> router.navigate(screen = Screen.MaterialDetail(uuid = action.materialUuid), option = Router.NavOption.REPLACE_LAST)

            ActionBarMode.BATCH_DELETE -> {
                val selectedUuid = action.materialUuid.toUuid()
                if (selectedUuid != null) {
                    val selectedUuids = state.selectedUuids.plusOrMinus(element = selectedUuid).toPersistentList()
                    updateState { it.copy(selectedUuids = selectedUuids) }
                }
            }
        }
    }

    /**
     * Commits the pending mode once the user confirms it.
     *
     * @param state Current material list state.
     * @param action Action carrying the mode being confirmed.
     * @return The [Job] representing this execution.
     */
    private fun ok(state: MaterialListScreenState, action: MaterialListScreenAction.Ok): Job = launch(id = "ok") {
        when (action.mode) {
            ActionBarMode.DEFAULT,
            ActionBarMode.SEARCH,
            ActionBarMode.ADD,
            ActionBarMode.EDIT,
            ActionBarMode.DELETE -> Unit

            ActionBarMode.BATCH_DELETE -> {
                val now = now()
                state.entities.filter { it.uuid in state.selectedUuids }.forEach {
                    materialUseCases.upsertMaterial(material = it.copy(modifiedAt = now, deletedAt = now))
                }
            }

        }
    }

    /**
     * Extracts the [MaterialListFilterCriteria] this state contributes to the displayed material list.
     *
     * @return The criteria derived from this state.
     */
    private fun MaterialListScreenState.toMaterialListFilterCriteria(): MaterialListFilterCriteria = MaterialListFilterCriteria(
        selectedUuids = selectedUuids
    )

    /**
     * Checks whether this material's searchable properties contain the given [search] text.
     *
     * @param search Search text to look for; an empty or blank value always matches.
     * @param searchableProperties Names of the [Property] values to search within.
     * @return `true` if any of the searchable properties contains [search], ignoring case.
     */
    private fun Material.matchesSearch(search: String, searchableProperties: List<String>): Boolean {
        if (search.isBlank()) return true
        return searchableProperties.any { property ->
            when (property) {
                Property.NAME.name -> name
                Property.CODE.name -> code
                Property.MODIFIED_AT.name -> modifiedAt.toString()
                Property.DELETED_AT.name -> deletedAt?.toString()
                else -> null
            }?.contains(other = search, ignoreCase = true) == true
        }
    }

    /**
     * Builds a comparator for the given [sortProperty], reversed when [sortAscending] is `false`.
     *
     * @param sortProperty Name of the [Material.Property] to sort by.
     * @param sortAscending Whether the comparator should sort in ascending order.
     * @return Comparator for the given property, or `null` if it doesn't match a known [Material.Property].
     */
    private fun materialComparator(sortProperty: String, sortAscending: Boolean): Comparator<Material>? {
        val comparator = when (sortProperty) {
            Property.MODIFIED_AT.name -> compareBy<Material> { it.modifiedAt }
            Property.DELETED_AT.name -> compareBy { it.deletedAt }
            Property.NAME.name -> compareBy { it.name }
            Property.CODE.name -> compareBy { it.code }
            else -> return null
        }
        return if (sortAscending) comparator else comparator.reversed()
    }

    /**
     * Converts this material into a [MaterialItem], including only the properties named in [visibilityProperties].
     *
     * @param visibilityProperties Names of the [Property] values that should be visible on the item.
     * @param selectedUuids UUIDs of the materials the user has picked.
     * @return Material item with hidden properties set to `null`.
     */
    private fun Material.toMaterialItem(visibilityProperties: List<String>, selectedUuids: ImmutableList<Uuid>): MaterialItem = MaterialItem(
        uuid = uuid.toString(),
        selected = uuid in selectedUuids,
        modifiedAt = modifiedAt.takeIf { Property.MODIFIED_AT.name in visibilityProperties }?.toString(),
        deletedAt = deletedAt.takeIf { Property.DELETED_AT.name in visibilityProperties }?.toString(),
        name = name.takeIf { Property.NAME.name in visibilityProperties },
        code = code.takeIf { Property.CODE.name in visibilityProperties }
    )

    companion object {
        private const val TAG = "MaterialListScreenStore"
    }
}
