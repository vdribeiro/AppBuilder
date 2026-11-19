package com.app.builder.ui.navigation.translations

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Account
import com.app.builder.domain.Translation
import com.app.builder.domain.Translation.Property
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.gateway.translation.toTranslationSchema
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TranslationsScreenStoreTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(http = true) }
    }

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
            authenticationUseCases = dependency.get().userGraph.useCases.authenticationUseCases,
            locale = flowOf(value = locale)
        )
    }

    /**
     * Seeds [translations], logs a user in and creates the store on that user's use cases, waiting until the English translations are listed.
     *
     * @param account The account of the user to log in.
     * @param credentials The credentials of the user to log in.
     * @return The [TranslationsScreenStore].
     */
    private suspend fun authenticatedStore(account: Account = FakeData.adminAccount, credentials: UserCredentials = FakeData.adminCredentials): TranslationsScreenStore {
        val translationDao = dependency.get()
            .database
            .translationQueries
        translations.forEach { translationDao.upsertTranslation(Translation = it.toTranslationSchema()) }
        AppFile.TranslationPreferences.save { AppFile.ActionBarData(searchableProperties = allProperties, visibleProperties = allProperties) }

        val useCases = dependency.get()
            .getUserDependency(account = account)
            .useCases
        useCases.authenticationUseCases.login(credentials = credentials)

        return TranslationsScreenStore(
            state = TranslationsScreenState(),
            translationUseCases = useCases.translationUseCases,
            authenticationUseCases = useCases.authenticationUseCases,
            locale = flowOf(value = "en")
        ).also { store -> store.listedValues(count = 2) }
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

    /** Verifies that the listed items carry their value as text even when the value property is hidden. */
    @Test
    fun hiddenValueIsKeptAsText() = runUnitTest {
        val store = store(preferences = AppFile.ActionBarData(visibleProperties = listOf(Property.KEY.name)))

        val items = store.stateFlow.first { it.translations.size == 2 }.translations
        assertTrue(actual = items.all { it.value == null })
        assertEquals(expected = setOf("Yes", "No"), actual = items.map { it.text }.toSet())
    }

    /** Verifies that translations are editable for a user with translation write permission, and not for a guest. */
    @Test
    fun editableFollowsWritePermission() = runUnitTest {
        assertFalse(actual = store().also { it.listedValues(count = 2) }.state.editable)

        val store = authenticatedStore()
        assertTrue(actual = store.stateFlow.first { it.editable }.editable)
    }

    /** Verifies that editing a translation opens the editor on its value, and that saving a changed value updates the listed translation and closes the editor. */
    @Test
    fun editAndSaveTranslation() = runUnitTest {
        val store = authenticatedStore()
        store.stateFlow.first { it.editable }

        store.send(action = TranslationsScreenAction.EditTranslation(key = "yes"))
        assertEquals(expected = TranslationEdit(key = "yes", value = "Yes"), actual = store.stateFlow.first { it.edit != null }.edit)

        store.send(action = TranslationsScreenAction.ChangeValue(value = "Yep"))
        assertEquals(expected = TranslationEdit(key = "yes", value = "Yep"), actual = store.stateFlow.first { it.edit?.value == "Yep" }.edit)

        store.send(action = TranslationsScreenAction.SaveTranslation)

        val state = store.stateFlow.first { state -> state.edit == null && state.translations.any { it.text == "Yep" } }
        assertEquals(expected = setOf("Yep", "No"), actual = state.translations.map { it.text }.toSet())
    }

    /** Verifies that dismissing the editor closes it and keeps the listed translation unchanged. */
    @Test
    fun dismissEdit() = runUnitTest {
        val store = authenticatedStore()
        store.stateFlow.first { it.editable }

        store.send(action = TranslationsScreenAction.EditTranslation(key = "yes"))
        store.send(action = TranslationsScreenAction.ChangeValue(value = "Yep"))
        store.stateFlow.first { it.edit?.value == "Yep" }
        store.send(action = TranslationsScreenAction.DismissEdit)

        val state = store.stateFlow.first { it.edit == null }
        assertEquals(expected = setOf("Yes", "No"), actual = state.translations.map { it.text }.toSet())
    }

    /** Verifies that the editor does not open for a user without translation write permission. */
    @Test
    fun editRequiresWritePermission() = runUnitTest {
        // FakeData.account only has TRANSLATION -> READ.
        val store = authenticatedStore(account = FakeData.account, credentials = FakeData.credentials)

        store.send(action = TranslationsScreenAction.EditTranslation(key = "yes"))
        store.send(action = TranslationsScreenAction.SelectLanguage(languageIso = "pt"))

        val state = store.stateFlow.first { it.selectedLanguage == "pt" }
        assertFalse(actual = state.editable)
        assertNull(actual = state.edit)
    }
}
