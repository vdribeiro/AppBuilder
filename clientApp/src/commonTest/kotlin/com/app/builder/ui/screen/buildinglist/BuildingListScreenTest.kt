package com.app.builder.ui.screen.buildinglist

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
import com.app.builder.domain.Building
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

class BuildingListScreenTest: TestCase() {

    /** Verifies that the building list displays a seeded building and that tapping its card navigates to its detail screen. */
    @Test
    fun buildingScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.buildingUseCases.upsertBuilding(building = FakeData.building)

        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.BUILDING), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        // The list store only emits once BuildingPreferences holds data, and it is the action bar store that seeds that file, so it has to be wired up as the provider does.
        val properties = Building.Property.entries.map { it.name }.toPersistentList()
        val defaults = AppFile.ActionBarData(
            mode = ActionBarMode.DEFAULT.name,
            search = "",
            sortProperty = Building.Property.MODIFIED_AT.name,
            sortAscending = false,
            visibleProperties = properties,
            searchableProperties = properties,
        )
        val actionBarStore = ActionBarStore(
            state = ActionBarState(title = "buildings", layout = ActionBarLayout.LIST, visibleProperties = properties, searchableProperties = properties),
            router = router,
            authenticationUseCases = authenticatedUseCases.authenticationUseCases,
            storageFile = AppFile.BuildingPreferences,
            defaults = defaults,
            entityType = EntityType.BUILDING,
        )
        val buildingListScreenStore = BuildingListScreenStore(state = BuildingListScreenState(), router = router, buildingUseCases = authenticatedUseCases.buildingUseCases)

        setUI {
            BuildingListScreen(
                actionBarStore = actionBarStore,
                navigationStore = navigationStore,
                store = buildingListScreenStore
            )
        }

        assertTrue(actual = router.backStack.isEmpty())
        waitUntil { buildingListScreenStore.state.buildings.size == 1 }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        assertTrue(actual = navigationStore.state.items.first { it.text == "building" }.selected)

        onNodeWithText(text = "buildings").assertIsDisplayed()
        onNodeWithTag(testTag = "building_list").assertIsDisplayed()
        onNodeWithTag(testTag = "building_list").count(count = 1)

        onNodeWithTag(testTag = "building_card_0").assertIsDisplayed().performClick()
        val buildingDetailScreen = Screen.BuildingDetail(uuid = buildingListScreenStore.state.buildings.first().uuid)
        assertEquals(expected = listOf(buildingDetailScreen), actual = router.backStack.toList())
    }

    /** Verifies that batch deleting a building whose properties are hidden from the list still keeps those properties on the stored entity. */
    @Test
    fun batchDeleteBuilding() = runUnitTest {
        val buildingUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .buildingUseCases
        buildingUseCases.upsertBuilding(building = FakeData.building)

        // Only the modification timestamp is visible, so the list item carries neither name nor code.
        AppFile.BuildingPreferences.save {
            AppFile.ActionBarData(
                mode = ActionBarMode.BATCH_DELETE.name,
                search = "",
                sortProperty = Building.Property.MODIFIED_AT.name,
                sortAscending = false,
                visibleProperties = listOf(element = Building.Property.MODIFIED_AT.name),
                searchableProperties = Building.Property.entries.map { it.name },
            )
        }

        val store = BuildingListScreenStore(state = BuildingListScreenState(), router = router, buildingUseCases = buildingUseCases)
        // The store only observes while its state is being collected, as it would be on screen.
        backgroundScope.launch { store.stateFlow.collect { } }
        advanceUntilIdle()
        assertEquals(expected = 1, actual = store.state.buildings.size)
        assertNull(actual = store.state.buildings.first().name)

        store.send(action = BuildingListScreenAction.SelectBuilding(buildingUuid = FakeData.building.uuid.toString()))
        advanceUntilIdle()
        store.send(action = BuildingListScreenAction.Ok(mode = ActionBarMode.BATCH_DELETE))
        advanceUntilIdle()

        val deleted = buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).firstOrNull()
        assertNotNull(actual = deleted?.deletedAt)
        assertEquals(expected = FakeData.building.name, actual = deleted.name)
        assertEquals(expected = FakeData.building.code, actual = deleted.code)
    }
}
