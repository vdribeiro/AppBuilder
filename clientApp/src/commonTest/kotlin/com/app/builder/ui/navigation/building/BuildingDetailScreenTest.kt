package com.app.builder.ui.navigation.building

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
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.gateway.UseCases
import com.app.builder.domain.gateway.buildingmaterial.MaterialInBuilding
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.equipment.equipmentFilterDefaults
import com.app.builder.ui.navigation.material.materialFilterDefaults

class BuildingDetailScreenTest: TestCase() {

    /**
     * Stores [FakeData.building] as the admin, then logs in with [credentials].
     *
     * @param credentials The credentials to log in with once the building is stored.
     * @return The admin's [UseCases].
     */
    private suspend fun storedBuildingUseCases(credentials: UserCredentials = FakeData.adminCredentials): UseCases {
        val useCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        useCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        useCases.buildingUseCases.upsertBuilding(building = FakeData.building)
        if (credentials != FakeData.adminCredentials) useCases.authenticationUseCases.login(credentials = credentials)
        return useCases
    }

    /**
     * Renders the building detail screen of the building with [buildingUuid] with an action bar offering every building detail action, as the building entry provider does.
     *
     * @param useCases The use cases backing the stores.
     * @param buildingUuid The uuid of the building to display.
     * @param new Whether the building is new and not stored yet.
     * @return The [BuildingDetailScreenStore] and the [SessionStore] rendered.
     */
    private fun ComposeUiTest.setBuildingDetailScreen(useCases: UseCases, buildingUuid: Uuid = FakeData.building.uuid, new: Boolean = false): Pair<BuildingDetailScreenStore, SessionStore> {
        val store = BuildingDetailScreenStore(
            state = BuildingDetailScreenState(),
            router = router,
            buildingUseCases = useCases.buildingUseCases,
            buildingEquipmentUseCases = useCases.buildingEquipmentUseCases,
            equipmentUseCases = useCases.equipmentUseCases,
            buildingMaterialUseCases = useCases.buildingMaterialUseCases,
            materialUseCases = useCases.materialUseCases,
            buildingUuid = buildingUuid.toString(),
            new = new
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = useCases.authenticationUseCases)
        val filtersStore = FiltersStore(state = FiltersState())
        val equipmentFiltersStore = FiltersStore(state = FiltersState())
        val materialFiltersStore = FiltersStore(state = FiltersState())

        setUI {
            BuildingDetailScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                equipmentFiltersStore = equipmentFiltersStore,
                materialFiltersStore = materialFiltersStore,
                store = store,
            )
        }

        return store to sessionStore
    }

    /** Verifies that the building detail screen displays the name and description of the loaded building. */
    @Test
    fun buildingDetailScreen() = runUITest {
        val useCases = storedBuildingUseCases()

        val store = BuildingDetailScreenStore(
            state = BuildingDetailScreenState(),
            router = router,
            buildingUseCases = useCases.buildingUseCases,
            buildingEquipmentUseCases = useCases.buildingEquipmentUseCases,
            equipmentUseCases = useCases.equipmentUseCases,
            buildingMaterialUseCases = useCases.buildingMaterialUseCases,
            materialUseCases = useCases.materialUseCases,
            buildingUuid = FakeData.building.uuid.toString()
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = useCases.authenticationUseCases)
        val filtersStore = FiltersStore(state = FiltersState())
        val equipmentFiltersStore = FiltersStore(state = FiltersState())
        val materialFiltersStore = FiltersStore(state = FiltersState())

        setUI {
            BuildingDetailScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                equipmentFiltersStore = equipmentFiltersStore,
                materialFiltersStore = materialFiltersStore,
                store = store,
            )
        }

        assertTrue(actual = router.backStack.isEmpty())
        waitUntil { store.state.building != null }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        onNode(matcher = isSelected() and hasText(text = "building")).assertIsDisplayed()

        onNodeWithText(text = "building_name").assertIsDisplayed()
        onNodeWithText(text = FakeData.building.name).assertIsDisplayed()
        onNodeWithText(text = "building_description").assertIsDisplayed()
        onNodeWithText(text = FakeData.building.description).assertIsDisplayed()
    }

    /** Verifies that a user with write permission views the building read only, with the edit and delete buttons available. */
    @Test
    fun viewBuilding() = runUITest {
        val (store, _) = setBuildingDetailScreen(useCases = storedBuildingUseCases())

        waitUntil { store.state.building != null }
        assertFalse(actual = store.state.editing)
        onNodeWithText(text = FakeData.building.name).assert(matcher = !hasSetTextAction())
        onNodeWithText(text = FakeData.building.description).assert(matcher = !hasSetTextAction())
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
        onNodeWithTag(testTag = "action_delete").assertIsDisplayed()
        onNodeWithTag(testTag = "action_save").assertDoesNotExist()
        onNodeWithTag(testTag = "action_cancel").assertDoesNotExist()
    }

    /** Verifies that a user with only read permission views the building read only, without any of the edit buttons. */
    @Test
    fun readOnlyBuilding() = runUITest {
        val (store, sessionStore) = setBuildingDetailScreen(useCases = storedBuildingUseCases(credentials = FakeData.credentials))

        waitUntil { store.state.building != null && sessionStore.state.user?.name == FakeData.user.name }
        onNodeWithText(text = FakeData.building.name).assert(matcher = !hasSetTextAction())
        onNodeWithText(text = FakeData.building.description).assert(matcher = !hasSetTextAction())
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_save").assertDoesNotExist()
        onNodeWithTag(testTag = "action_cancel").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
    }

    /** Verifies that the viewed building follows the changes to the stored building. */
    @Test
    fun viewFollowsBuilding() = runUITest {
        val useCases = storedBuildingUseCases()
        val (store, _) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.building != null }
        useCases.buildingUseCases.upsertBuilding(building = FakeData.building.copy(name = "Remote title"))

        waitUntil { store.state.building?.name == "Remote title" }
        onNodeWithText(text = "Remote title").assertIsDisplayed()
    }

    /** Verifies that the edit button makes the building editable, swaps the buttons and stops following the stored building. */
    @Test
    fun editBuilding() = runUITest {
        val useCases = storedBuildingUseCases()
        val (store, _) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.building != null }
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }

        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
        onNodeWithTag(testTag = "action_save").assertIsDisplayed()
        onNodeWithTag(testTag = "action_cancel").assertIsDisplayed()

        onNodeWithText(text = FakeData.building.name).assertIsEnabled().performTextReplacement(text = "New title")
        onNodeWithText(text = FakeData.building.description).assertIsEnabled().performTextReplacement(text = "New description")
        waitForIdle()

        useCases.buildingUseCases.upsertBuilding(building = FakeData.building.copy(name = "Remote title"))
        useCases.buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).first { it?.name == "Remote title" }
        waitForIdle()

        assertEquals(expected = "New title", actual = store.state.building?.name)
        assertEquals(expected = "New description", actual = store.state.building?.description)
    }

    /** Verifies that the save button persists the edits and goes back to viewing the building. */
    @Test
    fun saveBuilding() = runUITest {
        val useCases = storedBuildingUseCases()
        val (store, _) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.building != null }
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithText(text = FakeData.building.name).performTextReplacement(text = "New title")
        onNodeWithText(text = FakeData.building.description).performTextReplacement(text = "New description")
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { !store.state.editing }

        val saved = useCases.buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).first { it?.name == "New title" }
        assertEquals(expected = "New description", actual = saved?.description)
        onNodeWithText(text = "New title").assert(matcher = !hasSetTextAction())
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
        onNodeWithTag(testTag = "action_save").assertDoesNotExist()
    }

    /** Verifies that the cancel button discards the edits, reloads the stored building and goes back to viewing it. */
    @Test
    fun cancelEdit() = runUITest {
        val useCases = storedBuildingUseCases()
        val (store, _) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.building != null }
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithText(text = FakeData.building.name).performTextReplacement(text = "New title")
        onNodeWithTag(testTag = "action_cancel").performClick()
        waitUntil { !store.state.editing }

        waitUntil { store.state.building?.name == FakeData.building.name }
        onNodeWithText(text = FakeData.building.name).assert(matcher = !hasSetTextAction())
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
        onNodeWithTag(testTag = "action_cancel").assertDoesNotExist()
        val stored = useCases.buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).first()
        assertEquals(expected = FakeData.building.name, actual = stored?.name)
    }

    /** Verifies that the delete action soft deletes the building, bumping its modification time, and navigates back. */
    @Test
    fun deleteBuilding() = runUITest {
        val useCases = storedBuildingUseCases()

        router.navigate(screen = Screen.BuildingList, option = Router.NavOption.ADD)
        router.navigate(screen = Screen.BuildingDetail(uuid = FakeData.building.uuid.toString()), option = Router.NavOption.ADD)

        val (store, _) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.building != null }
        onNodeWithTag(testTag = "action_delete").assertIsDisplayed().performClick()
        waitForIdle()

        assertEquals(expected = listOf<Any>(Screen.BuildingList), actual = router.backStack.toList())
        val deleted = useCases.buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).first()
        assertNotNull(actual = deleted?.deletedAt)
        assertEquals(expected = deleted.deletedAt, actual = deleted.modifiedAt)
    }

    /** Verifies that a deleted building hides edit, and that its delete toggle restores it, clearing its deletion, bumping its modification time, keeping viewing it and showing edit again. */
    @Test
    fun restoreBuilding() = runUITest {
        val useCases = storedBuildingUseCases()
        val deletedBuilding = FakeData.building.copy(deletedAt = now())
        useCases.buildingUseCases.upsertBuilding(building = deletedBuilding)
        val (store, _) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.building?.deletedAt != null }
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertIsDisplayed().performClick()
        waitUntil { store.state.building?.deletedAt == null }

        val restored = useCases.buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).first { it?.deletedAt == null }
        assertTrue(actual = restored!!.modifiedAt > deletedBuilding.modifiedAt)
        assertTrue(actual = router.backStack.isEmpty())
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
        onNodeWithTag(testTag = "action_delete").assertIsDisplayed()
    }

    /** Verifies that the edit and delete buttons stay hidden while the building is not loaded, here because it does not exist. */
    @Test
    fun missingBuilding() = runUITest {
        val useCases = storedBuildingUseCases()
        val (store, sessionStore) = setBuildingDetailScreen(useCases = useCases, buildingUuid = uuid())

        waitUntil { sessionStore.state.account != null }
        waitForIdle()
        assertNull(actual = store.state.building)
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
    }

    /** Verifies that a new building opens in edit mode, with only the save and cancel buttons, and is not stored yet. */
    @Test
    fun newBuilding() = runUITest {
        val useCases = storedBuildingUseCases()
        val buildingUuid = uuid()
        val (store, _) = setBuildingDetailScreen(useCases = useCases, buildingUuid = buildingUuid, new = true)

        waitUntil { store.state.building != null }
        assertTrue(actual = store.state.editing)
        assertTrue(actual = store.state.new)
        assertEquals(expected = buildingUuid, actual = store.state.building?.uuid)
        onAllNodes(matcher = hasSetTextAction()).onFirst().assertIsEnabled()
        onNodeWithTag(testTag = "action_save").assertIsDisplayed()
        onNodeWithTag(testTag = "action_cancel").assertIsDisplayed()
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
        assertNull(actual = useCases.buildingUseCases.observeBuilding(uuid = buildingUuid).first())
    }

    /** Verifies that saving a new building stores it and goes back to viewing it. */
    @Test
    fun saveNewBuilding() = runUITest {
        val useCases = storedBuildingUseCases()
        val buildingUuid = uuid()
        val (store, _) = setBuildingDetailScreen(useCases = useCases, buildingUuid = buildingUuid, new = true)

        waitUntil { store.state.building != null }
        onAllNodes(matcher = hasSetTextAction()).onFirst().performTextReplacement(text = "New title")
        waitUntil { store.state.building?.name == "New title" }
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { !store.state.editing }

        assertFalse(actual = store.state.new)
        val saved = useCases.buildingUseCases.observeBuilding(uuid = buildingUuid).first { it != null }
        assertEquals(expected = "New title", actual = saved?.name)
        onNodeWithText(text = "New title").assert(matcher = !hasSetTextAction())
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
    }

    /** Verifies that canceling a new building leaves the screen without storing it. */
    @Test
    fun cancelNewBuilding() = runUITest {
        val useCases = storedBuildingUseCases()
        val buildingUuid = uuid()

        router.navigate(screen = Screen.BuildingList, option = Router.NavOption.ADD)
        router.navigate(screen = Screen.BuildingDetail(uuid = buildingUuid.toString(), new = true), option = Router.NavOption.ADD)

        val (store, _) = setBuildingDetailScreen(useCases = useCases, buildingUuid = buildingUuid, new = true)

        waitUntil { store.state.building != null }
        onNodeWithTag(testTag = "action_cancel").performClick()
        waitUntil { router.backStack.size == 1 }

        assertEquals(expected = listOf<Any>(Screen.BuildingList), actual = router.backStack.toList())
        assertNull(actual = useCases.buildingUseCases.observeBuilding(uuid = buildingUuid).first())
    }

    /** Verifies that the coordinates are shown while viewing, and the typed ones are saved when editing. */
    @Test
    fun changeCoordinates() = runUITest {
        val useCases = storedBuildingUseCases()
        val (store, _) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.building != null }
        onNodeWithText(text = FakeData.building.latitude.toString()).assertIsDisplayed()
        onNodeWithText(text = FakeData.building.longitude.toString()).assertIsDisplayed()

        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithTag(testTag = "building_latitude", useUnmergedTree = true).performTextReplacement(text = "40.5")
        onNodeWithTag(testTag = "building_longitude", useUnmergedTree = true).performTextReplacement(text = "-8.25")
        waitUntil { store.state.latitude == "40.5" && store.state.longitude == "-8.25" }
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { !store.state.editing }

        val saved = useCases.buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).first { it?.latitude == 40.5 }
        assertEquals(expected = -8.25, actual = saved?.longitude)
    }

    /** Verifies that out-of-range, malformed or half-filled coordinates are flagged and keep the building from being saved. */
    @Test
    fun invalidCoordinatesAreNotSaved() = runUnitTest {
        val useCases = storedBuildingUseCases()
        val store = BuildingDetailScreenStore(
            state = BuildingDetailScreenState(),
            router = router,
            buildingUseCases = useCases.buildingUseCases,
            buildingEquipmentUseCases = useCases.buildingEquipmentUseCases,
            equipmentUseCases = useCases.equipmentUseCases,
            buildingMaterialUseCases = useCases.buildingMaterialUseCases,
            materialUseCases = useCases.materialUseCases,
            buildingUuid = FakeData.building.uuid.toString()
        )
        store.stateFlow.first { it.building != null }
        store.send(action = BuildingDetailScreenAction.Edit)

        listOf(
            ("91" to "0") to (true to false),
            ("0" to "-181") to (false to true),
            ("north" to "0") to (true to false),
            ("" to "0") to (true to false),
            ("0" to "") to (false to true)
        ).forEach { (coordinates, errors) ->
            store.send(action = BuildingDetailScreenAction.ChangeLatitude(latitude = coordinates.first))
            store.send(action = BuildingDetailScreenAction.ChangeLongitude(longitude = coordinates.second))
            assertEquals(expected = errors, actual = store.state.latitudeError to store.state.longitudeError, message = "$coordinates")

            store.send(action = BuildingDetailScreenAction.Save)
            assertTrue(actual = store.state.editing, message = "$coordinates")
        }
        assertEquals(expected = FakeData.building, actual = useCases.buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).first())
    }

    /** Verifies that clearing both coordinates saves the building without a location. */
    @Test
    fun clearedCoordinatesAreSavedAsMissing() = runUnitTest {
        val useCases = storedBuildingUseCases()
        val store = BuildingDetailScreenStore(
            state = BuildingDetailScreenState(),
            router = router,
            buildingUseCases = useCases.buildingUseCases,
            buildingEquipmentUseCases = useCases.buildingEquipmentUseCases,
            equipmentUseCases = useCases.equipmentUseCases,
            buildingMaterialUseCases = useCases.buildingMaterialUseCases,
            materialUseCases = useCases.materialUseCases,
            buildingUuid = FakeData.building.uuid.toString()
        )
        store.stateFlow.first { it.building != null }
        store.send(action = BuildingDetailScreenAction.Edit)
        store.send(action = BuildingDetailScreenAction.ChangeLatitude(latitude = ""))
        store.send(action = BuildingDetailScreenAction.ChangeLongitude(longitude = ""))
        store.send(action = BuildingDetailScreenAction.Save)
        store.stateFlow.first { !it.editing }

        val saved = useCases.buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).first { it?.latitude == null }
        assertNull(actual = saved?.longitude)
    }

    /** Verifies that filling the coordinates, such as from the device location, rounds them to six decimal places. */
    @Test
    fun filledCoordinatesAreRounded() = runUnitTest {
        val useCases = storedBuildingUseCases()
        val store = BuildingDetailScreenStore(
            state = BuildingDetailScreenState(),
            router = router,
            buildingUseCases = useCases.buildingUseCases,
            buildingEquipmentUseCases = useCases.buildingEquipmentUseCases,
            equipmentUseCases = useCases.equipmentUseCases,
            buildingMaterialUseCases = useCases.buildingMaterialUseCases,
            materialUseCases = useCases.materialUseCases,
            buildingUuid = FakeData.building.uuid.toString()
        )
        store.stateFlow.first { it.building != null }
        store.send(action = BuildingDetailScreenAction.Edit)
        store.send(action = BuildingDetailScreenAction.ChangeCoordinates(latitude = 41.123456789, longitude = -8.987654321))

        store.stateFlow.first { it.latitude == "41.123457" && it.longitude == "-8.987654" }
    }

    /** Verifies that the use-my-location button is hidden when the device can't provide a location. */
    @Test
    fun useLocationHiddenWithoutProvider() = runUITest {
        val useCases = storedBuildingUseCases()
        val (store, _) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.building != null }
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithTag(testTag = "building_latitude", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag(testTag = "building_use_location").assertDoesNotExist()
    }

    /**
     * Builds the building detail store of [FakeData.building] with [useCases].
     *
     * @param useCases The use cases backing the store.
     * @return The [BuildingDetailScreenStore].
     */
    private fun buildingStore(useCases: UseCases): BuildingDetailScreenStore = BuildingDetailScreenStore(
        state = BuildingDetailScreenState(),
        router = router,
        buildingUseCases = useCases.buildingUseCases,
        buildingEquipmentUseCases = useCases.buildingEquipmentUseCases,
        equipmentUseCases = useCases.equipmentUseCases,
        buildingMaterialUseCases = useCases.buildingMaterialUseCases,
        materialUseCases = useCases.materialUseCases,
        buildingUuid = FakeData.building.uuid.toString()
    )

    /** Verifies that an equipment ticked in the picker and confirmed is listed, and linked to the building once saved. */
    @Test
    fun addEquipment() = runUITest {
        val useCases = storedBuildingUseCases()
        useCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)
        val (store, sessionStore) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.building != null && store.state.allEquipments.isNotEmpty() && sessionStore.state.account != null }
        onNodeWithTag(testTag = "building_equipments").assertIsDisplayed()
        onNodeWithTag(testTag = "building_equipments_link").assertDoesNotExist()

        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithTag(testTag = "building_equipments_link").performClick()
        waitUntil { store.state.equipmentPickerOpen }
        onNodeWithTag(testTag = "equipment_row_0").performClick()
        waitUntil { store.state.pickedEquipments.isNotEmpty() }
        onNode(matcher = hasTestTag(testTag = "action_save") and hasAnyAncestor(matcher = hasTestTag(testTag = "equipment_picker"))).performClick()
        waitUntil { !store.state.equipmentPickerOpen && store.state.equipments.isNotEmpty() }
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { !store.state.editing }

        val linked = useCases.buildingEquipmentUseCases.observeEquipmentsInBuilding(buildingUuid = FakeData.building.uuid).first { it.isNotEmpty() }
        assertEquals(expected = listOf(element = FakeData.equipment), actual = linked)
        onNodeWithText(text = FakeData.equipment.name).assertIsDisplayed()
    }

    /** Verifies that unlinking an equipment and canceling keeps it linked to the building. */
    @Test
    fun removeEquipmentThenCancel() = runUnitTest {
        val useCases = storedBuildingUseCases()
        useCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)
        useCases.buildingEquipmentUseCases.upsertBuildingEquipment(buildingEquipment = FakeData.buildingEquipment)
        val store = buildingStore(useCases = useCases)

        store.stateFlow.first { it.building != null && it.equipments.isNotEmpty() }
        store.send(action = BuildingDetailScreenAction.Edit)
        store.send(action = BuildingDetailScreenAction.RemoveEquipment(equipmentUuid = FakeData.equipment.uuid))
        assertTrue(actual = store.state.equipments.isEmpty())
        store.send(action = BuildingDetailScreenAction.Cancel)

        assertEquals(expected = listOf(element = FakeData.equipment), actual = store.stateFlow.first { !it.editing }.equipments)
        assertEquals(expected = listOf(element = FakeData.equipment), actual = useCases.buildingEquipmentUseCases.observeEquipmentsInBuilding(buildingUuid = FakeData.building.uuid).first())
    }

    /** Verifies that unlinking an equipment and saving deletes its building equipment. */
    @Test
    fun removeEquipmentThenSave() = runUnitTest {
        val useCases = storedBuildingUseCases()
        useCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)
        useCases.buildingEquipmentUseCases.upsertBuildingEquipment(buildingEquipment = FakeData.buildingEquipment)
        val store = buildingStore(useCases = useCases)

        store.stateFlow.first { it.building != null && it.equipments.isNotEmpty() }
        store.send(action = BuildingDetailScreenAction.Edit)
        store.send(action = BuildingDetailScreenAction.RemoveEquipment(equipmentUuid = FakeData.equipment.uuid))
        store.send(action = BuildingDetailScreenAction.Save)
        store.stateFlow.first { !it.editing }

        assertTrue(actual = useCases.buildingEquipmentUseCases.observeEquipmentsInBuilding(buildingUuid = FakeData.building.uuid).first { it.isEmpty() }.isEmpty())
        assertTrue(actual = store.stateFlow.first { it.storedEquipments.isEmpty() }.equipments.isEmpty())
    }

    /** Verifies that a user without building and building equipment write permission sees the linked equipments, without being able to change them. */
    @Test
    fun readOnlyEquipments() = runUITest {
        val useCases = storedBuildingUseCases(credentials = FakeData.credentials)
        useCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)
        useCases.buildingEquipmentUseCases.upsertBuildingEquipment(buildingEquipment = FakeData.buildingEquipment)
        val (store, sessionStore) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.equipments.isNotEmpty() && sessionStore.state.user?.name == FakeData.user.name }
        onNodeWithText(text = FakeData.equipment.name).assertIsDisplayed()
        onNodeWithTag(testTag = "building_equipments_remove_${FakeData.equipment.uuid}").assertDoesNotExist()
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
    }

    /** Verifies that saving after only linking an equipment saves the building equipment and leaves the unchanged building as stored. */
    @Test
    fun equipmentOnlyEditLeavesBuildingUnchanged() = runUnitTest {
        val useCases = storedBuildingUseCases()
        useCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)
        val store = buildingStore(useCases = useCases)

        store.stateFlow.first { it.storedBuilding != null && it.allEquipments.isNotEmpty() }
        store.send(action = BuildingDetailScreenAction.Edit)
        store.send(action = BuildingDetailScreenAction.OpenEquipmentPicker)
        store.send(action = BuildingDetailScreenAction.ToggleEquipmentPick(equipmentUuid = FakeData.equipment.uuid))
        store.send(action = BuildingDetailScreenAction.SaveEquipmentPick)
        store.send(action = BuildingDetailScreenAction.Save)
        store.stateFlow.first { !it.editing }

        assertEquals(expected = listOf(element = FakeData.equipment), actual = useCases.buildingEquipmentUseCases.observeEquipmentsInBuilding(buildingUuid = FakeData.building.uuid).first { it.isNotEmpty() })
        assertEquals(expected = FakeData.building, actual = useCases.buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).first())
    }

    /** Verifies that the picker opens with the linked equipments ticked, ticks and unticks any equipment, sets the linked equipments to the ticked ones when saved, and clears the ticks when closed. */
    @Test
    fun pickerEditsLinkedEquipments() = runUnitTest {
        val useCases = storedBuildingUseCases()
        val other = FakeData.equipment.copy(uuid = uuid(), name = "Other")
        useCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)
        useCases.equipmentUseCases.upsertEquipment(equipment = other)
        useCases.buildingEquipmentUseCases.upsertBuildingEquipment(buildingEquipment = FakeData.buildingEquipment)
        val store = buildingStore(useCases = useCases)

        store.stateFlow.first { it.equipments.isNotEmpty() && it.allEquipments.size == 2 }
        store.send(action = BuildingDetailScreenAction.Edit)
        store.send(action = BuildingDetailScreenAction.OpenEquipmentPicker)
        assertEquals(expected = setOf(FakeData.equipment.uuid), actual = store.state.pickedEquipments.toSet())
        assertEquals(expected = setOf(FakeData.equipment.uuid.toString()), actual = store.state.equipmentPickerItems.filter { it.checked == true }.map { it.uuid }.toSet())

        store.send(action = BuildingDetailScreenAction.ToggleEquipmentPick(equipmentUuid = other.uuid))
        store.send(action = BuildingDetailScreenAction.CloseEquipmentPicker)
        assertFalse(actual = store.state.equipmentPickerOpen)
        assertEquals(expected = listOf(element = FakeData.equipment), actual = store.state.equipments)

        store.send(action = BuildingDetailScreenAction.OpenEquipmentPicker)
        store.send(action = BuildingDetailScreenAction.ToggleEquipmentPick(equipmentUuid = FakeData.equipment.uuid))
        store.send(action = BuildingDetailScreenAction.ToggleEquipmentPick(equipmentUuid = other.uuid))
        store.send(action = BuildingDetailScreenAction.SaveEquipmentPick)
        assertFalse(actual = store.state.equipmentPickerOpen)
        assertEquals(expected = listOf(element = other), actual = store.state.equipments)

        store.send(action = BuildingDetailScreenAction.Save)
        store.stateFlow.first { !it.editing }
        assertEquals(expected = listOf(element = other), actual = useCases.buildingEquipmentUseCases.observeEquipmentsInBuilding(buildingUuid = FakeData.building.uuid).first { it == listOf(other) })
    }

    /** Verifies that the equipment section is hidden when the building equipments flag is disabled. */
    @Test
    fun equipmentSectionHiddenWhenFlagDisabled() = runUITest {
        ClientFlags.set { it.copy(buildingEquipments = false) }
        val useCases = storedBuildingUseCases()
        val (store, sessionStore) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.building != null && sessionStore.state.account != null }
        onNodeWithTag(testTag = "building_equipments").assertDoesNotExist()
    }

    /** Verifies that the equipment section disappears when the building equipments flag is disabled while the screen is showing. */
    @Test
    fun equipmentSectionHidesWhenFlagIsDisabledWhileShowing() = runUITest {
        val useCases = storedBuildingUseCases()
        val (store, sessionStore) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.building != null && sessionStore.state.account != null }
        onNodeWithTag(testTag = "building_equipments").assertExists()

        ClientFlags.set { it.copy(buildingEquipments = false) }
        waitForIdle()

        onNodeWithTag(testTag = "building_equipments").assertDoesNotExist()
    }

    /** Verifies that the picker applies the equipment list filters, searching and leaving out the deleted equipments. */
    @Test
    fun pickerAppliesEquipmentFilters() = runUnitTest {
        val useCases = storedBuildingUseCases()
        val other = FakeData.equipment.copy(uuid = uuid(), name = "Pump")
        useCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)
        useCases.equipmentUseCases.upsertEquipment(equipment = other)
        useCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment.copy(uuid = uuid(), name = "Pump gone", deletedAt = now()))
        AppFile.EquipmentPreferences.save { equipmentFilterDefaults.copy(search = "pump", showDeleted = true) }
        val store = buildingStore(useCases = useCases)

        val state = store.stateFlow.first { it.allEquipments.size == 2 && it.equipmentFilters.search == "pump" }
        assertEquals(expected = listOf(other.uuid.toString()), actual = state.equipmentPickerItems.map { it.uuid })
    }

    /** [FakeData.material] as linked to [FakeData.building] by [FakeData.buildingMaterial]. */
    private val storedMaterialLink = MaterialLink(material = FakeData.material, quantity = FakeData.buildingMaterial.quantity.toString())

    /** Verifies that a material ticked in the picker and confirmed is listed with a quantity of one, and linked to the building with the typed quantity once saved. */
    @Test
    fun addMaterial() = runUITest {
        val useCases = storedBuildingUseCases()
        useCases.materialUseCases.upsertMaterial(material = FakeData.material)
        val (store, sessionStore) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.building != null && store.state.allMaterials.isNotEmpty() && sessionStore.state.account != null }
        onNodeWithTag(testTag = "building_materials").assertIsDisplayed()
        onNodeWithTag(testTag = "building_materials_link").assertDoesNotExist()

        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithTag(testTag = "building_materials_link").performScrollTo().performClick()
        waitUntil { store.state.materialPickerOpen }
        onNodeWithTag(testTag = "material_row_0").performClick()
        waitUntil { store.state.pickedMaterials.isNotEmpty() }
        onNode(matcher = hasTestTag(testTag = "action_save") and hasAnyAncestor(matcher = hasTestTag(testTag = "material_picker"))).performClick()
        waitUntil { !store.state.materialPickerOpen && store.state.materials.isNotEmpty() }
        assertEquals(expected = listOf(element = MaterialLink(material = FakeData.material, quantity = "1")), actual = store.state.materials)
        onNodeWithTag(testTag = "building_materials_quantity_${FakeData.material.uuid}").performScrollTo().performTextReplacement(text = "5")
        waitUntil { store.state.materials.single().quantity == "5" }
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { !store.state.editing }

        val linked = useCases.buildingMaterialUseCases.observeMaterialsInBuilding(buildingUuid = FakeData.building.uuid).first { it.isNotEmpty() }
        assertEquals(expected = listOf(element = MaterialInBuilding(material = FakeData.material, quantity = 5)), actual = linked)
        onNodeWithText(text = FakeData.material.name).assertIsDisplayed()
    }

    /** Verifies that a changed quantity is discarded when canceled, keeps the building being edited while it is invalid, and is saved to the building material otherwise, leaving the unchanged building as stored. */
    @Test
    fun changeMaterialQuantity() = runUnitTest {
        val useCases = storedBuildingUseCases()
        useCases.materialUseCases.upsertMaterial(material = FakeData.material)
        useCases.buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial)
        val store = buildingStore(useCases = useCases)

        store.stateFlow.first { it.storedBuilding != null && it.materials.isNotEmpty() }
        store.send(action = BuildingDetailScreenAction.Edit)
        store.send(action = BuildingDetailScreenAction.ChangeMaterialQuantity(materialUuid = FakeData.material.uuid, quantity = "8"))
        store.send(action = BuildingDetailScreenAction.Cancel)
        assertEquals(expected = listOf(element = storedMaterialLink), actual = store.stateFlow.first { !it.editing }.materials)

        store.send(action = BuildingDetailScreenAction.Edit)
        listOf("", "0", "-2", "1.5", "many").forEach { quantity ->
            store.send(action = BuildingDetailScreenAction.ChangeMaterialQuantity(materialUuid = FakeData.material.uuid, quantity = quantity))
            assertTrue(actual = store.state.materials.single().quantityError)
            store.send(action = BuildingDetailScreenAction.Save)
            assertTrue(actual = store.state.editing)
        }

        store.send(action = BuildingDetailScreenAction.ChangeMaterialQuantity(materialUuid = FakeData.material.uuid, quantity = "8"))
        assertFalse(actual = store.state.materials.single().quantityError)
        store.send(action = BuildingDetailScreenAction.Save)
        store.stateFlow.first { !it.editing }

        val expected = listOf(element = MaterialInBuilding(material = FakeData.material, quantity = 8))
        assertEquals(expected = expected, actual = useCases.buildingMaterialUseCases.observeMaterialsInBuilding(buildingUuid = FakeData.building.uuid).first { it == expected })
        assertEquals(expected = FakeData.building, actual = useCases.buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).first())
    }

    /** Verifies that unlinking a material and canceling keeps it linked to the building, and unlinking it and saving deletes its building material. */
    @Test
    fun removeMaterial() = runUnitTest {
        val useCases = storedBuildingUseCases()
        useCases.materialUseCases.upsertMaterial(material = FakeData.material)
        useCases.buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial)
        val store = buildingStore(useCases = useCases)

        store.stateFlow.first { it.building != null && it.materials.isNotEmpty() }
        store.send(action = BuildingDetailScreenAction.Edit)
        store.send(action = BuildingDetailScreenAction.RemoveMaterial(materialUuid = FakeData.material.uuid))
        assertTrue(actual = store.state.materials.isEmpty())
        store.send(action = BuildingDetailScreenAction.Cancel)
        assertEquals(expected = listOf(element = storedMaterialLink), actual = store.stateFlow.first { !it.editing }.materials)

        store.send(action = BuildingDetailScreenAction.Edit)
        store.send(action = BuildingDetailScreenAction.RemoveMaterial(materialUuid = FakeData.material.uuid))
        store.send(action = BuildingDetailScreenAction.Save)
        store.stateFlow.first { !it.editing }

        assertTrue(actual = useCases.buildingMaterialUseCases.observeMaterialsInBuilding(buildingUuid = FakeData.building.uuid).first { it.isEmpty() }.isEmpty())
        assertTrue(actual = store.stateFlow.first { it.storedMaterials.isEmpty() }.materials.isEmpty())
    }

    /** Verifies that a user without building and building material write permission sees the linked materials and their quantities, without being able to change them. */
    @Test
    fun readOnlyMaterials() = runUITest {
        val useCases = storedBuildingUseCases(credentials = FakeData.credentials)
        useCases.materialUseCases.upsertMaterial(material = FakeData.material)
        useCases.buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial)
        val (store, sessionStore) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.materials.isNotEmpty() && sessionStore.state.user?.name == FakeData.user.name }
        onNodeWithText(text = FakeData.material.name).assertIsDisplayed()
        onNodeWithTag(testTag = "building_materials_quantity_${FakeData.material.uuid}", useUnmergedTree = true).assertTextEquals(FakeData.buildingMaterial.quantity.toString())
        onNodeWithTag(testTag = "building_materials_remove_${FakeData.material.uuid}").assertDoesNotExist()
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
    }

    /** Verifies that the picker opens with the linked materials ticked, keeps the quantity typed for a material that stays ticked, links a newly ticked material with a quantity of one, and clears the ticks when closed. */
    @Test
    fun pickerEditsLinkedMaterials() = runUnitTest {
        val useCases = storedBuildingUseCases()
        val other = FakeData.material.copy(uuid = uuid(), name = "Other")
        useCases.materialUseCases.upsertMaterial(material = FakeData.material)
        useCases.materialUseCases.upsertMaterial(material = other)
        useCases.buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial)
        val store = buildingStore(useCases = useCases)

        store.stateFlow.first { it.materials.isNotEmpty() && it.allMaterials.size == 2 }
        store.send(action = BuildingDetailScreenAction.Edit)
        store.send(action = BuildingDetailScreenAction.ChangeMaterialQuantity(materialUuid = FakeData.material.uuid, quantity = "8"))
        store.send(action = BuildingDetailScreenAction.OpenMaterialPicker)
        assertEquals(expected = setOf(FakeData.material.uuid), actual = store.state.pickedMaterials.toSet())
        assertEquals(expected = setOf(FakeData.material.uuid.toString()), actual = store.state.materialPickerItems.filter { it.checked == true }.map { it.uuid }.toSet())

        store.send(action = BuildingDetailScreenAction.ToggleMaterialPick(materialUuid = other.uuid))
        store.send(action = BuildingDetailScreenAction.CloseMaterialPicker)
        assertFalse(actual = store.state.materialPickerOpen)
        assertTrue(actual = store.state.pickedMaterials.isEmpty())

        store.send(action = BuildingDetailScreenAction.OpenMaterialPicker)
        store.send(action = BuildingDetailScreenAction.ToggleMaterialPick(materialUuid = other.uuid))
        store.send(action = BuildingDetailScreenAction.SaveMaterialPick)
        assertFalse(actual = store.state.materialPickerOpen)
        assertEquals(
            expected = setOf(MaterialLink(material = FakeData.material, quantity = "8"), MaterialLink(material = other, quantity = "1")),
            actual = store.state.materials.toSet()
        )

        store.send(action = BuildingDetailScreenAction.OpenMaterialPicker)
        store.send(action = BuildingDetailScreenAction.ToggleMaterialPick(materialUuid = FakeData.material.uuid))
        store.send(action = BuildingDetailScreenAction.SaveMaterialPick)
        assertEquals(expected = listOf(element = MaterialLink(material = other, quantity = "1")), actual = store.state.materials)
    }

    /** Verifies that the material section is hidden when the building materials flag is disabled. */
    @Test
    fun materialSectionHiddenWhenFlagDisabled() = runUITest {
        ClientFlags.set { it.copy(buildingMaterials = false) }
        val useCases = storedBuildingUseCases()
        val (store, sessionStore) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.building != null && sessionStore.state.account != null }
        onNodeWithTag(testTag = "building_equipments").assertExists()
        onNodeWithTag(testTag = "building_materials").assertDoesNotExist()
    }

    /** Verifies that the picker applies the material list filters, searching and leaving out the deleted materials. */
    @Test
    fun pickerAppliesMaterialFilters() = runUnitTest {
        val useCases = storedBuildingUseCases()
        val other = FakeData.material.copy(uuid = uuid(), name = "Copper Cable")
        val deleted = FakeData.material.copy(uuid = uuid(), name = "Copper Pipe", deletedAt = now())
        listOf(FakeData.material, other, deleted).forEach { useCases.materialUseCases.upsertMaterial(material = it) }
        AppFile.MaterialPreferences.save { materialFilterDefaults.copy(search = "copper", showDeleted = true) }
        val store = buildingStore(useCases = useCases)

        val state = store.stateFlow.first { it.allMaterials.size == 2 && it.materialFilters.search == "copper" }
        assertEquals(expected = listOf(other.uuid.toString()), actual = state.materialPickerItems.map { it.uuid })
    }
}
