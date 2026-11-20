package com.app.builder.ui.navigation.building

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.collections.immutable.persistentListOf
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
import com.app.builder.domain.Equipment
import com.app.builder.domain.Material
import com.app.builder.domain.User
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.gateway.UseCases
import com.app.builder.domain.gateway.buildingmaterial.MaterialInBuilding
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.equipment.equipmentLinkPickerStore
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.link.Link
import com.app.builder.ui.component.link.LinkPermissions
import com.app.builder.ui.component.link.LinkPickerStore
import com.app.builder.ui.component.list.EquipmentItem
import com.app.builder.ui.component.list.MaterialItem
import com.app.builder.ui.component.list.UserItem
import com.app.builder.ui.component.material.materialLinkPickerStore
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.component.user.userLinkPickerStore
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen

class BuildingDetailScreenTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(http = true, scheduler = true, buildings = true, buildingEquipments = true, buildingMaterials = true, buildingUsers = true) }
    }

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
     * The stores of a rendered building detail screen.
     *
     * @property store The building detail store.
     * @property sessionStore The session store.
     * @property equipmentPickerStore The equipment picker store.
     * @property materialPickerStore The material picker store.
     * @property userPickerStore The user picker store.
     */
    private data class BuildingStores(
        val store: BuildingDetailScreenStore,
        val sessionStore: SessionStore,
        val equipmentPickerStore: LinkPickerStore<Equipment, EquipmentItem>,
        val materialPickerStore: LinkPickerStore<Material, MaterialItem>,
        val userPickerStore: LinkPickerStore<User, UserItem>,
    )

    /** [FakeData.equipment] as linked to [FakeData.building] by [FakeData.buildingEquipment]. */
    private val storedEquipmentLink = Link(uuid = FakeData.equipment.uuid, name = FakeData.equipment.name)

    /** [FakeData.user] as linked to [FakeData.building] by [FakeData.buildingUser]. */
    private val storedUserLink = Link(uuid = FakeData.user.uuid, name = FakeData.user.name)

    /**
     * Renders the building detail screen of the building with [buildingUuid] with an action bar offering every building detail action, as the building entry provider does.
     *
     * @param useCases The use cases backing the stores.
     * @param buildingUuid The uuid of the building to display.
     * @param new Whether the building is new and not stored yet.
     * @return The stores rendered.
     */
    private fun ComposeUiTest.setBuildingDetailScreen(useCases: UseCases, buildingUuid: Uuid = FakeData.building.uuid, new: Boolean = false): BuildingStores {
        val store = BuildingDetailScreenStore(
            state = BuildingDetailScreenState(),
            router = router,
            buildingUseCases = useCases.buildingUseCases,
            buildingEquipmentUseCases = useCases.buildingEquipmentUseCases,
            buildingMaterialUseCases = useCases.buildingMaterialUseCases,
            buildingUserUseCases = useCases.buildingUserUseCases,
            authenticationUseCases = useCases.authenticationUseCases,
            buildingUuid = buildingUuid.toString(),
            new = new
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = useCases.authenticationUseCases)
        val filtersStore = FiltersStore(state = FiltersState())
        val equipmentFiltersStore = FiltersStore(state = FiltersState())
        val equipmentPickerStore = equipmentLinkPickerStore(equipmentUseCases = useCases.equipmentUseCases)
        val materialFiltersStore = FiltersStore(state = FiltersState())
        val materialPickerStore = materialLinkPickerStore(materialUseCases = useCases.materialUseCases)
        val userFiltersStore = FiltersStore(state = FiltersState())
        val userPickerStore = userLinkPickerStore(userUseCases = useCases.userUseCases)

        setUI {
            BuildingDetailScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                equipmentFiltersStore = equipmentFiltersStore,
                equipmentPickerStore = equipmentPickerStore,
                materialFiltersStore = materialFiltersStore,
                materialPickerStore = materialPickerStore,
                userFiltersStore = userFiltersStore,
                userPickerStore = userPickerStore,
                store = store,
            )
        }

        return BuildingStores(store = store, sessionStore = sessionStore, equipmentPickerStore = equipmentPickerStore, materialPickerStore = materialPickerStore, userPickerStore = userPickerStore)
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
            buildingMaterialUseCases = useCases.buildingMaterialUseCases,
            buildingUserUseCases = useCases.buildingUserUseCases,
            authenticationUseCases = useCases.authenticationUseCases,
            buildingUuid = FakeData.building.uuid.toString()
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = useCases.authenticationUseCases)
        val filtersStore = FiltersStore(state = FiltersState())
        val equipmentFiltersStore = FiltersStore(state = FiltersState())
        val equipmentPickerStore = equipmentLinkPickerStore(equipmentUseCases = useCases.equipmentUseCases)
        val materialFiltersStore = FiltersStore(state = FiltersState())
        val materialPickerStore = materialLinkPickerStore(materialUseCases = useCases.materialUseCases)
        val userFiltersStore = FiltersStore(state = FiltersState())
        val userPickerStore = userLinkPickerStore(userUseCases = useCases.userUseCases)

        setUI {
            BuildingDetailScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                equipmentFiltersStore = equipmentFiltersStore,
                equipmentPickerStore = equipmentPickerStore,
                materialFiltersStore = materialFiltersStore,
                materialPickerStore = materialPickerStore,
                userFiltersStore = userFiltersStore,
                userPickerStore = userPickerStore,
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

        waitUntil { store.state.building != null && sessionStore.state.user?.name == FakeData.user.name && store.state.equipmentLinks.canRead }
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

        waitUntil { sessionStore.state.account != null && store.state.writable }
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

        waitUntil { store.state.building != null && store.state.writable }
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

        waitUntil { store.state.building != null && store.state.writable }
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

        waitUntil { store.state.building != null && store.state.writable }
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
            buildingMaterialUseCases = useCases.buildingMaterialUseCases,
            buildingUserUseCases = useCases.buildingUserUseCases,
            authenticationUseCases = useCases.authenticationUseCases,
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
            buildingMaterialUseCases = useCases.buildingMaterialUseCases,
            buildingUserUseCases = useCases.buildingUserUseCases,
            authenticationUseCases = useCases.authenticationUseCases,
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
            buildingMaterialUseCases = useCases.buildingMaterialUseCases,
            buildingUserUseCases = useCases.buildingUserUseCases,
            authenticationUseCases = useCases.authenticationUseCases,
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
        buildingMaterialUseCases = useCases.buildingMaterialUseCases,
        buildingUserUseCases = useCases.buildingUserUseCases,
        authenticationUseCases = useCases.authenticationUseCases,
        buildingUuid = FakeData.building.uuid.toString()
    )

    /** Verifies that an equipment ticked in the picker and confirmed is listed, and linked to the building once saved. */
    @Test
    fun addEquipment() = runUITest {
        val useCases = storedBuildingUseCases()
        useCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)
        val (store, sessionStore, equipmentPickerStore) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.building != null && equipmentPickerStore.state.candidates.isNotEmpty() && sessionStore.state.account != null && store.state.writable }
        onNodeWithTag(testTag = "building_equipments").assertIsDisplayed()
        onNodeWithTag(testTag = "building_equipments_link").assertDoesNotExist()

        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithTag(testTag = "building_equipments_link").performClick()
        waitUntil { equipmentPickerStore.state.open }
        onNodeWithTag(testTag = "equipment_row_0").performClick()
        waitUntil { equipmentPickerStore.state.picked.isNotEmpty() }
        onNode(matcher = hasTestTag(testTag = "action_save") and hasAnyAncestor(matcher = hasTestTag(testTag = "equipment_picker"))).performClick()
        waitUntil { !equipmentPickerStore.state.open && store.state.equipments.isNotEmpty() }
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

        assertEquals(expected = listOf(element = storedEquipmentLink), actual = store.stateFlow.first { !it.editing }.equipments)
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

        waitUntil { store.state.equipments.isNotEmpty() && sessionStore.state.user?.name == FakeData.user.name && store.state.equipmentLinks.canRead }
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

        store.stateFlow.first { it.storedBuilding != null }
        store.send(action = BuildingDetailScreenAction.Edit)
        store.send(action = BuildingDetailScreenAction.SetEquipments(equipments = persistentListOf(storedEquipmentLink)))
        store.send(action = BuildingDetailScreenAction.Save)
        store.stateFlow.first { !it.editing }

        assertEquals(expected = listOf(element = FakeData.equipment), actual = useCases.buildingEquipmentUseCases.observeEquipmentsInBuilding(buildingUuid = FakeData.building.uuid).first { it.isNotEmpty() })
        assertEquals(expected = FakeData.building, actual = useCases.buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).first())
    }

    /** Verifies that setting the linked equipments replaces them, and links and unlinks the building equipments once saved. */
    @Test
    fun setEquipments() = runUnitTest {
        val useCases = storedBuildingUseCases()
        val other = FakeData.equipment.copy(uuid = uuid(), name = "Other")
        useCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)
        useCases.equipmentUseCases.upsertEquipment(equipment = other)
        useCases.buildingEquipmentUseCases.upsertBuildingEquipment(buildingEquipment = FakeData.buildingEquipment)
        val store = buildingStore(useCases = useCases)
        val otherLink = Link(uuid = other.uuid, name = other.name)

        store.stateFlow.first { it.equipments.isNotEmpty() }
        store.send(action = BuildingDetailScreenAction.Edit)
        store.send(action = BuildingDetailScreenAction.SetEquipments(equipments = persistentListOf(otherLink)))
        assertEquals(expected = listOf(element = otherLink), actual = store.state.equipments)

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

        waitUntil { store.state.building != null && sessionStore.state.account != null && store.state.writable }
        onNodeWithTag(testTag = "building_equipments").assertDoesNotExist()
    }

    /** Verifies that the equipment section disappears when the building equipments flag is disabled while the screen is showing. */
    @Test
    fun equipmentSectionHidesWhenFlagIsDisabledWhileShowing() = runUITest {
        val useCases = storedBuildingUseCases()
        val (store, sessionStore) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.building != null && sessionStore.state.account != null && store.state.writable }
        onNodeWithTag(testTag = "building_equipments").assertExists()

        ClientFlags.set { it.copy(buildingEquipments = false) }
        waitForIdle()

        onNodeWithTag(testTag = "building_equipments").assertDoesNotExist()
    }

    /** [FakeData.material] as linked to [FakeData.building] by [FakeData.buildingMaterial]. */
    private val storedMaterialLink = Link(uuid = FakeData.material.uuid, name = FakeData.material.name, quantity = FakeData.buildingMaterial.quantity.toString())

    /** Verifies that a material ticked in the picker and confirmed is listed with a quantity of one, and linked to the building with the typed quantity once saved. */
    @Test
    fun addMaterial() = runUITest {
        val useCases = storedBuildingUseCases()
        useCases.materialUseCases.upsertMaterial(material = FakeData.material)
        val (store, sessionStore, _, materialPickerStore) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.building != null && materialPickerStore.state.candidates.isNotEmpty() && sessionStore.state.account != null && store.state.writable }
        onNodeWithTag(testTag = "building_materials").assertIsDisplayed()
        onNodeWithTag(testTag = "building_materials_link").assertDoesNotExist()

        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithTag(testTag = "building_materials_link").performScrollTo().performClick()
        waitUntil { materialPickerStore.state.open }
        onNodeWithTag(testTag = "material_row_0").performClick()
        waitUntil { materialPickerStore.state.picked.isNotEmpty() }
        onNode(matcher = hasTestTag(testTag = "action_save") and hasAnyAncestor(matcher = hasTestTag(testTag = "material_picker"))).performClick()
        waitUntil { !materialPickerStore.state.open && store.state.materials.isNotEmpty() }
        assertEquals(expected = listOf(element = storedMaterialLink.copy(quantity = "1")), actual = store.state.materials)
        onNodeWithTag(testTag = "building_materials_extra_${FakeData.material.uuid}").performScrollTo().performTextReplacement(text = "5")
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

        waitUntil { store.state.materials.isNotEmpty() && sessionStore.state.user?.name == FakeData.user.name && store.state.materialLinks.canRead }
        onNodeWithText(text = FakeData.material.name).assertIsDisplayed()
        onNodeWithTag(testTag = "building_materials_extra_${FakeData.material.uuid}", useUnmergedTree = true).assertTextEquals(FakeData.buildingMaterial.quantity.toString())
        onNodeWithTag(testTag = "building_materials_remove_${FakeData.material.uuid}").assertDoesNotExist()
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
    }

    /** Verifies that setting the linked materials keeps the quantity typed for a material that stays linked, and links a new material with a quantity of one. */
    @Test
    fun setMaterials() = runUnitTest {
        val useCases = storedBuildingUseCases()
        useCases.materialUseCases.upsertMaterial(material = FakeData.material)
        useCases.buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial)
        val store = buildingStore(useCases = useCases)
        val picked = Link(uuid = FakeData.material.uuid, name = FakeData.material.name)
        val other = Link(uuid = uuid(), name = "Other")

        store.stateFlow.first { it.materials.isNotEmpty() }
        store.send(action = BuildingDetailScreenAction.Edit)
        store.send(action = BuildingDetailScreenAction.ChangeMaterialQuantity(materialUuid = FakeData.material.uuid, quantity = "8"))
        store.send(action = BuildingDetailScreenAction.SetMaterials(materials = persistentListOf(picked, other)))
        assertEquals(expected = listOf(picked.copy(quantity = "8"), other.copy(quantity = "1")), actual = store.state.materials)

        store.send(action = BuildingDetailScreenAction.SetMaterials(materials = persistentListOf(other)))
        assertEquals(expected = listOf(element = other.copy(quantity = "1")), actual = store.state.materials)
    }

    /** Verifies that the material section is hidden when the building materials flag is disabled. */
    @Test
    fun materialSectionHiddenWhenFlagDisabled() = runUITest {
        ClientFlags.set { it.copy(buildingMaterials = false) }
        val useCases = storedBuildingUseCases()
        val (store, sessionStore) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.building != null && sessionStore.state.account != null && store.state.writable }
        onNodeWithTag(testTag = "building_equipments").assertExists()
        onNodeWithTag(testTag = "building_materials").assertDoesNotExist()
    }

    /** Verifies that the linked users are listed, and selecting one opens it. */
    @Test
    fun usersAreListed() = runUITest {
        val useCases = storedBuildingUseCases()
        useCases.userUseCases.upsertUser(user = FakeData.user)
        useCases.buildingUserUseCases.upsertBuildingUser(buildingUser = FakeData.buildingUser)
        val (store, sessionStore) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.users.isNotEmpty() && sessionStore.state.account != null && store.state.writable }
        onNodeWithTag(testTag = "building_users").assertExists()
        onNodeWithTag(testTag = "building_users_${FakeData.user.uuid}").performScrollTo().performClick()

        waitUntil { router.backStack.lastOrNull() == Screen.UserDetail(uuid = FakeData.user.uuid.toString()) }
    }

    /** Verifies that a user ticked in the picker and confirmed is listed, and linked to the building once saved. */
    @Test
    fun addUser() = runUITest {
        val useCases = storedBuildingUseCases()
        val (store, sessionStore, _, _, userPickerStore) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.building != null && userPickerStore.state.candidates.isNotEmpty() && sessionStore.state.account != null && store.state.writable }
        onNodeWithTag(testTag = "building_users").assertExists()
        onNodeWithTag(testTag = "building_users_link").assertDoesNotExist()

        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithTag(testTag = "building_users_link").performScrollTo().performClick()
        waitUntil { userPickerStore.state.open }
        onNodeWithTag(testTag = "user_row_0").performClick()
        waitUntil { userPickerStore.state.picked.isNotEmpty() }
        onNode(matcher = hasTestTag(testTag = "action_save") and hasAnyAncestor(matcher = hasTestTag(testTag = "user_picker"))).performClick()
        waitUntil { !userPickerStore.state.open && store.state.users.isNotEmpty() }
        val picked = store.state.users.map { it.uuid }
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { !store.state.editing }

        val linked = useCases.buildingUserUseCases.observeUsersInBuilding(buildingUuid = FakeData.building.uuid).first { it.isNotEmpty() }
        assertEquals(expected = picked, actual = linked.map { it.uuid })
    }

    /** Verifies that unlinking a user and canceling keeps it linked to the building, and unlinking it and saving deletes its building user and leaves the unchanged building as stored. */
    @Test
    fun removeUser() = runUnitTest {
        val useCases = storedBuildingUseCases()
        useCases.userUseCases.upsertUser(user = FakeData.user)
        useCases.buildingUserUseCases.upsertBuildingUser(buildingUser = FakeData.buildingUser)
        val store = buildingStore(useCases = useCases)

        store.stateFlow.first { it.storedBuilding != null && it.users.isNotEmpty() }
        store.send(action = BuildingDetailScreenAction.Edit)
        store.send(action = BuildingDetailScreenAction.RemoveUser(userUuid = FakeData.user.uuid))
        assertTrue(actual = store.state.users.isEmpty())
        store.send(action = BuildingDetailScreenAction.Cancel)
        assertEquals(expected = listOf(element = storedUserLink), actual = store.stateFlow.first { !it.editing }.users)

        store.send(action = BuildingDetailScreenAction.Edit)
        store.send(action = BuildingDetailScreenAction.RemoveUser(userUuid = FakeData.user.uuid))
        store.send(action = BuildingDetailScreenAction.Save)
        store.stateFlow.first { !it.editing }

        assertTrue(actual = useCases.buildingUserUseCases.observeUsersInBuilding(buildingUuid = FakeData.building.uuid).first { it.isEmpty() }.isEmpty())
        assertTrue(actual = store.stateFlow.first { it.storedUsers.isEmpty() }.users.isEmpty())
        assertEquals(expected = FakeData.building, actual = useCases.buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).first())
    }

    /** Verifies that setting the linked users replaces them, and links the building users once saved. */
    @Test
    fun setUsers() = runUnitTest {
        val useCases = storedBuildingUseCases()
        useCases.userUseCases.upsertUser(user = FakeData.user)
        val store = buildingStore(useCases = useCases)

        store.stateFlow.first { it.storedBuilding != null }
        store.send(action = BuildingDetailScreenAction.Edit)
        store.send(action = BuildingDetailScreenAction.SetUsers(users = persistentListOf(storedUserLink)))
        assertEquals(expected = listOf(element = storedUserLink), actual = store.state.users)

        store.send(action = BuildingDetailScreenAction.Save)
        store.stateFlow.first { !it.editing }
        assertEquals(expected = listOf(element = FakeData.user), actual = useCases.buildingUserUseCases.observeUsersInBuilding(buildingUuid = FakeData.building.uuid).first { it.isNotEmpty() })
    }

    /** Verifies that a user without building and building user write permission sees the linked users, without being able to change them. */
    @Test
    fun readOnlyUsers() = runUITest {
        val useCases = storedBuildingUseCases(credentials = FakeData.credentials)
        useCases.userUseCases.upsertUser(user = FakeData.user)
        useCases.buildingUserUseCases.upsertBuildingUser(buildingUser = FakeData.buildingUser)
        val (store, sessionStore) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.users.isNotEmpty() && sessionStore.state.user?.name == FakeData.user.name && store.state.userLinks.canRead }
        onNodeWithTag(testTag = "building_users_${FakeData.user.uuid}").assertExists()
        onNodeWithTag(testTag = "building_users_remove_${FakeData.user.uuid}").assertDoesNotExist()
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
    }

    /** Verifies that the user section is hidden when the building users flag is disabled. */
    @Test
    fun userSectionHiddenWhenFlagDisabled() = runUITest {
        ClientFlags.set { it.copy(buildingUsers = false) }
        val useCases = storedBuildingUseCases()
        val (store, sessionStore) = setBuildingDetailScreen(useCases = useCases)

        waitUntil { store.state.building != null && sessionStore.state.account != null && store.state.writable }
        onNodeWithTag(testTag = "building_equipments").assertExists()
        onNodeWithTag(testTag = "building_users").assertDoesNotExist()
    }

    /** Verifies that the state follows what the current account can do: an admin can write the building and edit its links, a user with only read permission can do neither, and the links can't be read while their client feature flag is off. */
    @Test
    fun permissions() = runUnitTest {
        val admin = buildingStore(useCases = storedBuildingUseCases()).stateFlow.first { it.writable }
        assertTrue(actual = admin.let { it.equipmentLinks == LinkPermissions(canRead = true, canEdit = true) && it.materialLinks == LinkPermissions(canRead = true, canEdit = true) && it.userLinks == LinkPermissions(canRead = true, canEdit = true) })

        val reader = buildingStore(useCases = storedBuildingUseCases(credentials = FakeData.credentials)).stateFlow.first { it.equipmentLinks.canRead }
        assertFalse(actual = reader.writable || reader.equipmentLinks.canEdit || reader.userLinks.canEdit)

        ClientFlags.set { it.copy(buildingEquipments = false, buildingMaterials = false, buildingUsers = false) }
        val disabled = buildingStore(useCases = storedBuildingUseCases()).stateFlow.first { it.writable }
        assertTrue(actual = disabled.let { it.equipmentLinks == LinkPermissions() && it.materialLinks == LinkPermissions() && it.userLinks == LinkPermissions() })
    }
}
