package com.app.builder.ui.screen.design

import kotlinx.coroutines.Job
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.AppFile
import com.app.builder.data.storage.StorageFile
import com.app.builder.data.theme.CustomTheme
import com.app.builder.ui.store.Store

/**
 * Store backing the design screen, persisting the theme customized in its showcases.
 *
 * @property storageFile File the theme is persisted to.
 */
class DesignScreenStore(
    private val storageFile: StorageFile<CustomTheme> = AppFile.Theme,
): Store<Unit, DesignScreenAction>(initialState = Unit) {

    override fun reducer(state: Unit, action: DesignScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is DesignScreenAction.SaveTheme -> saveTheme(action = action)
        }
    }

    /**
     * Persists the theme, deleting the file when it holds no customizations.
     * A newer save replaces one still in progress, as each carries the whole theme.
     *
     * @param action The action holding the theme to persist.
     * @return The [Job] representing this execution.
     */
    private fun saveTheme(action: DesignScreenAction.SaveTheme): Job = launch(id = "save_theme") {
        val saved = storageFile.save { action.customTheme.takeUnless { it == CustomTheme() } }
        if (!saved) Telemetry.error(tag = TAG, message = "Unable to save the theme")
    }

    companion object {
        private const val TAG = "DesignScreenStore"
    }
}
