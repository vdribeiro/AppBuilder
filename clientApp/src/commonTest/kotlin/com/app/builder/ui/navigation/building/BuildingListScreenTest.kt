package com.app.builder.ui.navigation.building

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.first
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.security.uuid
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Building
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.test.count
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen

class BuildingListScreenTest: TestCase() {

    /** Verifies that the building list displays a seeded building and that tapping its card navigates to its detail screen. */
    @Test
    fun buildingScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.buildingUseCases.upsertBuilding(building = FakeData.building)

        // The list store only emits once BuildingPreferences holds data, and it is the filters store that seeds that file, so it has to be wired up as the provider does.
        val properties = Building.Property.entries.map { it.name }.toPersistentList()
        val defaults = AppFile.ActionBarData(
            search = "",
            sortProperty = Building.Property.MODIFIED_AT.name,
            sortAscending = false,
            visibleProperties = properties,
            searchableProperties = properties,
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val filtersStore = FiltersStore(
            state = FiltersState(visibleProperties = properties, searchableProperties = properties),
            storageFile = AppFile.BuildingPreferences,
            defaults = defaults
        )
        val buildingListScreenStore = BuildingListScreenStore(state = BuildingListScreenState(), router = router, buildingUseCases = authenticatedUseCases.buildingUseCases)

        setUI {
            BuildingListScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                store = buildingListScreenStore
            )
        }

        assertTrue(actual = router.backStack.isEmpty())
        waitUntil { buildingListScreenStore.state.buildings.size == 1 }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        onNodeWithText(text = "building").assertIsSelected()

        onNodeWithTag(testTag = "building_list").assertIsDisplayed()
        onNodeWithTag(testTag = "building_list").count(count = 1)

        onNodeWithTag(testTag = "building_row_0").assertIsDisplayed().performClick()
        val buildingDetailScreen = Screen.BuildingDetail(uuid = buildingListScreenStore.state.buildings.first().uuid)
        assertEquals(expected = listOf(buildingDetailScreen), actual = router.backStack.toList())
        waitUntil { buildingListScreenStore.state.buildings.first().selected }
    }

    /** Verifies that the selected building follows the building detail screen in the backstack, however it got there. */
    @Test
    fun selectionFollowsDetailScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        val otherBuilding = FakeData.building.copy(uuid = uuid(), name = "Other")
        authenticatedUseCases.buildingUseCases.upsertBuilding(building = FakeData.building)
        authenticatedUseCases.buildingUseCases.upsertBuilding(building = otherBuilding)

        // The list store only emits once BuildingPreferences holds data, and it is the filters store that seeds that file, so it has to be wired up as the provider does.
        val properties = Building.Property.entries.map { it.name }.toPersistentList()
        val defaults = AppFile.ActionBarData(
            search = "",
            sortProperty = Building.Property.MODIFIED_AT.name,
            sortAscending = false,
            visibleProperties = properties,
            searchableProperties = properties,
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val filtersStore = FiltersStore(
            state = FiltersState(visibleProperties = properties, searchableProperties = properties),
            storageFile = AppFile.BuildingPreferences,
            defaults = defaults
        )
        val buildingListScreenStore = BuildingListScreenStore(state = BuildingListScreenState(), router = router, buildingUseCases = authenticatedUseCases.buildingUseCases)

        setUI {
            BuildingListScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                store = buildingListScreenStore
            )
        }
        waitUntil { buildingListScreenStore.state.buildings.size == 2 }
        fun selectedUuids() = buildingListScreenStore.state.buildings.filter { it.selected }.map { it.uuid }

        router.navigate(screen = Screen.BuildingList)
        router.navigate(screen = Screen.BuildingDetail(uuid = otherBuilding.uuid.toString()), option = Router.NavOption.REPLACE_LAST)
        waitUntil { selectedUuids() == listOf(otherBuilding.uuid.toString()) }

        router.navigate(screen = Screen.BuildingDetail(uuid = FakeData.building.uuid.toString()), option = Router.NavOption.REPLACE_LAST)
        waitUntil { selectedUuids() == listOf(FakeData.building.uuid.toString()) }

        router.navigate(screen = Screen.BuildingDetail(uuid = uuid().toString(), new = true), option = Router.NavOption.REPLACE_LAST)
        waitUntil { selectedUuids().isEmpty() }

        router.navigate(screen = Screen.BuildingDetail(uuid = otherBuilding.uuid.toString()), option = Router.NavOption.REPLACE_LAST)
        waitUntil { selectedUuids() == listOf(otherBuilding.uuid.toString()) }
        router.back()
        waitUntil { selectedUuids().isEmpty() }
    }

    /** Verifies that the add action opens the detail screen of a new building without storing it. */
    @Test
    fun addBuilding() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val properties = Building.Property.entries.map { it.name }.toPersistentList()
        val defaults = AppFile.ActionBarData(
            search = "",
            sortProperty = Building.Property.MODIFIED_AT.name,
            sortAscending = false,
            visibleProperties = properties,
            searchableProperties = properties,
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val filtersStore = FiltersStore(
            state = FiltersState(visibleProperties = properties, searchableProperties = properties),
            storageFile = AppFile.BuildingPreferences,
            defaults = defaults
        )
        val buildingListScreenStore = BuildingListScreenStore(state = BuildingListScreenState(), router = router, buildingUseCases = authenticatedUseCases.buildingUseCases)

        setUI {
            BuildingListScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                store = buildingListScreenStore
            )
        }

        onNodeWithTag(testTag = "action_add").assertIsDisplayed().performClick()
        waitUntil { router.backStack.isNotEmpty() }

        val screen = assertIs<Screen.BuildingDetail>(value = router.backStack.single())
        assertTrue(actual = screen.new)
        assertTrue(actual = authenticatedUseCases.buildingUseCases.observeBuildings().first().isEmpty())
    }
}
