package com.app.builder.ui.navigation.material

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import com.app.builder.Dependency.getUserDependency
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Material
import com.app.builder.domain.Material.Property
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.list.MaterialItem

class MaterialListScreenStoreTest: TestCase() {

    /** A live material, modified in 2020. */
    private val alpha = Material(
        modifiedAt = Instant.parse(input = "2020-01-01T00:00:00Z"),
        name = "Alpha",
        description = "first",
    )

    /** A live material, modified in 2021. */
    private val beta = Material(
        modifiedAt = Instant.parse(input = "2021-06-01T00:00:00Z"),
        name = "Beta",
        description = "second",
    )

    /** A deleted material, modified in 2019 and deleted in 2021, which the list never shows. */
    private val gamma = Material(
        modifiedAt = Instant.parse(input = "2019-01-01T00:00:00Z"),
        deletedAt = Instant.parse(input = "2021-01-01T00:00:00Z"),
        name = "Gamma",
        description = "third",
    )

    /** Every [Property] name, so every property is searchable and visible unless a test narrows it. */
    private val allProperties = Property.entries.map { it.name }

    /**
     * Seeds [alpha], [beta] and [gamma], saves [preferences] as the material preferences, and waits for the store to list [count] materials.
     *
     * @param preferences The action bar data the store filters, sorts and projects with.
     * @param count The number of materials the store is expected to list.
     * @return The listed [MaterialItem]s, in display order.
     */
    private suspend fun listedMaterials(preferences: AppFile.ActionBarData, count: Int): List<MaterialItem> {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.materialUseCases.upsertMaterial(material = alpha)
        authenticatedUseCases.materialUseCases.upsertMaterial(material = beta)
        authenticatedUseCases.materialUseCases.upsertMaterial(material = gamma)
        AppFile.MaterialPreferences.save { preferences }

        val store = MaterialListScreenStore(state = MaterialListScreenState(), router = router, materialUseCases = authenticatedUseCases.materialUseCases)
        return store.stateFlow.first { it.materials.size == count }.materials
    }

    /**
     * Lists the materials matching [search] within [searchableProperties].
     *
     * @param search The search text.
     * @param searchableProperties The property names to search within.
     * @return The titles of the matching materials.
     */
    private suspend fun search(search: String, vararg searchableProperties: Property): List<String?> = listedMaterials(
        preferences = AppFile.ActionBarData(
            search = search,
            searchableProperties = searchableProperties.map { it.name },
            visibleProperties = allProperties,
        ),
        count = 1
    ).map { it.name }

    /**
     * Lists every material sorted by [sortProperty].
     *
     * @param sortProperty The property name to sort by.
     * @param sortAscending Whether to sort in ascending order.
     * @return The titles of the materials, in display order.
     */
    private suspend fun sort(sortProperty: String, sortAscending: Boolean): List<String?> = listedMaterials(
        preferences = AppFile.ActionBarData(
            sortProperty = sortProperty,
            sortAscending = sortAscending,
            visibleProperties = allProperties,
        ),
        count = 2
    ).map { it.name }

    /** Verifies that a search matches the material's name, ignoring case. */
    @Test
    fun searchMatchesName() = runUnitTest {
        assertEquals(expected = listOf("Alpha"), actual = search(search = "ALP", Property.NAME))
    }

    /** Verifies that a search matches the material's description. */
    @Test
    fun searchMatchesDescription() = runUnitTest {
        assertEquals(expected = listOf("Beta"), actual = search(search = "second", Property.DESCRIPTION))
    }

    /** Verifies that a search matches the material's modification time. */
    @Test
    fun searchMatchesModifiedAt() = runUnitTest {
        assertEquals(expected = listOf("Alpha"), actual = search(search = "2020", Property.MODIFIED_AT))
    }

    /** Verifies that materials are sorted by modification time in both directions. */
    @Test
    fun sortByModifiedAt() = runUnitTest {
        assertEquals(expected = listOf("Alpha", "Beta"), actual = sort(sortProperty = Property.MODIFIED_AT.name, sortAscending = true))
        assertEquals(expected = listOf("Beta", "Alpha"), actual = sort(sortProperty = Property.MODIFIED_AT.name, sortAscending = false))
    }

    /** Verifies that materials are sorted by name. */
    @Test
    fun sortByName() = runUnitTest {
        assertEquals(expected = listOf("Beta", "Alpha"), actual = sort(sortProperty = Property.NAME.name, sortAscending = false))
    }

    /** Verifies that materials are sorted by description. */
    @Test
    fun sortByDescription() = runUnitTest {
        assertEquals(expected = listOf("Alpha", "Beta"), actual = sort(sortProperty = Property.DESCRIPTION.name, sortAscending = true))
    }

    /** Verifies that a property the sort does not support lists every material without sorting them. */
    @Test
    fun sortIgnoresUnsupportedProperties() = runUnitTest {
        assertEquals(expected = setOf("Alpha", "Beta"), actual = sort(sortProperty = "UNKNOWN", sortAscending = true).toSet())
    }

    /** Verifies that only the visible properties are projected onto the listed items. */
    @Test
    fun hiddenPropertiesAreLeftOut() = runUnitTest {
        val items = listedMaterials(preferences = AppFile.ActionBarData(visibleProperties = listOf(Property.NAME.name)), count = 2)

        items.forEach { item ->
            assertNull(actual = item.modifiedAt)
            assertNull(actual = item.deletedAt)
            assertNull(actual = item.description)
            assertFalse(actual = item.uuidVisible)
        }
        assertEquals(expected = setOf("Alpha", "Beta"), actual = items.map { it.name }.toSet())
    }

    /** Verifies that deleted materials are not listed. */
    @Test
    fun deletedMaterialsAreLeftOut() = runUnitTest {
        val items = listedMaterials(preferences = AppFile.ActionBarData(visibleProperties = allProperties), count = 2)

        assertEquals(expected = setOf("Alpha", "Beta"), actual = items.map { it.name }.toSet())
    }
}
