package com.app.builder.ui.navigation.building

import kotlin.math.round
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.locale.now
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.Building
import com.app.builder.domain.gateway.building.BuildingUseCases
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.store.Store

/**
 * Store backing the Building Detail Screen, observing the building while viewing it, and applying, saving and discarding edits while editing it.
 *
 * @param state Initial Building Detail Screen state.
 * @property router The router used for navigation.
 * @property buildingUseCases Use cases used to observe and save the building.
 * @property buildingUuid UUID of the building being displayed.
 * @property new Whether the building is new and not stored yet, starting it in edit mode.
 */
class BuildingDetailScreenStore(
    state: BuildingDetailScreenState,
    private val router: Router,
    private val buildingUseCases: BuildingUseCases,
    private val buildingUuid: String,
    private val new: Boolean = false
): Store<BuildingDetailScreenState, BuildingDetailScreenAction>(initialState = state) {
    init {
        setup()
    }

    /**
     * Starts a [new] building in edit mode, then observes the building with [buildingUuid], ignoring its changes while it's being edited.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val uuid = buildingUuid.toUuid() ?: run {
            Telemetry.info(tag = TAG, message = "Invalid building uuid")
            return@launch
        }

        if (new) updateState { it.showing(building = Building(uuid = uuid)).copy(editing = true, new = true) }

        buildingUseCases.observeBuilding(uuid = uuid).observe(id = "building") { building ->
            updateState { if (it.editing) it else it.showing(building = building) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    override fun reducer(state: BuildingDetailScreenState, action: BuildingDetailScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is BuildingDetailScreenAction.ChangeName -> updateState { it.copy(building = it.building?.copy(name = action.name)) }
            is BuildingDetailScreenAction.ChangeDescription -> updateState { it.copy(building = it.building?.copy(description = action.description)) }
            is BuildingDetailScreenAction.ChangeAddress -> updateState { it.copy(building = it.building?.copy(address = action.address)) }
            is BuildingDetailScreenAction.ChangeLatitude -> updateState { it.copy(latitude = action.latitude) }
            is BuildingDetailScreenAction.ChangeLongitude -> updateState { it.copy(longitude = action.longitude) }
            is BuildingDetailScreenAction.ChangeCoordinates -> updateState {
                it.copy(latitude = action.latitude.rounded().toString(), longitude = action.longitude.rounded().toString())
            }
            BuildingDetailScreenAction.Edit -> updateState { it.copy(editing = true) }
            BuildingDetailScreenAction.Save -> save(state = state)
            BuildingDetailScreenAction.Cancel -> cancel(state = state)
            BuildingDetailScreenAction.Delete -> delete(state = state)
            BuildingDetailScreenAction.Restore -> restore(state = state)
        }
    }

    /**
     * Saves the edited building with the typed coordinates and stops editing it, unless a coordinate is invalid.
     *
     * @param state Building Detail Screen state at the time the save was requested.
     * @return The [Job] representing this execution.
     */
    private fun save(state: BuildingDetailScreenState): Job = launch(id = "save") {
        if (state.latitudeError || state.longitudeError) return@launch
        val building = state.building?.copy(
            modifiedAt = now(),
            latitude = state.latitude.toDoubleOrNull(),
            longitude = state.longitude.toDoubleOrNull()
        ) ?: return@launch
        buildingUseCases.upsertBuilding(building = building)
        updateState { it.showing(building = building).copy(editing = false, new = false) }
    }

    /**
     * Discards the edits, then reloads the stored building and stops editing it, or leaves the screen when the building is new.
     *
     * @param state Building Detail Screen state at the time the cancel was requested.
     * @return The [Job] representing this execution.
     */
    private fun cancel(state: BuildingDetailScreenState): Job = launch(id = "cancel") {
        if (state.new) {
            router.back()
            return@launch
        }
        val building = state.building?.let { buildingUseCases.observeBuilding(uuid = it.uuid).firstOrNull() }
        updateState { it.showing(building = building).copy(editing = false) }
    }

    /**
     * Soft deletes the building by setting its deletion and modification times, then leaves the screen.
     *
     * @param state Building Detail Screen state at the time the delete was requested.
     * @return The [Job] representing this execution.
     */
    private fun delete(state: BuildingDetailScreenState): Job = launch(id = "delete") {
        val now = now()
        val building = state.building?.copy(deletedAt = now, modifiedAt = now)
        building?.let { buildingUseCases.upsertBuilding(building = it) }
        router.back()
    }

    /**
     * Clears the deletion of the building, so it is active again, and keeps viewing it.
     *
     * @param state Building Detail Screen state at the time the restore was requested.
     * @return The [Job] representing this execution.
     */
    private fun restore(state: BuildingDetailScreenState): Job = launch(id = "restore") {
        val building = state.building?.copy(deletedAt = null, modifiedAt = now()) ?: return@launch
        buildingUseCases.upsertBuilding(building = building)
        updateState { it.showing(building = building) }
    }

    /**
     * Shows [building], with its coordinates as text.
     *
     * @param building The building to show.
     * @return This state showing [building].
     */
    private fun BuildingDetailScreenState.showing(building: Building?): BuildingDetailScreenState = copy(
        building = building,
        latitude = building?.latitude?.toString().orEmpty(),
        longitude = building?.longitude?.toString().orEmpty()
    )

    /**
     * Rounds this coordinate to six decimal places, about 10 cm.
     *
     * @return The rounded coordinate.
     */
    private fun Double.rounded(): Double = round(x = this * 1_000_000) / 1_000_000

    companion object {
        private const val TAG = "BuildingDetailScreenStore"
    }
}
