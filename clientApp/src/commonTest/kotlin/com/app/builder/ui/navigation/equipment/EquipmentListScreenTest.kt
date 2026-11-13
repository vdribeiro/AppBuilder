package com.app.builder.ui.navigation.equipment

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
import com.app.builder.domain.Equipment
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.test.count
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen

class EquipmentListScreenTest: TestCase() {

    /** Verifies that the equipment list displays a seeded equipment and that tapping its card navigates to its detail screen. */
    @Test
    fun equipmentScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)

        // The list store only emits once EquipmentPreferences holds data, and it is the filters store that seeds that file, so it has to be wired up as the provider does.
        val properties = Equipment.Property.entries.map { it.name }.toPersistentList()
        val defaults = AppFile.ActionBarData(
            search = "",
            sortProperty = Equipment.Property.MODIFIED_AT.name,
            sortAscending = false,
            visibleProperties = properties,
            searchableProperties = properties,
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val filtersStore = FiltersStore(
            state = FiltersState(visibleProperties = properties, searchableProperties = properties),
            storageFile = AppFile.EquipmentPreferences,
            defaults = defaults
        )
        val equipmentListScreenStore = EquipmentListScreenStore(state = EquipmentListScreenState(), router = router, equipmentUseCases = authenticatedUseCases.equipmentUseCases)

        setUI {
            EquipmentListScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                store = equipmentListScreenStore
            )
        }

        assertTrue(actual = router.backStack.isEmpty())
        waitUntil { equipmentListScreenStore.state.equipments.size == 1 }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        onNodeWithText(text = "equipment").assertIsSelected()

        onNodeWithTag(testTag = "equipment_list").assertIsDisplayed()
        onNodeWithTag(testTag = "equipment_list").count(count = 1)

        onNodeWithTag(testTag = "equipment_row_0").assertIsDisplayed().performClick()
        val equipmentDetailScreen = Screen.EquipmentDetail(uuid = equipmentListScreenStore.state.equipments.first().uuid)
        assertEquals(expected = listOf(equipmentDetailScreen), actual = router.backStack.toList())
        waitUntil { equipmentListScreenStore.state.equipments.first().selected }
    }

    /** Verifies that the selected equipment follows the equipment detail screen in the backstack, however it got there. */
    @Test
    fun selectionFollowsDetailScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        val otherEquipment = FakeData.equipment.copy(uuid = uuid(), name = "Other")
        authenticatedUseCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)
        authenticatedUseCases.equipmentUseCases.upsertEquipment(equipment = otherEquipment)

        // The list store only emits once EquipmentPreferences holds data, and it is the filters store that seeds that file, so it has to be wired up as the provider does.
        val properties = Equipment.Property.entries.map { it.name }.toPersistentList()
        val defaults = AppFile.ActionBarData(
            search = "",
            sortProperty = Equipment.Property.MODIFIED_AT.name,
            sortAscending = false,
            visibleProperties = properties,
            searchableProperties = properties,
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val filtersStore = FiltersStore(
            state = FiltersState(visibleProperties = properties, searchableProperties = properties),
            storageFile = AppFile.EquipmentPreferences,
            defaults = defaults
        )
        val equipmentListScreenStore = EquipmentListScreenStore(state = EquipmentListScreenState(), router = router, equipmentUseCases = authenticatedUseCases.equipmentUseCases)

        setUI {
            EquipmentListScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                store = equipmentListScreenStore
            )
        }
        waitUntil { equipmentListScreenStore.state.equipments.size == 2 }
        fun selectedUuids() = equipmentListScreenStore.state.equipments.filter { it.selected }.map { it.uuid }

        router.navigate(screen = Screen.EquipmentList)
        router.navigate(screen = Screen.EquipmentDetail(uuid = otherEquipment.uuid.toString()), option = Router.NavOption.REPLACE_LAST)
        waitForIdle()
        waitUntil { selectedUuids() == listOf(otherEquipment.uuid.toString()) }

        router.navigate(screen = Screen.EquipmentDetail(uuid = FakeData.equipment.uuid.toString()), option = Router.NavOption.REPLACE_LAST)
        waitForIdle()
        waitUntil { selectedUuids() == listOf(FakeData.equipment.uuid.toString()) }

        router.navigate(screen = Screen.EquipmentDetail(uuid = uuid().toString(), new = true), option = Router.NavOption.REPLACE_LAST)
        waitForIdle()
        waitUntil { selectedUuids().isEmpty() }

        router.navigate(screen = Screen.EquipmentDetail(uuid = otherEquipment.uuid.toString()), option = Router.NavOption.REPLACE_LAST)
        waitForIdle()
        waitUntil { selectedUuids() == listOf(otherEquipment.uuid.toString()) }
        router.back()
        waitForIdle()
        waitUntil { selectedUuids().isEmpty() }
    }

    /** Verifies that the add action opens the detail screen of a new equipment without storing it. */
    @Test
    fun addEquipment() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val properties = Equipment.Property.entries.map { it.name }.toPersistentList()
        val defaults = AppFile.ActionBarData(
            search = "",
            sortProperty = Equipment.Property.MODIFIED_AT.name,
            sortAscending = false,
            visibleProperties = properties,
            searchableProperties = properties,
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val filtersStore = FiltersStore(
            state = FiltersState(visibleProperties = properties, searchableProperties = properties),
            storageFile = AppFile.EquipmentPreferences,
            defaults = defaults
        )
        val equipmentListScreenStore = EquipmentListScreenStore(state = EquipmentListScreenState(), router = router, equipmentUseCases = authenticatedUseCases.equipmentUseCases)

        setUI {
            EquipmentListScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                store = equipmentListScreenStore
            )
        }

        onNodeWithTag(testTag = "action_add").assertIsDisplayed().performClick()
        waitUntil { router.backStack.isNotEmpty() }

        val screen = assertIs<Screen.EquipmentDetail>(value = router.backStack.single())
        assertTrue(actual = screen.new)
        assertTrue(actual = authenticatedUseCases.equipmentUseCases.observeEquipments().first().isEmpty())
    }
}
