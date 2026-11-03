package com.app.builder.ui.screen.design

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.app.builder.data.storage.AppFile
import com.app.builder.data.theme.CustomTheme
import com.app.builder.data.theme.ThemeMode
import com.app.builder.test.TestCase

class DesignScreenStoreTest: TestCase() {

    /** Verifies that saving a customized theme persists it to the storage file. */
    @Test
    fun saveTheme() = runUnitTest {
        val store = DesignScreenStore(storageFile = AppFile.Theme)
        val customTheme = CustomTheme(mode = ThemeMode.DARK, light = mapOf("PRIMARY" to "#123456"), shapes = mapOf("SMALL" to 2f))

        store.send(action = DesignScreenAction.SaveTheme(customTheme = customTheme))
        testScheduler.advanceUntilIdle()

        assertEquals(expected = customTheme, actual = AppFile.Theme.load())
    }

    /** Verifies that saving the default theme deletes the storage file. */
    @Test
    fun saveDefaultThemeDeletesFile() = runUnitTest {
        val store = DesignScreenStore(storageFile = AppFile.Theme)

        store.send(action = DesignScreenAction.SaveTheme(customTheme = CustomTheme(mode = ThemeMode.LIGHT)))
        testScheduler.advanceUntilIdle()
        store.send(action = DesignScreenAction.SaveTheme(customTheme = CustomTheme()))
        testScheduler.advanceUntilIdle()

        assertNull(actual = AppFile.Theme.load())
    }
}
