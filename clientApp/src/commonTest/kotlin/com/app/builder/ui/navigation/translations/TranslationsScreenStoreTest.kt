package com.app.builder.ui.navigation.translations

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Translation
import com.app.builder.domain.Translation.Property
import com.app.builder.domain.gateway.translation.toTranslationSchema
import com.app.builder.test.TestCase

class TranslationsScreenStoreTest: TestCase() {

    /** Translations stored in English and Portuguese. */
    private val translations = listOf(
        Translation(languageIso = "en", key = "yes", value = "Yes"),
        Translation(languageIso = "en", key = "no", value = "No"),
        Translation(languageIso = "pt", key = "yes", value = "Sim"),
    )

    /** Every [Property] name, so every property is searchable and visible unless a test narrows it. */
    private val allProperties = Property.entries.map { it.name }

    /**
     * Seeds [translations], saves [preferences] as the translation preferences and creates the store.
     *
     * @param preferences The action bar data the store filters, sorts and projects with.
     * @param locale The device language.
     * @return The [TranslationsScreenStore].
     */
    private suspend fun store(
        preferences: AppFile.ActionBarData = AppFile.ActionBarData(searchableProperties = allProperties, visibleProperties = allProperties),
        locale: String = "en"
    ): TranslationsScreenStore {
        val translationDao = dependency.get()
            .database
            .translationQueries
        translations.forEach { translationDao.upsertTranslation(Translation = it.toTranslationSchema()) }
        AppFile.TranslationPreferences.save { preferences }

        return TranslationsScreenStore(
            state = TranslationsScreenState(),
            translationUseCases = dependency.get().userGraph.useCases.translationUseCases,
            locale = flowOf(value = locale)
        )
    }

    /**
     * Waits for the store to list [count] translations.
     *
     * @param count The number of translations the store is expected to list.
     * @return The listed values, in display order.
     */
    private suspend fun TranslationsScreenStore.listedValues(count: Int): List<String?> =
        stateFlow.first { it.translations.size == count }.translations.map { it.value }

    /** Verifies that every language holding translations is listed, and the device language is selected. */
    @Test
    fun selectsDeviceLanguage() = runUnitTest {
        val store = store(locale = "pt")

        val state = store.stateFlow.first { it.languages.size == 2 && it.translations.size == 1 }
        assertEquals(expected = listOf("en", "pt"), actual = state.languages)
        assertEquals(expected = "pt", actual = state.selectedLanguage)
        assertEquals(expected = listOf("Sim"), actual = state.translations.map { it.value })
    }

    /** Verifies that the default language is selected when the device language holds no translations. */
    @Test
    fun fallsBackToDefaultLanguage() = runUnitTest {
        val store = store(locale = "fr")

        assertEquals(expected = "en", actual = store.stateFlow.first { it.selectedLanguage != null }.selectedLanguage)
    }

    /** Verifies that selecting a language lists its translations. */
    @Test
    fun selectLanguage() = runUnitTest {
        val store = store()
        store.listedValues(count = 2)

        store.send(action = TranslationsScreenAction.SelectLanguage(languageIso = "pt"))

        assertEquals(expected = listOf("Sim"), actual = store.listedValues(count = 1))
    }

    /** Verifies that a search matches the translation's key, ignoring case. */
    @Test
    fun searchMatchesKey() = runUnitTest {
        val store = store(preferences = AppFile.ActionBarData(search = "YE", searchableProperties = listOf(Property.KEY.name), visibleProperties = allProperties))

        assertEquals(expected = listOf("Yes"), actual = store.listedValues(count = 1))
    }

    /** Verifies that a search matches the translation's value, and only within the searchable properties. */
    @Test
    fun searchMatchesValue() = runUnitTest {
        val store = store(preferences = AppFile.ActionBarData(search = "no", searchableProperties = listOf(Property.VALUE.name), visibleProperties = allProperties))

        assertEquals(expected = listOf("No"), actual = store.listedValues(count = 1))
    }

    /** Verifies that translations are sorted by value in both directions. */
    @Test
    fun sortByValue() = runUnitTest {
        val ascending = store(preferences = AppFile.ActionBarData(sortProperty = Property.VALUE.name, sortAscending = true, visibleProperties = allProperties))
        assertEquals(expected = listOf("No", "Yes"), actual = ascending.listedValues(count = 2))

        val descending = store(preferences = AppFile.ActionBarData(sortProperty = Property.VALUE.name, sortAscending = false, visibleProperties = allProperties))
        assertEquals(expected = listOf("Yes", "No"), actual = descending.listedValues(count = 2))
    }

    /** Verifies that only the visible properties are projected onto the listed items. */
    @Test
    fun hiddenPropertiesAreLeftOut() = runUnitTest {
        val store = store(preferences = AppFile.ActionBarData(visibleProperties = listOf(Property.VALUE.name)))

        store.stateFlow.first { it.translations.size == 2 }.translations.forEach { item ->
            assertNull(actual = item.key)
            assertEquals(expected = item.id == "yes", actual = item.value == "Yes")
        }
    }
}
