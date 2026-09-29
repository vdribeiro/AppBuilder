package com.app.builder.ui.screen.materialdetail

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

class MaterialDetailScreenTest: TestCase() {

    /** Verifies that the material detail screen displays the name and code of the loaded material. */
    @Test
    fun materialDetailScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.materialUseCases.upsertMaterial(material = FakeData.material)

        val store = MaterialDetailScreenStore(state = MaterialDetailScreenState(), router = router, materialUseCases = authenticatedUseCases.materialUseCases, materialUuid = FakeData.material.uuid.toString())
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.MATERIAL), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val actionBarStore = ActionBarStore(state = ActionBarState(title = "material", layout = ActionBarLayout.DETAIL), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI {
            MaterialDetailScreen(
                actionBarStore = actionBarStore,
                store = store,
                navigationStore = navigationStore
            )
        }

        assertTrue(actual = router.backStack.isEmpty())
        assertNotNull(actual = store.state.material)

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        assertTrue(actual = navigationStore.state.items.first { it.text == "material" }.selected)

        onNodeWithText(text = "material_name").assertIsDisplayed()
        onNodeWithText(text = FakeData.material.name).assertIsDisplayed()
        onNodeWithText(text = "material_code").assertIsDisplayed()
        onNodeWithText(text = FakeData.material.code).assertIsDisplayed()
    }
}
