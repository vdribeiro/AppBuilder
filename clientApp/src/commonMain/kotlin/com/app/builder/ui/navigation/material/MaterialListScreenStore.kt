package com.app.builder.ui.navigation.material

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
import com.app.builder.domain.gateway.material.MaterialUseCases
import com.app.builder.ui.component.material.filtered
import com.app.builder.ui.component.material.toMaterialItem
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
            is MaterialListScreenAction.SelectMaterial -> selectMaterial(action = action)
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
        val selectedUuidFlow = snapshotFlow { (router.backStack.lastOrNull { it is Screen.MaterialDetail } as? Screen.MaterialDetail)?.uuid }
            .distinctUntilChanged()

        val materialsActionBarFlow = combine(
            flow = materialsFlow,
            flow2 = actionBarDataFlow,
        ) { materials, actionBarData ->
            materials to actionBarData
        }.mapLatest { (materials, actionBarData) ->
            materials.filtered(actionBarData = actionBarData) to actionBarData
        }

        combine(
            flow = materialsActionBarFlow,
            flow2 = selectedUuidFlow
        ) { (materials, actionBar), selectedUuid ->
            materials
                .map { it.toMaterialItem(visibilityProperties = actionBar.visibleProperties, selectedUuid = selectedUuid) }
                .toPersistentList()
        }
            .flowOn(context = Dispatcher.Default)
            .observe(id = "filterMaterials") { materials ->
                updateState { it.copy(materials = materials) }
            }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Navigates to the detail screen of the selected material.
     *
     * @param action Action carrying the UUID of the selected material.
     * @return The [Job] representing this execution.
     */
    private fun selectMaterial(action: MaterialListScreenAction.SelectMaterial): Job = launch(id = "selectMaterial") {
        router.navigate(screen = Screen.MaterialDetail(uuid = action.materialUuid), option = Router.NavOption.REPLACE_LAST)
    }

    companion object {
        private const val TAG = "MaterialListScreenStore"
    }
}