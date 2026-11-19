package com.app.builder.ui.navigation.material

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.first
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.security.uuid
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Material
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.test.count
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen

class MaterialListScreenTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(http = true, scheduler = true) }
    }

    /** Verifies that the material list displays a seeded material and that tapping its card navigates to its detail screen. */
    @Test
    fun materialScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.materialUseCases.upsertMaterial(material = FakeData.material)

        // The list store only emits once MaterialPreferences holds data, and it is the filters store that seeds that file, so it has to be wired up as the provider does.
        val properties = Material.Property.entries.map { it.name }.toPersistentList()
        val defaults = AppFile.ActionBarData(
            search = "",
            sortProperty = Material.Property.MODIFIED_AT.name,
            sortAscending = false,
            visibleProperties = properties,
            searchableProperties = properties,
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val filtersStore = FiltersStore(
            state = FiltersState(visibleProperties = properties, searchableProperties = properties),
            storageFile = AppFile.MaterialPreferences,
            defaults = defaults
        )
        val materialListScreenStore = MaterialListScreenStore(state = MaterialListScreenState(), router = router, materialUseCases = authenticatedUseCases.materialUseCases)

        setUI {
            MaterialListScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                store = materialListScreenStore
            )
        }

        assertTrue(actual = router.backStack.isEmpty())
        waitUntil { materialListScreenStore.state.materials.size == 1 }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()

        onNodeWithTag(testTag = "material_list").assertIsDisplayed()
        onNodeWithTag(testTag = "material_list").count(count = 1)

        onNodeWithTag(testTag = "material_row_0").assertIsDisplayed().performClick()
        val materialDetailScreen = Screen.MaterialDetail(uuid = materialListScreenStore.state.materials.first().uuid)
        assertEquals(expected = listOf(materialDetailScreen), actual = router.backStack.toList())
        waitUntil { materialListScreenStore.state.materials.first().selected }
    }

    /** Verifies that the selected material follows the material detail screen in the backstack, however it got there. */
    @Test
    fun selectionFollowsDetailScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        val otherMaterial = FakeData.material.copy(uuid = uuid(), name = "Other")
        authenticatedUseCases.materialUseCases.upsertMaterial(material = FakeData.material)
        authenticatedUseCases.materialUseCases.upsertMaterial(material = otherMaterial)

        // The list store only emits once MaterialPreferences holds data, and it is the filters store that seeds that file, so it has to be wired up as the provider does.
        val properties = Material.Property.entries.map { it.name }.toPersistentList()
        val defaults = AppFile.ActionBarData(
            search = "",
            sortProperty = Material.Property.MODIFIED_AT.name,
            sortAscending = false,
            visibleProperties = properties,
            searchableProperties = properties,
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val filtersStore = FiltersStore(
            state = FiltersState(visibleProperties = properties, searchableProperties = properties),
            storageFile = AppFile.MaterialPreferences,
            defaults = defaults
        )
        val materialListScreenStore = MaterialListScreenStore(state = MaterialListScreenState(), router = router, materialUseCases = authenticatedUseCases.materialUseCases)

        setUI {
            MaterialListScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                store = materialListScreenStore
            )
        }
        waitUntil { materialListScreenStore.state.materials.size == 2 }
        fun selectedUuids() = materialListScreenStore.state.materials.filter { it.selected }.map { it.uuid }

        router.navigate(screen = Screen.MaterialList)
        router.navigate(screen = Screen.MaterialDetail(uuid = otherMaterial.uuid.toString()), option = Router.NavOption.REPLACE_LAST)
        waitForIdle()
        waitUntil { selectedUuids() == listOf(otherMaterial.uuid.toString()) }

        router.navigate(screen = Screen.MaterialDetail(uuid = FakeData.material.uuid.toString()), option = Router.NavOption.REPLACE_LAST)
        waitForIdle()
        waitUntil { selectedUuids() == listOf(FakeData.material.uuid.toString()) }

        router.navigate(screen = Screen.MaterialDetail(uuid = uuid().toString(), new = true), option = Router.NavOption.REPLACE_LAST)
        waitForIdle()
        waitUntil { selectedUuids().isEmpty() }

        router.navigate(screen = Screen.MaterialDetail(uuid = otherMaterial.uuid.toString()), option = Router.NavOption.REPLACE_LAST)
        waitForIdle()
        waitUntil { selectedUuids() == listOf(otherMaterial.uuid.toString()) }
        router.back()
        waitForIdle()
        waitUntil { selectedUuids().isEmpty() }
    }

    /** Verifies that the add action opens the detail screen of a new material without storing it. */
    @Test
    fun addMaterial() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val properties = Material.Property.entries.map { it.name }.toPersistentList()
        val defaults = AppFile.ActionBarData(
            search = "",
            sortProperty = Material.Property.MODIFIED_AT.name,
            sortAscending = false,
            visibleProperties = properties,
            searchableProperties = properties,
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val filtersStore = FiltersStore(
            state = FiltersState(visibleProperties = properties, searchableProperties = properties),
            storageFile = AppFile.MaterialPreferences,
            defaults = defaults
        )
        val materialListScreenStore = MaterialListScreenStore(state = MaterialListScreenState(), router = router, materialUseCases = authenticatedUseCases.materialUseCases)

        setUI {
            MaterialListScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                store = materialListScreenStore
            )
        }

        onNodeWithTag(testTag = "action_add").assertIsDisplayed().performClick()
        waitUntil { router.backStack.isNotEmpty() }

        val screen = assertIs<Screen.MaterialDetail>(value = router.backStack.single())
        assertTrue(actual = screen.new)
        assertTrue(actual = authenticatedUseCases.materialUseCases.observeMaterials().first().isEmpty())
    }
}
