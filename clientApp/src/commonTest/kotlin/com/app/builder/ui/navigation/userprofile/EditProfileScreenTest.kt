package com.app.builder.ui.navigation.userprofile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.app.builder.Dependency.getUserDependency
import com.app.builder.domain.Account
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.navigation.Screen

class EditProfileScreenTest: TestCase() {

    /**
     * Logs in as the admin, opens the edit profile screen over the user profile, and creates its store with the use cases of the given [account].
     *
     * @param account The account the user use cases belong to.
     * @return The store of the edit profile screen.
     */
    private suspend fun editProfileStore(account: Account = FakeData.adminAccount): EditProfileScreenStore {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        authenticationUseCases.login(credentials = FakeData.adminCredentials)
        router.navigate(screen = Screen.UserProfile)
        router.navigate(screen = Screen.EditProfile)

        return EditProfileScreenStore(
            state = EditProfileScreenState(),
            router = router,
            authenticationUseCases = authenticationUseCases,
            userUseCases = dependency.get().getUserDependency(account = account).useCases.userUseCases
        )
    }

    /** Verifies that the name is filled with the current user's, that the save button is disabled while it is blank, and that saving stores the new name and leaves the screen. */
    @Test
    fun editProfile() = runUITest {
        val store = editProfileStore()

        setUI { EditProfileScreen(store = store) }

        waitUntil { store.state.user == FakeData.adminUser }
        onNodeWithTag(testTag = "user_name").assertIsDisplayed().assertTextContains(value = FakeData.adminUser.name)
        onNodeWithTag(testTag = "save").assertIsDisplayed().assertIsEnabled()

        onNodeWithTag(testTag = "user_name").performTextReplacement(text = " ")
        onNodeWithTag(testTag = "save").assertIsNotEnabled()

        onNodeWithTag(testTag = "user_name").performTextReplacement(text = "Renamed Admin")
        onNodeWithTag(testTag = "save").assertIsEnabled().performClick()

        waitUntil(timeoutMillis = 10_000) { router.backStack.toList() == listOf(Screen.UserProfile) }
        val user = dependency.get().userGraph.useCases.authenticationUseCases.observeCurrentUser().firstOrNull()
        assertEquals(expected = "Renamed Admin", actual = user?.name)
    }

    /** Verifies that saving an avatar URL stores it on the current user, and that saving a blank one removes it. */
    @Test
    fun editAvatar() = runUITest {
        val authenticationUseCases = dependency.get().userGraph.useCases.authenticationUseCases
        val store = editProfileStore()

        setUI { EditProfileScreen(store = store) }

        waitUntil { store.state.user == FakeData.adminUser }
        onNodeWithTag(testTag = "user_avatar").assertIsDisplayed().performTextReplacement(text = "https://example.com/avatar.png")
        onNodeWithTag(testTag = "save").performClick()

        waitUntil(timeoutMillis = 10_000) { router.backStack.toList() == listOf(Screen.UserProfile) }
        waitUntil(timeoutMillis = 10_000) { store.state.user?.avatar == "https://example.com/avatar.png" }
        assertEquals(expected = "https://example.com/avatar.png", actual = authenticationUseCases.observeCurrentUser().firstOrNull()?.avatar)

        router.navigate(screen = Screen.EditProfile)
        onNodeWithTag(testTag = "user_avatar").performTextReplacement(text = " ")
        onNodeWithTag(testTag = "save").performClick()

        waitUntil(timeoutMillis = 10_000) { router.backStack.toList() == listOf(Screen.UserProfile) }
        waitUntil(timeoutMillis = 10_000) { store.state.user != null && store.state.user?.avatar == null }
        assertNull(actual = authenticationUseCases.observeCurrentUser().firstOrNull()?.avatar)
    }

    /** Verifies that a user without any permission can save its own name. */
    @Test
    fun editProfileWithoutPermission() = runUITest {
        val store = editProfileStore(account = FakeData.adminAccount.copy(permissions = emptyMap()))

        setUI { EditProfileScreen(store = store) }

        waitUntil { store.state.user == FakeData.adminUser }
        onNodeWithTag(testTag = "user_name").performTextReplacement(text = "Renamed Admin")
        onNodeWithTag(testTag = "save").performClick()

        waitUntil(timeoutMillis = 10_000) { router.backStack.toList() == listOf(Screen.UserProfile) }
        val user = dependency.get().userGraph.useCases.authenticationUseCases.observeCurrentUser().firstOrNull()
        assertEquals(expected = "Renamed Admin", actual = user?.name)
    }

    /** Verifies that saving an unchanged name leaves the screen without changing the user, and that the back button leaves the screen without saving an edited name. */
    @Test
    fun unchangedProfile() = runUITest {
        val store = editProfileStore()

        setUI { EditProfileScreen(store = store) }

        waitUntil { store.state.user == FakeData.adminUser }
        onNodeWithTag(testTag = "save").performClick()
        waitUntil(timeoutMillis = 10_000) { router.backStack.toList() == listOf(Screen.UserProfile) }

        router.navigate(screen = Screen.EditProfile)
        onNodeWithTag(testTag = "user_name").performTextReplacement(text = "Renamed Admin")
        onNodeWithTag(testTag = "action_back").performClick()
        waitForIdle()

        assertEquals(expected = listOf(Screen.UserProfile), actual = router.backStack.toList())
        val user = dependency.get().userGraph.useCases.authenticationUseCases.observeCurrentUser().firstOrNull()
        assertEquals(expected = FakeData.adminUser, actual = user)
    }
}
