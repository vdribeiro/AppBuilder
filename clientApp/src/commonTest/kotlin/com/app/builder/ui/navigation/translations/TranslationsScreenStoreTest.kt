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

    /** Verifies that translations are editable for a user with translation write permission, and not for a guest. */
    @Test
    fun editableFollowsWritePermission() = runUnitTest {
        assertFalse(actual = store().also { it.listedValues(count = 2) }.state.editable)

        val store = authenticatedStore()
        assertTrue(actual = store.stateFlow.first { it.editable }.editable)
    }

    /** Verifies that updating a value shows it on the listed translation as pending at once, then stores it and clears the pending mark. */
    @Test
    fun updateValue() = runUnitTest {
        val store = authenticatedStore()
        store.stateFlow.first { it.editable }

        store.send(action = TranslationsScreenAction.UpdateValue(key = "yes", value = "Yep"))

        val pending = store.state.translations.single { it.id == "yes" }
        assertEquals(expected = "Yep", actual = pending.value)
        assertTrue(actual = pending.pending)

        val state = store.stateFlow.first { state -> state.translations.none { it.pending } }
        assertEquals(expected = setOf("Yep", "No"), actual = state.translations.map { it.value }.toSet())
        assertEquals(expected = setOf("Yep", "No"), actual = storedValues(languageIso = "en"))
    }

    /** Verifies that values updated in quick succession on the same translation store only the last one. */
    @Test
    fun updateValueStoresLastValue() = runUnitTest {
        val store = authenticatedStore()
        store.stateFlow.first { it.editable }

        store.send(action = TranslationsScreenAction.UpdateValue(key = "yes", value = "Yep"))
        store.send(action = TranslationsScreenAction.UpdateValue(key = "yes", value = "Yeah"))

        assertEquals(expected = setOf("Yes", "No"), actual = storedValues(languageIso = "en"))

        store.stateFlow.first { state -> state.translations.none { it.pending } }
        assertEquals(expected = setOf("Yeah", "No"), actual = storedValues(languageIso = "en"))
    }

    /** Verifies that a value updated before selecting another language is stored in the language it was updated in. */
    @Test
    fun updateValueKeepsItsLanguage() = runUnitTest {
        val store = authenticatedStore()
        store.stateFlow.first { it.editable }

        store.send(action = TranslationsScreenAction.UpdateValue(key = "yes", value = "Yep"))
        store.send(action = TranslationsScreenAction.SelectLanguage(languageIso = "pt"))

        val state = store.stateFlow.first { state -> state.selectedLanguage == "pt" && state.translations.map { it.value } == listOf("Sim") }
        assertFalse(actual = state.translations.single().pending)

        testScheduler.advanceUntilIdle()
        assertEquals(expected = setOf("Yep", "No"), actual = storedValues(languageIso = "en"))
        assertEquals(expected = setOf("Sim"), actual = storedValues(languageIso = "pt"))
    }

    /** Verifies that updating a value does nothing for a user without translation write permission. */
    @Test
    fun updateValueRequiresWritePermission() = runUnitTest {
        // FakeData.account only has TRANSLATION -> READ.
        val store = authenticatedStore(account = FakeData.account, credentials = FakeData.credentials)

        store.send(action = TranslationsScreenAction.UpdateValue(key = "yes", value = "Yep"))

        assertFalse(actual = store.state.editable)
        assertEquals(expected = setOf("Yes", "No"), actual = store.state.translations.map { it.value }.toSet())
        assertTrue(actual = store.state.translations.none { it.pending })

        testScheduler.advanceUntilIdle()
        assertEquals(expected = setOf("Yes", "No"), actual = storedValues(languageIso = "en"))
    }

    /**
     * Reads the stored values of a language.
     *
     * @param languageIso The ISO code of the language.
     * @return The stored values.
     */
    private suspend fun storedValues(languageIso: String): Set<String> = dependency.get()
        .userGraph
        .useCases
        .translationUseCases
        .observeTranslations(languageIso = languageIso)
        .first()
        .map { it.value }
        .toSet()
}
