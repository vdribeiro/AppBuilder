package com.app.builder.ui.screen.buildingdetail

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import com.app.builder.core.locale.now
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Building
import com.app.builder.domain.gateway.building.BuildingUseCases
import com.app.builder.toEnumOrNull
import com.app.builder.ui.component.bar.ActionBarMode
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.store.Store

/**
 * Store backing the Building Detail Screen, observing the building and the action bar's edit mode, and applying name/code edits.
 *
 * @param state Initial Building Detail Screen state.
 * @property router The router used for navigation.
 * @property buildingUseCases Use cases used to observe the building.
 * @property buildingUuid UUID of the building being displayed.
 */
class BuildingDetailScreenStore(
    state: BuildingDetailScreenState,
    private val router: Router,
    private val buildingUseCases: BuildingUseCases,
    private val buildingUuid: String
): Store<BuildingDetailScreenState, BuildingDetailScreenAction>(initialState = state) {
    init {
        setup()
    }

    /**
     * Observes the building with [buildingUuid], creating a placeholder if it doesn't exist yet, and observes the cached action bar state to keep edit mode in sync.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val uuid = buildingUuid.toUuid() ?: run {
            Telemetry.info(tag = TAG, message = "Invalid building uuid")
            return@launch
        }

        buildingUseCases.observeBuilding(uuid = uuid).observe(id = "building") { building ->
            val editMode = building == null
            val building = building ?: Building(
                uuid = uuid,
                modifiedAt = now(),
                deletedAt = null,
                name = "",
                code = ""
            )
            updateState {
                it.copy(
                    building = building,
                    editMode = editMode
                )
            }
        }

        AppFile.BuildingPreferences.cache()
            .mapNotNull { it?.mode }
            .distinctUntilChanged()
            .observe(id = "syncMode") { newMode ->
                val mode = newMode.toEnumOrNull<ActionBarMode>() ?: return@observe
                when (mode) {
                    ActionBarMode.DEFAULT,
                    ActionBarMode.SEARCH,
                    ActionBarMode.DELETE,
                    ActionBarMode.BATCH_DELETE -> updateState { it.copy(editMode = false) }

                    ActionBarMode.ADD,
                    ActionBarMode.EDIT -> updateState { it.copy(editMode = true) }
                }
            }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    override fun reducer(state: BuildingDetailScreenState, action: BuildingDetailScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is BuildingDetailScreenAction.ChangeName -> updateState { it.copy(building = it.building?.copy(name = action.name)) }
            is BuildingDetailScreenAction.ChangeCode -> updateState { it.copy(building = it.building?.copy(code = action.code)) }
            is BuildingDetailScreenAction.Ok -> ok(state = state, action = action)
            is BuildingDetailScreenAction.Cancel -> cancel(action = action)
        }
    }

    /**
     * Saves the edited building when the confirmed mode is one that edits it, and does nothing for the modes that do not.
     *
     * @param state Current building detail state, holding the building being edited.
     * @param action Action carrying the mode being confirmed.
     * @return The [Job] representing this execution.
     */
    private fun ok(state: BuildingDetailScreenState, action: BuildingDetailScreenAction.Ok): Job = launch(id = "ok") {
        when (action.mode) {
            ActionBarMode.DEFAULT,
            ActionBarMode.SEARCH,
            ActionBarMode.DELETE,
            ActionBarMode.BATCH_DELETE -> Unit

            ActionBarMode.ADD,
            ActionBarMode.EDIT -> state.building?.let { buildingUseCases.upsertBuilding(building = it) }
        }
    }

    private fun cancel(action: BuildingDetailScreenAction.Cancel): Job = launch(id = "cancel") {
        when (action.mode) {
            ActionBarMode.DEFAULT,
            ActionBarMode.SEARCH,
            ActionBarMode.EDIT,
            ActionBarMode.DELETE,
            ActionBarMode.BATCH_DELETE -> Unit

            ActionBarMode.ADD -> router.back()
        }

        updateState { it.copy(editMode = false) }
    }

    companion object {
        private const val TAG = "BuildingDetailScreenStore"
    }
}
