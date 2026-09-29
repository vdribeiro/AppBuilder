package com.app.builder.ui.screen.materialdetail

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import com.app.builder.core.locale.now
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Material
import com.app.builder.domain.gateway.material.MaterialUseCases
import com.app.builder.toEnumOrNull
import com.app.builder.ui.component.bar.ActionBarMode
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.store.Store

/**
 * Store backing the Material Detail Screen, observing the material and the action bar's edit mode, and applying name/code edits.
 *
 * @param state Initial Material Detail Screen state.
 * @property router The router used for navigation.
 * @property materialUseCases Use cases used to observe the material.
 * @property materialUuid UUID of the material being displayed.
 */
class MaterialDetailScreenStore(
    state: MaterialDetailScreenState,
    private val router: Router,
    private val materialUseCases: MaterialUseCases,
    private val materialUuid: String
): Store<MaterialDetailScreenState, MaterialDetailScreenAction>(initialState = state) {
    init {
        setup()
    }

    /**
     * Observes the material with [materialUuid], creating a placeholder if it doesn't exist yet, and observes the cached action bar state to keep edit mode in sync.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val uuid = materialUuid.toUuid() ?: run {
            Telemetry.info(tag = TAG, message = "Invalid material uuid")
            return@launch
        }

        materialUseCases.observeMaterial(uuid = uuid).observe(id = "material") { material ->
            val editMode = material == null
            val material = material ?: Material(
                uuid = uuid,
                modifiedAt = now(),
                deletedAt = null,
                name = "",
                code = ""
            )
            updateState {
                it.copy(
                    material = material,
                    editMode = editMode
                )
            }
        }

        AppFile.MaterialPreferences.cache()
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

    override fun reducer(state: MaterialDetailScreenState, action: MaterialDetailScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is MaterialDetailScreenAction.ChangeName -> updateState { it.copy(material = it.material?.copy(name = action.name)) }
            is MaterialDetailScreenAction.ChangeCode -> updateState { it.copy(material = it.material?.copy(code = action.code)) }
            is MaterialDetailScreenAction.Ok -> ok(state = state, action = action)
            is MaterialDetailScreenAction.Cancel -> cancel(action = action)
        }
    }

    /**
     * Saves the edited material when the confirmed mode is one that edits it, and does nothing for the modes that do not.
     *
     * @param state Current material detail state, holding the material being edited.
     * @param action Action carrying the mode being confirmed.
     * @return The [Job] representing this execution.
     */
    private fun ok(state: MaterialDetailScreenState, action: MaterialDetailScreenAction.Ok): Job = launch(id = "ok") {
        when (action.mode) {
            ActionBarMode.DEFAULT,
            ActionBarMode.SEARCH,
            ActionBarMode.DELETE,
            ActionBarMode.BATCH_DELETE -> Unit

            ActionBarMode.ADD,
            ActionBarMode.EDIT -> state.material?.let { materialUseCases.upsertMaterial(material = it) }
        }
    }

    private fun cancel(action: MaterialDetailScreenAction.Cancel): Job = launch(id = "cancel") {
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
        private const val TAG = "MaterialDetailScreenStore"
    }
}
