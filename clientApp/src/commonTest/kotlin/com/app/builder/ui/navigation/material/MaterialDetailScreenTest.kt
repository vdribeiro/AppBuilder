package com.app.builder.ui.navigation.material

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
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.domain.Building
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.gateway.UseCases
import com.app.builder.domain.gateway.buildingmaterial.BuildingOfMaterial
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.building.buildingLinkPickerStore
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.link.Link
import com.app.builder.ui.component.link.LinkPermissions
import com.app.builder.ui.component.link.LinkPickerStore
import com.app.builder.ui.component.list.BuildingItem
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen

class MaterialDetailScreenTest: TestCase() {

    /**
     * Stores [FakeData.material] as the admin, then logs in with [credentials].
     *
     * @param credentials The credentials to log in with once the material is stored.
     * @return The admin's [UseCases].
     */
    private suspend fun storedMaterialUseCases(credentials: UserCredentials = FakeData.adminCredentials): UseCases {
        val useCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        useCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        useCases.materialUseCases.upsertMaterial(material = FakeData.material)
        if (credentials != FakeData.adminCredentials) useCases.authenticationUseCases.login(credentials = credentials)
        return useCases
    }

    /**
     * Renders the material detail screen of the material with [materialUuid] with an action bar offering every material detail action, as the material entry provider does.
     *
     * @param useCases The use cases backing the stores.
     * @param materialUuid The uuid of the material to display.
     * @param new Whether the material is new and not stored yet.
     * @return The [MaterialDetailScreenStore], the [SessionStore] and the building picker store rendered.
     */
    private fun ComposeUiTest.setMaterialDetailScreen(useCases: UseCases, materialUuid: Uuid = FakeData.material.uuid, new: Boolean = false): Triple<MaterialDetailScreenStore, SessionStore, LinkPickerStore<Building, BuildingItem>> {
        val store = MaterialDetailScreenStore(
            state = MaterialDetailScreenState(),
            router = router,
            materialUseCases = useCases.materialUseCases,
            buildingMaterialUseCases = useCases.buildingMaterialUseCases,
            authenticationUseCases = useCases.authenticationUseCases,
            materialUuid = materialUuid.toString(),
            new = new
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = useCases.authenticationUseCases)
        val filtersStore = FiltersStore(state = FiltersState())
        val buildingFiltersStore = FiltersStore(state = FiltersState())
        val buildingPickerStore = buildingLinkPickerStore(buildingUseCases = useCases.buildingUseCases)

        setUI {
            MaterialDetailScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                buildingFiltersStore = buildingFiltersStore,
                buildingPickerStore = buildingPickerStore,
                store = store,
            )
        }

        return Triple(first = store, second = sessionStore, third = buildingPickerStore)
    }

    /** Verifies that the material detail screen displays the name and description of the loaded material. */
    @Test
    fun materialDetailScreen() = runUITest {
        val useCases = storedMaterialUseCases()

        val store = MaterialDetailScreenStore(
            state = MaterialDetailScreenState(),
            router = router,
            materialUseCases = useCases.materialUseCases,
            buildingMaterialUseCases = useCases.buildingMaterialUseCases,
            authenticationUseCases = useCases.authenticationUseCases,
            materialUuid = FakeData.material.uuid.toString()
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = useCases.authenticationUseCases)
        val filtersStore = FiltersStore(state = FiltersState())
        val buildingFiltersStore = FiltersStore(state = FiltersState())
        val buildingPickerStore = buildingLinkPickerStore(buildingUseCases = useCases.buildingUseCases)

        setUI {
            MaterialDetailScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                buildingFiltersStore = buildingFiltersStore,
                buildingPickerStore = buildingPickerStore,
                store = store,
            )
        }

        assertTrue(actual = router.backStack.isEmpty())
        waitUntil { store.state.material != null }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()

        onNodeWithText(text = "material_name").assertIsDisplayed()
        onNodeWithText(text = FakeData.material.name).assertIsDisplayed()
        onNodeWithText(text = "material_description").assertIsDisplayed()
        onNodeWithText(text = FakeData.material.description).assertIsDisplayed()
    }

    /** Verifies that a user with write permission views the material read only, with the edit and delete buttons available. */
    @Test
    fun viewMaterial() = runUITest {
        val (store, _) = setMaterialDetailScreen(useCases = storedMaterialUseCases())

        waitUntil { store.state.material != null }
        assertFalse(actual = store.state.editing)
        onNodeWithText(text = FakeData.material.name).assert(matcher = !hasSetTextAction())
        onNodeWithText(text = FakeData.material.description).assert(matcher = !hasSetTextAction())
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
        onNodeWithTag(testTag = "action_delete").assertIsDisplayed()
        onNodeWithTag(testTag = "action_save").assertDoesNotExist()
        onNodeWithTag(testTag = "action_cancel").assertDoesNotExist()
    }

    /** Verifies that a user with only read permission views the material read only, without any of the edit buttons. */
    @Test
    fun readOnlyMaterial() = runUITest {
        val (store, sessionStore) = setMaterialDetailScreen(useCases = storedMaterialUseCases(credentials = FakeData.credentials))

        waitUntil { store.state.material != null && sessionStore.state.user?.name == FakeData.user.name && store.state.buildingLinks.canRead }
        onNodeWithText(text = FakeData.material.name).assert(matcher = !hasSetTextAction())
        onNodeWithText(text = FakeData.material.description).assert(matcher = !hasSetTextAction())
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_save").assertDoesNotExist()
        onNodeWithTag(testTag = "action_cancel").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
    }

    /** Verifies that the viewed material follows the changes to the stored material. */
    @Test
    fun viewFollowsMaterial() = runUITest {
        val useCases = storedMaterialUseCases()
        val (store, _) = setMaterialDetailScreen(useCases = useCases)

        waitUntil { store.state.material != null }
        useCases.materialUseCases.upsertMaterial(material = FakeData.material.copy(name = "Remote title"))

        waitUntil { store.state.material?.name == "Remote title" }
        onNodeWithText(text = "Remote title").assertIsDisplayed()
    }

    /** Verifies that the edit button makes the material editable, swaps the buttons and stops following the stored material. */
    @Test
    fun editMaterial() = runUITest {
        val useCases = storedMaterialUseCases()
        val (store, _) = setMaterialDetailScreen(useCases = useCases)

        waitUntil { store.state.material != null }
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }

        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
        onNodeWithTag(testTag = "action_save").assertIsDisplayed()
        onNodeWithTag(testTag = "action_cancel").assertIsDisplayed()

        onNodeWithText(text = FakeData.material.name).assertIsEnabled().performTextReplacement(text = "New title")
        onNodeWithText(text = FakeData.material.description).assertIsEnabled().performTextReplacement(text = "New description")
        waitForIdle()

        useCases.materialUseCases.upsertMaterial(material = FakeData.material.copy(name = "Remote title"))
        useCases.materialUseCases.observeMaterial(uuid = FakeData.material.uuid).first { it?.name == "Remote title" }
        waitForIdle()

        assertEquals(expected = "New title", actual = store.state.material?.name)
        assertEquals(expected = "New description", actual = store.state.material?.description)
    }

    /** Verifies that the save button persists the edits and goes back to viewing the material. */
    @Test
    fun saveMaterial() = runUITest {
        val useCases = storedMaterialUseCases()
        val (store, _) = setMaterialDetailScreen(useCases = useCases)

        waitUntil { store.state.material != null }
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithText(text = FakeData.material.name).performTextReplacement(text = "New title")
        onNodeWithText(text = FakeData.material.description).performTextReplacement(text = "New description")
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { !store.state.editing }

        val saved = useCases.materialUseCases.observeMaterial(uuid = FakeData.material.uuid).first { it?.name == "New title" }
        assertEquals(expected = "New description", actual = saved?.description)
        onNodeWithText(text = "New title").assert(matcher = !hasSetTextAction())
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
        onNodeWithTag(testTag = "action_save").assertDoesNotExist()
    }

    /** Verifies that the cancel button discards the edits, reloads the stored material and goes back to viewing it. */
    @Test
    fun cancelEdit() = runUITest {
        val useCases = storedMaterialUseCases()
        val (store, _) = setMaterialDetailScreen(useCases = useCases)

        waitUntil { store.state.material != null }
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithText(text = FakeData.material.name).performTextReplacement(text = "New title")
        onNodeWithTag(testTag = "action_cancel").performClick()
        waitUntil { !store.state.editing }

        waitUntil { store.state.material?.name == FakeData.material.name }
        onNodeWithText(text = FakeData.material.name).assert(matcher = !hasSetTextAction())
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
        onNodeWithTag(testTag = "action_cancel").assertDoesNotExist()
        val stored = useCases.materialUseCases.observeMaterial(uuid = FakeData.material.uuid).first()
        assertEquals(expected = FakeData.material.name, actual = stored?.name)
    }

    /** Verifies that the delete action soft deletes the material, bumping its modification time, and navigates back. */
    @Test
    fun deleteMaterial() = runUITest {
        val useCases = storedMaterialUseCases()

        router.navigate(screen = Screen.MaterialList, option = Router.NavOption.ADD)
        router.navigate(screen = Screen.MaterialDetail(uuid = FakeData.material.uuid.toString()), option = Router.NavOption.ADD)

        val (store, _) = setMaterialDetailScreen(useCases = useCases)

        waitUntil { store.state.material != null }
        onNodeWithTag(testTag = "action_delete").assertIsDisplayed().performClick()
        waitForIdle()

        assertEquals(expected = listOf<Any>(Screen.MaterialList), actual = router.backStack.toList())
        val deleted = useCases.materialUseCases.observeMaterial(uuid = FakeData.material.uuid).first()
        assertNotNull(actual = deleted?.deletedAt)
        assertEquals(expected = deleted.deletedAt, actual = deleted.modifiedAt)
    }

    /** Verifies that a deleted material hides edit, and that its delete toggle restores it, clearing its deletion, bumping its modification time, keeping viewing it and showing edit again. */
    @Test
    fun restoreMaterial() = runUITest {
        val useCases = storedMaterialUseCases()
        val deletedMaterial = FakeData.material.copy(deletedAt = now())
        useCases.materialUseCases.upsertMaterial(material = deletedMaterial)
        val (store, _) = setMaterialDetailScreen(useCases = useCases)

        waitUntil { store.state.material?.deletedAt != null }
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertIsDisplayed().performClick()
        waitUntil { store.state.material?.deletedAt == null }

        val restored = useCases.materialUseCases.observeMaterial(uuid = FakeData.material.uuid).first { it?.deletedAt == null }
        assertTrue(actual = restored!!.modifiedAt > deletedMaterial.modifiedAt)
        assertTrue(actual = router.backStack.isEmpty())
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
        onNodeWithTag(testTag = "action_delete").assertIsDisplayed()
    }

    /** Verifies that the edit and delete buttons stay hidden while the material is not loaded, here because it does not exist. */
    @Test
    fun missingMaterial() = runUITest {
        val useCases = storedMaterialUseCases()
        val (store, sessionStore) = setMaterialDetailScreen(useCases = useCases, materialUuid = uuid())

        waitUntil { sessionStore.state.account != null && store.state.writable }
        waitForIdle()
        assertNull(actual = store.state.material)
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
    }

    /** Verifies that a new material opens in edit mode, with only the save and cancel buttons, and is not stored yet. */
    @Test
    fun newMaterial() = runUITest {
        val useCases = storedMaterialUseCases()
        val materialUuid = uuid()
        val (store, _) = setMaterialDetailScreen(useCases = useCases, materialUuid = materialUuid, new = true)

        waitUntil { store.state.material != null && store.state.writable }
        assertTrue(actual = store.state.editing)
        assertTrue(actual = store.state.new)
        assertEquals(expected = materialUuid, actual = store.state.material?.uuid)
        onAllNodes(matcher = hasSetTextAction()).onFirst().assertIsEnabled()
        onNodeWithTag(testTag = "action_save").assertIsDisplayed()
        onNodeWithTag(testTag = "action_cancel").assertIsDisplayed()
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
        assertNull(actual = useCases.materialUseCases.observeMaterial(uuid = materialUuid).first())
    }

    /** Verifies that saving a new material stores it and goes back to viewing it. */
    @Test
    fun saveNewMaterial() = runUITest {
        val useCases = storedMaterialUseCases()
        val materialUuid = uuid()
        val (store, _) = setMaterialDetailScreen(useCases = useCases, materialUuid = materialUuid, new = true)

        waitUntil { store.state.material != null && store.state.writable }
        onAllNodes(matcher = hasSetTextAction()).onFirst().performTextReplacement(text = "New title")
        waitUntil { store.state.material?.name == "New title" }
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { !store.state.editing }

        assertFalse(actual = store.state.new)
        val saved = useCases.materialUseCases.observeMaterial(uuid = materialUuid).first { it != null }
        assertEquals(expected = "New title", actual = saved?.name)
        onNodeWithText(text = "New title").assert(matcher = !hasSetTextAction())
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
    }

    /** Verifies that canceling a new material leaves the screen without storing it. */
    @Test
    fun cancelNewMaterial() = runUITest {
        val useCases = storedMaterialUseCases()
        val materialUuid = uuid()

        router.navigate(screen = Screen.MaterialList, option = Router.NavOption.ADD)
        router.navigate(screen = Screen.MaterialDetail(uuid = materialUuid.toString(), new = true), option = Router.NavOption.ADD)

        val (store, _) = setMaterialDetailScreen(useCases = useCases, materialUuid = materialUuid, new = true)

        waitUntil { store.state.material != null && store.state.writable }
        onNodeWithTag(testTag = "action_cancel").performClick()
        waitUntil { router.backStack.size == 1 }

        assertEquals(expected = listOf<Any>(Screen.MaterialList), actual = router.backStack.toList())
        assertNull(actual = useCases.materialUseCases.observeMaterial(uuid = materialUuid).first())
    }

    /** Verifies that the linked buildings are listed with their quantities, and selecting one opens it. */
    @Test
    fun buildingsAreListed() = runUITest {
        val useCases = storedMaterialUseCases()
        useCases.buildingUseCases.upsertBuilding(building = FakeData.building)
        useCases.buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial)
        val (store, sessionStore) = setMaterialDetailScreen(useCases = useCases)

        waitUntil { store.state.buildings.isNotEmpty() && sessionStore.state.account != null && store.state.writable }
        onNodeWithTag(testTag = "material_buildings").assertIsDisplayed()
        onNodeWithTag(testTag = "material_buildings_extra_${FakeData.building.uuid}", useUnmergedTree = true).assertTextEquals(FakeData.buildingMaterial.quantity.toString())
        onNodeWithTag(testTag = "material_buildings_${FakeData.building.uuid}").performClick()

        waitUntil { router.backStack.lastOrNull() == Screen.BuildingDetail(uuid = FakeData.building.uuid.toString()) }
    }

    /** [FakeData.building] as linked to [FakeData.material] by [FakeData.buildingMaterial]. */
    private val storedLink = Link(uuid = FakeData.building.uuid, name = FakeData.building.name, quantity = FakeData.buildingMaterial.quantity.toString())

    /**
     * Builds the material detail store of [FakeData.material] with [useCases].
     *
     * @param useCases The use cases backing the store.
     * @return The [MaterialDetailScreenStore].
     */
    private fun materialStore(useCases: UseCases): MaterialDetailScreenStore = MaterialDetailScreenStore(
        state = MaterialDetailScreenState(),
        router = router,
        materialUseCases = useCases.materialUseCases,
        buildingMaterialUseCases = useCases.buildingMaterialUseCases,
        authenticationUseCases = useCases.authenticationUseCases,
        materialUuid = FakeData.material.uuid.toString()
    )

    /** Verifies that a building ticked in the picker and confirmed is listed with a quantity of one, and linked to the material with the typed quantity once saved. */
    @Test
    fun addBuilding() = runUITest {
        val useCases = storedMaterialUseCases()
        useCases.buildingUseCases.upsertBuilding(building = FakeData.building)
        val (store, sessionStore, buildingPickerStore) = setMaterialDetailScreen(useCases = useCases)

        waitUntil { store.state.material != null && buildingPickerStore.state.candidates.isNotEmpty() && sessionStore.state.account != null && store.state.writable }
        onNodeWithTag(testTag = "material_buildings_link").assertDoesNotExist()
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithTag(testTag = "material_buildings_link").performClick()
        waitUntil { buildingPickerStore.state.open }
        onNodeWithTag(testTag = "building_row_0").performClick()
        waitUntil { buildingPickerStore.state.picked.isNotEmpty() }
        onNode(matcher = hasTestTag(testTag = "action_save") and hasAnyAncestor(matcher = hasTestTag(testTag = "building_picker"))).performClick()
        waitUntil { !buildingPickerStore.state.open && store.state.buildings.isNotEmpty() }
        assertEquals(expected = listOf(element = storedLink.copy(quantity = "1")), actual = store.state.buildings)
        onNodeWithTag(testTag = "material_buildings_extra_${FakeData.building.uuid}").performTextReplacement(text = "5")
        waitUntil { store.state.buildings.single().quantity == "5" }
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { !store.state.editing }

        val buildings = useCases.buildingMaterialUseCases.observeBuildingsOfMaterial(materialUuid = FakeData.material.uuid).first { it.isNotEmpty() }
        assertEquals(expected = listOf(element = BuildingOfMaterial(building = FakeData.building, quantity = 5)), actual = buildings)
    }

    /** Verifies that canceling after unlinking a building keeps it linked, and that saving after only unlinking it deletes the building material and leaves the unchanged material as stored. */
    @Test
    fun buildingsOnlyEditLeavesMaterialUnchanged() = runUnitTest {
        val useCases = storedMaterialUseCases()
        useCases.buildingUseCases.upsertBuilding(building = FakeData.building)
        useCases.buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial)
        val store = materialStore(useCases = useCases)

        store.stateFlow.first { it.storedMaterial != null && it.buildings.isNotEmpty() }
        store.send(action = MaterialDetailScreenAction.Edit)
        store.send(action = MaterialDetailScreenAction.RemoveBuilding(buildingUuid = FakeData.building.uuid))
        store.send(action = MaterialDetailScreenAction.Cancel)
        assertEquals(expected = listOf(element = storedLink), actual = store.stateFlow.first { !it.editing }.buildings)

        store.send(action = MaterialDetailScreenAction.Edit)
        store.send(action = MaterialDetailScreenAction.RemoveBuilding(buildingUuid = FakeData.building.uuid))
        store.send(action = MaterialDetailScreenAction.Save)
        store.stateFlow.first { !it.editing }

        assertTrue(actual = useCases.buildingMaterialUseCases.observeBuildingsOfMaterial(materialUuid = FakeData.material.uuid).first { it.isEmpty() }.isEmpty())
        assertEquals(expected = FakeData.material, actual = useCases.materialUseCases.observeMaterial(uuid = FakeData.material.uuid).first())
    }

    /** Verifies that a changed quantity is discarded when canceled, keeps the material being edited while it is invalid, and is saved to the building material otherwise. */
    @Test
    fun changeBuildingQuantity() = runUnitTest {
        val useCases = storedMaterialUseCases()
        useCases.buildingUseCases.upsertBuilding(building = FakeData.building)
        useCases.buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial)
        val store = materialStore(useCases = useCases)

        store.stateFlow.first { it.storedMaterial != null && it.buildings.isNotEmpty() }
        store.send(action = MaterialDetailScreenAction.Edit)
        store.send(action = MaterialDetailScreenAction.ChangeBuildingQuantity(buildingUuid = FakeData.building.uuid, quantity = "8"))
        store.send(action = MaterialDetailScreenAction.Cancel)
        assertEquals(expected = listOf(element = storedLink), actual = store.stateFlow.first { !it.editing }.buildings)

        store.send(action = MaterialDetailScreenAction.Edit)
        listOf("", "0", "-2", "1.5", "many").forEach { quantity ->
            store.send(action = MaterialDetailScreenAction.ChangeBuildingQuantity(buildingUuid = FakeData.building.uuid, quantity = quantity))
            assertTrue(actual = store.state.buildings.single().quantityError)
            store.send(action = MaterialDetailScreenAction.Save)
            assertTrue(actual = store.state.editing)
        }

        store.send(action = MaterialDetailScreenAction.ChangeBuildingQuantity(buildingUuid = FakeData.building.uuid, quantity = "8"))
        assertFalse(actual = store.state.buildings.single().quantityError)
        store.send(action = MaterialDetailScreenAction.Save)
        store.stateFlow.first { !it.editing }

        val expected = listOf(element = BuildingOfMaterial(building = FakeData.building, quantity = 8))
        assertEquals(expected = expected, actual = useCases.buildingMaterialUseCases.observeBuildingsOfMaterial(materialUuid = FakeData.material.uuid).first { it == expected })
        assertEquals(expected = FakeData.material, actual = useCases.materialUseCases.observeMaterial(uuid = FakeData.material.uuid).first())
    }

    /** Verifies that setting the linked buildings keeps the quantity typed for a building that stays linked, and links a new building with a quantity of one. */
    @Test
    fun setBuildings() = runUnitTest {
        val useCases = storedMaterialUseCases()
        useCases.buildingUseCases.upsertBuilding(building = FakeData.building)
        useCases.buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial)
        val store = materialStore(useCases = useCases)
        val picked = Link(uuid = FakeData.building.uuid, name = FakeData.building.name)
        val other = Link(uuid = uuid(), name = "Other")

        store.stateFlow.first { it.buildings.isNotEmpty() }
        store.send(action = MaterialDetailScreenAction.Edit)
        store.send(action = MaterialDetailScreenAction.ChangeBuildingQuantity(buildingUuid = FakeData.building.uuid, quantity = "8"))
        store.send(action = MaterialDetailScreenAction.SetBuildings(buildings = persistentListOf(picked, other)))

        assertEquals(expected = listOf(picked.copy(quantity = "8"), other.copy(quantity = "1")), actual = store.state.buildings)
    }

    /** Verifies that the state follows what the current account can do: an admin can write the material and edit its links, a user with only read permission can do neither, and the links can't be read while their client feature flag is off. */
    @Test
    fun permissions() = runUnitTest {
        val admin = materialStore(useCases = storedMaterialUseCases()).stateFlow.first { it.writable }
        assertTrue(actual = admin.let { it.buildingLinks == LinkPermissions(canRead = true, canEdit = true) })

        val reader = materialStore(useCases = storedMaterialUseCases(credentials = FakeData.credentials)).stateFlow.first { it.buildingLinks.canRead }
        assertFalse(actual = reader.writable || reader.buildingLinks.canEdit)

        ClientFlags.set { it.copy(buildingMaterials = false) }
        val disabled = materialStore(useCases = storedMaterialUseCases()).stateFlow.first { it.writable }
        assertTrue(actual = disabled.let { it.buildingLinks == LinkPermissions() })
    }
}
