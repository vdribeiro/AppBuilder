package com.app.builder.ui.screen.equipmentlist

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
import com.app.builder.domain.Equipment
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

class EquipmentListScreenTest: TestCase() {

    /** Verifies that the equipment list displays a seeded equipment and that tapping its card navigates to its detail screen. */
    @Test
    fun equipmentScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)

        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.EQUIPMENT), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        // The list store only emits once EquipmentPreferences holds data, and it is the action bar store that seeds that file, so it has to be wired up as the provider does.
        val properties = Equipment.Property.entries.map { it.name }.toPersistentList()
        val defaults = AppFile.ActionBarData(
            mode = ActionBarMode.DEFAULT.name,
            search = "",
            sortProperty = Equipment.Property.MODIFIED_AT.name,
            sortAscending = false,
            visibleProperties = properties,
            searchableProperties = properties,
        )
        val actionBarStore = ActionBarStore(
            state = ActionBarState(title = "equipments", layout = ActionBarLayout.LIST, visibleProperties = properties, searchableProperties = properties),
            router = router,
            authenticationUseCases = authenticatedUseCases.authenticationUseCases,
            storageFile = AppFile.EquipmentPreferences,
            defaults = defaults,
            entityType = EntityType.EQUIPMENT,
        )
        val equipmentListScreenStore = EquipmentListScreenStore(state = EquipmentListScreenState(), router = router, equipmentUseCases = authenticatedUseCases.equipmentUseCases)

        setUI {
            EquipmentListScreen(
                actionBarStore = actionBarStore,
                navigationStore = navigationStore,
                store = equipmentListScreenStore
            )
        }

        assertTrue(actual = router.backStack.isEmpty())
        waitUntil { equipmentListScreenStore.state.equipments.size == 1 }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        assertTrue(actual = navigationStore.state.items.first { it.text == "equipment" }.selected)

        onNodeWithText(text = "equipments").assertIsDisplayed()
        onNodeWithTag(testTag = "equipment_list").assertIsDisplayed()
        onNodeWithTag(testTag = "equipment_list").count(count = 1)

        onNodeWithTag(testTag = "equipment_card_0").assertIsDisplayed().performClick()
        val equipmentDetailScreen = Screen.EquipmentDetail(uuid = equipmentListScreenStore.state.equipments.first().uuid)
        assertEquals(expected = listOf(equipmentDetailScreen), actual = router.backStack.toList())
    }

    /** Verifies that batch deleting a equipment whose properties are hidden from the list still keeps those properties on the stored entity. */
    @Test
    fun batchDeleteEquipment() = runUnitTest {
        val equipmentUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .equipmentUseCases
        equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)

        // Only the modification timestamp is visible, so the list item carries neither name nor code.
        AppFile.EquipmentPreferences.save {
            AppFile.ActionBarData(
                mode = ActionBarMode.BATCH_DELETE.name,
                search = "",
                sortProperty = Equipment.Property.MODIFIED_AT.name,
                sortAscending = false,
                visibleProperties = listOf(element = Equipment.Property.MODIFIED_AT.name),
                searchableProperties = Equipment.Property.entries.map { it.name },
            )
        }

        val store = EquipmentListScreenStore(state = EquipmentListScreenState(), router = router, equipmentUseCases = equipmentUseCases)
        // The store only observes while its state is being collected, as it would be on screen.
        backgroundScope.launch { store.stateFlow.collect { } }
        advanceUntilIdle()
        assertEquals(expected = 1, actual = store.state.equipments.size)
        assertNull(actual = store.state.equipments.first().name)

        store.send(action = EquipmentListScreenAction.SelectEquipment(equipmentUuid = FakeData.equipment.uuid.toString()))
        advanceUntilIdle()
        store.send(action = EquipmentListScreenAction.Ok(mode = ActionBarMode.BATCH_DELETE))
        advanceUntilIdle()

        val deleted = equipmentUseCases.observeEquipment(uuid = FakeData.equipment.uuid).firstOrNull()
        assertNotNull(actual = deleted?.deletedAt)
        assertEquals(expected = FakeData.equipment.name, actual = deleted.name)
        assertEquals(expected = FakeData.equipment.code, actual = deleted.code)
    }
}
