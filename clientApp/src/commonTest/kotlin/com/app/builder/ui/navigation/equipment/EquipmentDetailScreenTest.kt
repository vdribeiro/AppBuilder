package com.app.builder.ui.navigation.equipment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.domain.Equipment
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.gateway.UseCases
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen

class EquipmentDetailScreenTest: TestCase() {

    /**
     * Stores [FakeData.equipment] as the admin, then logs in with [credentials].
     *
     * @param credentials The credentials to log in with once the equipment is stored.
     * @return The admin's [UseCases].
     */
    private suspend fun storedEquipmentUseCases(credentials: UserCredentials = FakeData.adminCredentials): UseCases {
        val useCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        useCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        useCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)
        if (credentials != FakeData.adminCredentials) useCases.authenticationUseCases.login(credentials = credentials)
        return useCases
    }

    /**
     * Renders the equipment detail screen of the equipment with [equipmentUuid] with an action bar offering every equipment detail action, as the equipment entry provider does.
     *
     * @param useCases The use cases backing the stores.
     * @param equipmentUuid The uuid of the equipment to display.
     * @param new Whether the equipment is new and not stored yet.
     * @return The [EquipmentDetailScreenStore] and the [SessionStore] rendered.
     */
    private fun ComposeUiTest.setEquipmentDetailScreen(useCases: UseCases, equipmentUuid: Uuid = FakeData.equipment.uuid, new: Boolean = false): Pair<EquipmentDetailScreenStore, SessionStore> {
        val store = EquipmentDetailScreenStore(
            state = EquipmentDetailScreenState(),
            router = router,
            equipmentUseCases = useCases.equipmentUseCases,
            buildingEquipmentUseCases = useCases.buildingEquipmentUseCases,
            buildingUseCases = useCases.buildingUseCases,
            equipmentUuid = equipmentUuid.toString(),
            new = new
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = useCases.authenticationUseCases)
        val filtersStore = FiltersStore(state = FiltersState())
        val buildingFiltersStore = FiltersStore(state = FiltersState())

        setUI {
            EquipmentDetailScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                buildingFiltersStore = buildingFiltersStore,
                store = store,
            )
        }

        return store to sessionStore
    }

    /** Verifies that the equipment detail screen displays the name and description of the loaded equipment. */
    @Test
    fun equipmentDetailScreen() = runUITest {
        val useCases = storedEquipmentUseCases()

        val store = EquipmentDetailScreenStore(
            state = EquipmentDetailScreenState(),
            router = router,
            equipmentUseCases = useCases.equipmentUseCases,
            buildingEquipmentUseCases = useCases.buildingEquipmentUseCases,
            buildingUseCases = useCases.buildingUseCases,
            equipmentUuid = FakeData.equipment.uuid.toString()
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = useCases.authenticationUseCases)
        val filtersStore = FiltersStore(state = FiltersState())
        val buildingFiltersStore = FiltersStore(state = FiltersState())

        setUI {
            EquipmentDetailScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                buildingFiltersStore = buildingFiltersStore,
                store = store,
            )
        }

        assertTrue(actual = router.backStack.isEmpty())
        waitUntil { store.state.equipment != null }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        onNode(matcher = isSelected() and hasText(text = "equipment")).assertIsDisplayed()

        onNodeWithText(text = "equipment_name").assertIsDisplayed()
        onNodeWithText(text = FakeData.equipment.name).assertIsDisplayed()
        onNodeWithText(text = "equipment_description").assertIsDisplayed()
        onNodeWithText(text = FakeData.equipment.description).assertIsDisplayed()
    }

    /** Verifies that a user with write permission views the equipment read only, with the edit and delete buttons available. */
    @Test
    fun viewEquipment() = runUITest {
        val (store, _) = setEquipmentDetailScreen(useCases = storedEquipmentUseCases())

        waitUntil { store.state.equipment != null }
        assertFalse(actual = store.state.editing)
        onNodeWithText(text = FakeData.equipment.name).assert(matcher = !hasSetTextAction())
        onNodeWithText(text = FakeData.equipment.description).assert(matcher = !hasSetTextAction())
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
        onNodeWithTag(testTag = "action_delete").assertIsDisplayed()
        onNodeWithTag(testTag = "action_save").assertDoesNotExist()
        onNodeWithTag(testTag = "action_cancel").assertDoesNotExist()
    }

    /** Verifies that a user with only read permission views the equipment read only, without any of the edit buttons. */
    @Test
    fun readOnlyEquipment() = runUITest {
        val (store, sessionStore) = setEquipmentDetailScreen(useCases = storedEquipmentUseCases(credentials = FakeData.credentials))

        waitUntil { store.state.equipment != null && sessionStore.state.user?.name == FakeData.user.name }
        onNodeWithText(text = FakeData.equipment.name).assert(matcher = !hasSetTextAction())
        onNodeWithText(text = FakeData.equipment.description).assert(matcher = !hasSetTextAction())
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_save").assertDoesNotExist()
        onNodeWithTag(testTag = "action_cancel").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
    }

    /** Verifies that the viewed equipment follows the changes to the stored equipment. */
    @Test
    fun viewFollowsEquipment() = runUITest {
        val useCases = storedEquipmentUseCases()
        val (store, _) = setEquipmentDetailScreen(useCases = useCases)

        waitUntil { store.state.equipment != null }
        useCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment.copy(name = "Remote title"))

        waitUntil { store.state.equipment?.name == "Remote title" }
        onNodeWithText(text = "Remote title").assertIsDisplayed()
    }

    /** Verifies that the edit button makes the equipment editable, swaps the buttons and stops following the stored equipment. */
    @Test
    fun editEquipment() = runUITest {
        val useCases = storedEquipmentUseCases()
        val (store, _) = setEquipmentDetailScreen(useCases = useCases)

        waitUntil { store.state.equipment != null }
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }

        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
        onNodeWithTag(testTag = "action_save").assertIsDisplayed()
        onNodeWithTag(testTag = "action_cancel").assertIsDisplayed()

        onNodeWithText(text = FakeData.equipment.name).assertIsEnabled().performTextReplacement(text = "New title")
        onNodeWithText(text = FakeData.equipment.description).assertIsEnabled().performTextReplacement(text = "New description")
        waitForIdle()

        useCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment.copy(name = "Remote title"))
        useCases.equipmentUseCases.observeEquipment(uuid = FakeData.equipment.uuid).first { it?.name == "Remote title" }
        waitForIdle()

        assertEquals(expected = "New title", actual = store.state.equipment?.name)
        assertEquals(expected = "New description", actual = store.state.equipment?.description)
    }

    /** Verifies that the save button persists the edits and goes back to viewing the equipment. */
    @Test
    fun saveEquipment() = runUITest {
        val useCases = storedEquipmentUseCases()
        val (store, _) = setEquipmentDetailScreen(useCases = useCases)

        waitUntil { store.state.equipment != null }
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithText(text = FakeData.equipment.name).performTextReplacement(text = "New title")
        onNodeWithText(text = FakeData.equipment.description).performTextReplacement(text = "New description")
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { !store.state.editing }

        val saved = useCases.equipmentUseCases.observeEquipment(uuid = FakeData.equipment.uuid).first { it?.name == "New title" }
        assertEquals(expected = "New description", actual = saved?.description)
        onNodeWithText(text = "New title").assert(matcher = !hasSetTextAction())
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
        onNodeWithTag(testTag = "action_save").assertDoesNotExist()
    }

    /** Verifies that the cancel button discards the edits, reloads the stored equipment and goes back to viewing it. */
    @Test
    fun cancelEdit() = runUITest {
        val useCases = storedEquipmentUseCases()
        val (store, _) = setEquipmentDetailScreen(useCases = useCases)

        waitUntil { store.state.equipment != null }
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithText(text = FakeData.equipment.name).performTextReplacement(text = "New title")
        onNodeWithTag(testTag = "action_cancel").performClick()
        waitUntil { !store.state.editing }

        waitUntil { store.state.equipment?.name == FakeData.equipment.name }
        onNodeWithText(text = FakeData.equipment.name).assert(matcher = !hasSetTextAction())
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
        onNodeWithTag(testTag = "action_cancel").assertDoesNotExist()
        val stored = useCases.equipmentUseCases.observeEquipment(uuid = FakeData.equipment.uuid).first()
        assertEquals(expected = FakeData.equipment.name, actual = stored?.name)
    }

    /** Verifies that the delete action soft deletes the equipment, bumping its modification time, and navigates back. */
    @Test
    fun deleteEquipment() = runUITest {
        val useCases = storedEquipmentUseCases()

        router.navigate(screen = Screen.EquipmentList, option = Router.NavOption.ADD)
        router.navigate(screen = Screen.EquipmentDetail(uuid = FakeData.equipment.uuid.toString()), option = Router.NavOption.ADD)

        val (store, _) = setEquipmentDetailScreen(useCases = useCases)

        waitUntil { store.state.equipment != null }
        onNodeWithTag(testTag = "action_delete").assertIsDisplayed().performClick()
        waitForIdle()

        assertEquals(expected = listOf<Any>(Screen.EquipmentList), actual = router.backStack.toList())
        val deleted = useCases.equipmentUseCases.observeEquipment(uuid = FakeData.equipment.uuid).first()
        assertNotNull(actual = deleted?.deletedAt)
        assertEquals(expected = deleted.deletedAt, actual = deleted.modifiedAt)
    }

    /** Verifies that a deleted equipment hides edit, and that its delete toggle restores it, clearing its deletion, bumping its modification time, keeping viewing it and showing edit again. */
    @Test
    fun restoreEquipment() = runUITest {
        val useCases = storedEquipmentUseCases()
        val deletedEquipment = FakeData.equipment.copy(deletedAt = now())
        useCases.equipmentUseCases.upsertEquipment(equipment = deletedEquipment)
        val (store, _) = setEquipmentDetailScreen(useCases = useCases)

        waitUntil { store.state.equipment?.deletedAt != null }
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertIsDisplayed().performClick()
        waitUntil { store.state.equipment?.deletedAt == null }

        val restored = useCases.equipmentUseCases.observeEquipment(uuid = FakeData.equipment.uuid).first { it?.deletedAt == null }
        assertTrue(actual = restored!!.modifiedAt > deletedEquipment.modifiedAt)
        assertTrue(actual = router.backStack.isEmpty())
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
        onNodeWithTag(testTag = "action_delete").assertIsDisplayed()
    }

    /** Verifies that the edit and delete buttons stay hidden while the equipment is not loaded, here because it does not exist. */
    @Test
    fun missingEquipment() = runUITest {
        val useCases = storedEquipmentUseCases()
        val (store, sessionStore) = setEquipmentDetailScreen(useCases = useCases, equipmentUuid = uuid())

        waitUntil { sessionStore.state.account != null }
        waitForIdle()
        assertNull(actual = store.state.equipment)
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
    }

    /** Verifies that a new equipment opens in edit mode, with only the save and cancel buttons, and is not stored yet. */
    @Test
    fun newEquipment() = runUITest {
        val useCases = storedEquipmentUseCases()
        val equipmentUuid = uuid()
        val (store, _) = setEquipmentDetailScreen(useCases = useCases, equipmentUuid = equipmentUuid, new = true)

        waitUntil { store.state.equipment != null }
        assertTrue(actual = store.state.editing)
        assertTrue(actual = store.state.new)
        assertEquals(expected = equipmentUuid, actual = store.state.equipment?.uuid)
        onAllNodes(matcher = hasSetTextAction()).onFirst().assertIsEnabled()
        onNodeWithTag(testTag = "action_save").assertIsDisplayed()
        onNodeWithTag(testTag = "action_cancel").assertIsDisplayed()
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
        assertNull(actual = useCases.equipmentUseCases.observeEquipment(uuid = equipmentUuid).first())
    }

    /** Verifies that saving a new equipment stores it and goes back to viewing it. */
    @Test
    fun saveNewEquipment() = runUITest {
        val useCases = storedEquipmentUseCases()
        val equipmentUuid = uuid()
        val (store, _) = setEquipmentDetailScreen(useCases = useCases, equipmentUuid = equipmentUuid, new = true)

        waitUntil { store.state.equipment != null }
        onAllNodes(matcher = hasSetTextAction()).onFirst().performTextReplacement(text = "New title")
        waitUntil { store.state.equipment?.name == "New title" }
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { !store.state.editing }

        assertFalse(actual = store.state.new)
        val saved = useCases.equipmentUseCases.observeEquipment(uuid = equipmentUuid).first { it != null }
        assertEquals(expected = "New title", actual = saved?.name)
        onNodeWithText(text = "New title").assert(matcher = !hasSetTextAction())
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
    }

    /** Verifies that canceling a new equipment leaves the screen without storing it. */
    @Test
    fun cancelNewEquipment() = runUITest {
        val useCases = storedEquipmentUseCases()
        val equipmentUuid = uuid()

        router.navigate(screen = Screen.EquipmentList, option = Router.NavOption.ADD)
        router.navigate(screen = Screen.EquipmentDetail(uuid = equipmentUuid.toString(), new = true), option = Router.NavOption.ADD)

        val (store, _) = setEquipmentDetailScreen(useCases = useCases, equipmentUuid = equipmentUuid, new = true)

        waitUntil { store.state.equipment != null }
        onNodeWithTag(testTag = "action_cancel").performClick()
        waitUntil { router.backStack.size == 1 }

        assertEquals(expected = listOf<Any>(Screen.EquipmentList), actual = router.backStack.toList())
        assertNull(actual = useCases.equipmentUseCases.observeEquipment(uuid = equipmentUuid).first())
    }

    /** Verifies that the equipment status is shown, can't be changed while viewing, and is saved when changed while editing. */
    @Test
    fun changeStatus() = runUITest {
        val useCases = storedEquipmentUseCases()
        val (store, _) = setEquipmentDetailScreen(useCases = useCases)

        waitUntil { store.state.equipment != null }
        onNodeWithText(text = "equipment_status").assertIsDisplayed()
        onNodeWithText(text = "equipment_status_working").assertIsDisplayed()
        onNodeWithText(text = "equipment_status_malfunctioning").assertDoesNotExist()

        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithText(text = "equipment_status_malfunctioning").assertIsEnabled().performClick()
        waitUntil { store.state.equipment?.status == Equipment.Status.MALFUNCTIONING }
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { !store.state.editing }

        val saved = useCases.equipmentUseCases.observeEquipment(uuid = FakeData.equipment.uuid).first { it?.status == Equipment.Status.MALFUNCTIONING }
        assertEquals(expected = Equipment.Status.MALFUNCTIONING, actual = saved?.status)
    }

    /** Verifies that the linked buildings are listed, and selecting one opens it. */
    @Test
    fun buildingsAreListed() = runUITest {
        val useCases = storedEquipmentUseCases()
        useCases.buildingUseCases.upsertBuilding(building = FakeData.building)
        useCases.buildingEquipmentUseCases.upsertBuildingEquipment(buildingEquipment = FakeData.buildingEquipment)
        val (store, sessionStore) = setEquipmentDetailScreen(useCases = useCases)

        waitUntil { store.state.buildings.isNotEmpty() && sessionStore.state.account != null }
        onNodeWithTag(testTag = "equipment_buildings").assertIsDisplayed()
        onNodeWithTag(testTag = "equipment_buildings_${FakeData.building.uuid}").performClick()

        waitUntil { router.backStack.lastOrNull() == Screen.BuildingDetail(uuid = FakeData.building.uuid.toString()) }
    }

    /**
     * Builds the equipment detail store of [FakeData.equipment] with [useCases].
     *
     * @param useCases The use cases backing the store.
     * @return The [EquipmentDetailScreenStore].
     */
    private fun equipmentStore(useCases: UseCases): EquipmentDetailScreenStore = EquipmentDetailScreenStore(
        state = EquipmentDetailScreenState(),
        router = router,
        equipmentUseCases = useCases.equipmentUseCases,
        buildingEquipmentUseCases = useCases.buildingEquipmentUseCases,
        buildingUseCases = useCases.buildingUseCases,
        equipmentUuid = FakeData.equipment.uuid.toString()
    )

    /** Verifies that a building ticked in the picker and confirmed is listed, and linked to the equipment once saved. */
    @Test
    fun addBuilding() = runUITest {
        val useCases = storedEquipmentUseCases()
        useCases.buildingUseCases.upsertBuilding(building = FakeData.building)
        val (store, sessionStore) = setEquipmentDetailScreen(useCases = useCases)

        waitUntil { store.state.equipment != null && store.state.allBuildings.isNotEmpty() && sessionStore.state.account != null }
        onNodeWithTag(testTag = "equipment_buildings_link").assertDoesNotExist()
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithTag(testTag = "equipment_buildings_link").performClick()
        waitUntil { store.state.buildingPickerOpen }
        onNodeWithTag(testTag = "building_row_0").performClick()
        waitUntil { store.state.pickedBuildings.isNotEmpty() }
        onNode(matcher = hasTestTag(testTag = "action_save") and hasAnyAncestor(matcher = hasTestTag(testTag = "building_picker"))).performClick()
        waitUntil { !store.state.buildingPickerOpen && store.state.buildings.isNotEmpty() }
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { !store.state.editing }

        val buildings = useCases.buildingEquipmentUseCases.observeBuildingsOfEquipment(equipmentUuid = FakeData.equipment.uuid).first { it.isNotEmpty() }
        assertEquals(expected = listOf(element = FakeData.building), actual = buildings)
    }

    /** Verifies that canceling after unlinking a building keeps it linked, and that saving after only unlinking it deletes the building equipment and leaves the unchanged equipment as stored. */
    @Test
    fun buildingsOnlyEditLeavesEquipmentUnchanged() = runUnitTest {
        val useCases = storedEquipmentUseCases()
        useCases.buildingUseCases.upsertBuilding(building = FakeData.building)
        useCases.buildingEquipmentUseCases.upsertBuildingEquipment(buildingEquipment = FakeData.buildingEquipment)
        val store = equipmentStore(useCases = useCases)

        store.stateFlow.first { it.storedEquipment != null && it.buildings.isNotEmpty() }
        store.send(action = EquipmentDetailScreenAction.Edit)
        store.send(action = EquipmentDetailScreenAction.RemoveBuilding(buildingUuid = FakeData.building.uuid))
        store.send(action = EquipmentDetailScreenAction.Cancel)
        assertEquals(expected = listOf(element = FakeData.building), actual = store.stateFlow.first { !it.editing }.buildings)

        store.send(action = EquipmentDetailScreenAction.Edit)
        store.send(action = EquipmentDetailScreenAction.RemoveBuilding(buildingUuid = FakeData.building.uuid))
        store.send(action = EquipmentDetailScreenAction.Save)
        store.stateFlow.first { !it.editing }

        assertTrue(actual = useCases.buildingEquipmentUseCases.observeBuildingsOfEquipment(equipmentUuid = FakeData.equipment.uuid).first { it.isEmpty() }.isEmpty())
        assertEquals(expected = FakeData.equipment, actual = useCases.equipmentUseCases.observeEquipment(uuid = FakeData.equipment.uuid).first())
    }
}
