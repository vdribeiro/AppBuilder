package com.app.builder.ui.screen.materiallist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.collections.immutable.toPersistentList
import com.app.builder.Dependency.getUserDependency
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.Material
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.test.count
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.bar.ActionBarLayout
import com.app.builder.ui.component.bar.ActionBarMode
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.navigation.Screen

class MaterialListScreenTest: TestCase() {

    /** Verifies that the material list displays a seeded material and that tapping its card navigates to its detail screen. */
    @Test
    fun materialScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.materialUseCases.upsertMaterial(material = FakeData.material)

        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.MATERIAL), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        // The list store only emits once MaterialPreferences holds data, and it is the action bar store that seeds that file, so it has to be wired up as the provider does.
        val properties = Material.Property.entries.map { it.name }.toPersistentList()
        val defaults = AppFile.ActionBarData(
            mode = ActionBarMode.DEFAULT.name,
            search = "",
            sortProperty = Material.Property.MODIFIED_AT.name,
            sortAscending = false,
            visibleProperties = properties,
            searchableProperties = properties,
        )
        val actionBarStore = ActionBarStore(
            state = ActionBarState(title = "materials", layout = ActionBarLayout.LIST, visibleProperties = properties, searchableProperties = properties),
            router = router,
            authenticationUseCases = authenticatedUseCases.authenticationUseCases,
            storageFile = AppFile.MaterialPreferences,
            defaults = defaults,
            entityType = EntityType.MATERIAL,
        )
        val materialListScreenStore = MaterialListScreenStore(state = MaterialListScreenState(), router = router, materialUseCases = authenticatedUseCases.materialUseCases)

        setUI {
            MaterialListScreen(
                actionBarStore = actionBarStore,
                navigationStore = navigationStore,
                store = materialListScreenStore
            )
        }

        assertTrue(actual = router.backStack.isEmpty())
        waitUntil { materialListScreenStore.state.materials.size == 1 }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        assertTrue(actual = navigationStore.state.items.first { it.text == "material" }.selected)

        onNodeWithText(text = "materials").assertIsDisplayed()
        onNodeWithTag(testTag = "material_list").assertIsDisplayed()
        onNodeWithTag(testTag = "material_list").count(count = 1)

        onNodeWithTag(testTag = "material_card_0").assertIsDisplayed().performClick()
        val materialDetailScreen = Screen.MaterialDetail(uuid = materialListScreenStore.state.materials.first().uuid)
        assertEquals(expected = listOf(materialDetailScreen), actual = router.backStack.toList())
    }

    /** Verifies that batch deleting a material whose properties are hidden from the list still keeps those properties on the stored entity. */
    @Test
    fun batchDeleteMaterial() = runUnitTest {
        val materialUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .materialUseCases
        materialUseCases.upsertMaterial(material = FakeData.material)

        // Only the modification timestamp is visible, so the list item carries neither name nor code.
        AppFile.MaterialPreferences.save {
            AppFile.ActionBarData(
                mode = ActionBarMode.BATCH_DELETE.name,
                search = "",
                sortProperty = Material.Property.MODIFIED_AT.name,
                sortAscending = false,
                visibleProperties = listOf(element = Material.Property.MODIFIED_AT.name),
                searchableProperties = Material.Property.entries.map { it.name },
            )
        }

        val store = MaterialListScreenStore(state = MaterialListScreenState(), router = router, materialUseCases = materialUseCases)
        // The store only observes while its state is being collected, as it would be on screen.
        backgroundScope.launch { store.stateFlow.collect { } }
        advanceUntilIdle()
        assertEquals(expected = 1, actual = store.state.materials.size)
        assertNull(actual = store.state.materials.first().name)

        store.send(action = MaterialListScreenAction.SelectMaterial(materialUuid = FakeData.material.uuid.toString()))
        advanceUntilIdle()
        store.send(action = MaterialListScreenAction.Ok(mode = ActionBarMode.BATCH_DELETE))
        advanceUntilIdle()

        val deleted = materialUseCases.observeMaterial(uuid = FakeData.material.uuid).firstOrNull()
        assertNotNull(actual = deleted?.deletedAt)
        assertEquals(expected = FakeData.material.name, actual = deleted.name)
        assertEquals(expected = FakeData.material.code, actual = deleted.code)
    }
}
