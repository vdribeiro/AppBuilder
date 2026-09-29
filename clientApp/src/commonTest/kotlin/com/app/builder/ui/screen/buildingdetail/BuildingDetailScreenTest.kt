package com.app.builder.ui.screen.buildingdetail

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

class BuildingDetailScreenTest: TestCase() {

    /** Verifies that the building detail screen displays the name and code of the loaded building. */
    @Test
    fun buildingDetailScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.buildingUseCases.upsertBuilding(building = FakeData.building)

        val store = BuildingDetailScreenStore(state = BuildingDetailScreenState(), router = router, buildingUseCases = authenticatedUseCases.buildingUseCases, buildingUuid = FakeData.building.uuid.toString())
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.BUILDING), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val actionBarStore = ActionBarStore(state = ActionBarState(title = "building", layout = ActionBarLayout.DETAIL), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI {
            BuildingDetailScreen(
                actionBarStore = actionBarStore,
                store = store,
                navigationStore = navigationStore
            )
        }

        assertTrue(actual = router.backStack.isEmpty())
        assertNotNull(actual = store.state.building)

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        assertTrue(actual = navigationStore.state.items.first { it.text == "building" }.selected)

        onNodeWithText(text = "building_name").assertIsDisplayed()
        onNodeWithText(text = FakeData.building.name).assertIsDisplayed()
        onNodeWithText(text = "building_code").assertIsDisplayed()
        onNodeWithText(text = FakeData.building.code).assertIsDisplayed()
    }
}
