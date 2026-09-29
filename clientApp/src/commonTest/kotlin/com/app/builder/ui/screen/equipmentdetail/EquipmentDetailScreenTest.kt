package com.app.builder.ui.screen.equipmentdetail

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.app.builder.Dependency.getUserDependency
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.bar.ActionBarLayout
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore

class EquipmentDetailScreenTest: TestCase() {

    /** Verifies that the equipment detail screen displays the name and code of the loaded equipment. */
    @Test
    fun equipmentDetailScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)

        val store = EquipmentDetailScreenStore(state = EquipmentDetailScreenState(), router = router, equipmentUseCases = authenticatedUseCases.equipmentUseCases, equipmentUuid = FakeData.equipment.uuid.toString())
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.EQUIPMENT), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val actionBarStore = ActionBarStore(state = ActionBarState(title = "equipment", layout = ActionBarLayout.DETAIL), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI {
            EquipmentDetailScreen(
                actionBarStore = actionBarStore,
                store = store,
                navigationStore = navigationStore
            )
        }

        assertTrue(actual = router.backStack.isEmpty())
        assertNotNull(actual = store.state.equipment)

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        assertTrue(actual = navigationStore.state.items.first { it.text == "equipment" }.selected)

        onNodeWithText(text = "equipment_name").assertIsDisplayed()
        onNodeWithText(text = FakeData.equipment.name).assertIsDisplayed()
        onNodeWithText(text = "equipment_code").assertIsDisplayed()
        onNodeWithText(text = FakeData.equipment.code).assertIsDisplayed()
    }
}
