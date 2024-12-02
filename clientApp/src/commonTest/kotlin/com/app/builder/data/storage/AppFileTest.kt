package com.app.builder.data.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.test.TestCase

class AppFileTest: TestCase() {

    /** Verifies that each [AppFile] variant resolves to its expected storage path. */
    @Test
    fun appFilePaths() = runUnitTest {
        assertEquals(expected = "preferences", actual = AppFile.Preferences.path)
        assertEquals(expected = "sync", actual = AppFile.Sync.path)
        assertEquals(expected = "client_flags", actual = AppFile.ClientFeatureFlags.path)
        assertEquals(expected = "client_configs", actual = AppFile.ClientRemoteConfigs.path)
        assertEquals(expected = "server_flags", actual = AppFile.ServerFeatureFlags.path)
        assertEquals(expected = "server_configs", actual = AppFile.ServerRemoteConfigs.path)
        assertEquals(expected = "task_preferences", actual = AppFile.TaskPreferences.path)

        assertEquals(expected = 7, actual = AppFile.all.size)
    }
}
