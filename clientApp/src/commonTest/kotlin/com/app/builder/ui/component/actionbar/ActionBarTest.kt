package com.app.builder.ui.component.actionbar

import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.app.builder.domain.Account
import com.app.builder.domain.EntityType
import com.app.builder.domain.User
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

class ActionBarTest: TestCase() {

    /** Every entity button tag. */
    private val entityButtons = listOf("action_add", "action_edit", "action_save", "action_cancel", "action_delete")

    /**
     * Renders an action bar offering every entity button, for the given session.
     *
     * @param user The signed in user, or `null` when no user is signed in.
     * @param account The signed in user's account, or `null` when no user is signed in.
     * @param entityType Entity the write permission is checked against.
     * @param editEntityTypes Further entities whose write permission also allows editing.
     */
    private fun ComposeUiTest.setActionBar(user: User?, account: Account?, entityType: EntityType? = EntityType.TASK, editEntityTypes: Set<EntityType> = emptySet()) {
        val sessionStore = Store<SessionState, Unit>(initialState = SessionState(loaded = true, user = user, account = account))
        setUI {
            ActionBar(
                sessionStore = sessionStore,
                entityType = entityType,
                editEntityTypes = editEntityTypes,
                onAddClick = {},
                onEditClick = {},
                onSaveClick = {},
                onCancelClick = {},
                onDeleteClick = {},
            )
        }
    }

    /** Verifies that a logged out user is offered the login button, which opens the login screen, and only the cancel button among the entity ones. */
    @Test
    fun loggedOut() = runUITest {
        setActionBar(user = null, account = null)

        onNodeWithTag(testTag = "action_cancel").assertIsDisplayed()
        (entityButtons - "action_cancel").forEach { onNodeWithTag(testTag = it).assertDoesNotExist() }
        onNodeWithTag(testTag = "action_login").assertIsDisplayed().performClick()
        assertEquals(expected = listOf(Screen.Login), actual = router.backStack.toList())
    }

    /** Verifies that a user with write permission on the entity is offered every entity button, and that their avatar opens the user profile. */
    @Test
    fun writePermission() = runUITest {
        setActionBar(user = FakeData.adminUser, account = FakeData.adminAccount)

        entityButtons.forEach { onNodeWithTag(testTag = it).assertIsDisplayed() }
        onNodeWithTag(testTag = "action_login").assertDoesNotExist()
        onNodeWithTag(testTag = "action_avatar").assertIsDisplayed().performClick()
        assertEquals(expected = listOf(Screen.UserProfile), actual = router.backStack.toList())
    }

    /** Verifies that a user with only read permission on the entity is offered only the cancel button among the entity ones. */
    @Test
    fun readPermission() = runUITest {
        setActionBar(user = FakeData.user, account = FakeData.account)

        onNodeWithTag(testTag = "action_cancel").assertIsDisplayed()
        (entityButtons - "action_cancel").forEach { onNodeWithTag(testTag = it).assertDoesNotExist() }
    }

    /** Verifies that write permission on a further editable entity offers editing and saving, while adding and deleting still need it on the entity. */
    @Test
    fun editPermission() = runUITest {
        setActionBar(user = FakeData.user, account = FakeData.account, editEntityTypes = setOf(EntityType.DEVICE_LOCATION))

        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
        onNodeWithTag(testTag = "action_save").assertIsDisplayed()
        onNodeWithTag(testTag = "action_add").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
    }

    /** Verifies that without an entity to check permissions against, only the cancel button is offered among the entity ones. */
    @Test
    fun withoutEntity() = runUITest {
        setActionBar(user = FakeData.adminUser, account = FakeData.adminAccount, entityType = null)

        onNodeWithTag(testTag = "action_cancel").assertIsDisplayed()
        (entityButtons - "action_cancel").forEach { onNodeWithTag(testTag = it).assertDoesNotExist() }
    }

    /** Verifies that the filters only show while a filters store is given. */
    @Test
    fun filters() = runUITest {
        val sessionStore = Store<SessionState, Unit>(initialState = SessionState(loaded = true))
        val filtersStore = Store<FiltersState, FiltersAction>(initialState = FiltersState())
        var withFilters by mutableStateOf(value = false)
        setUI { ActionBar(sessionStore = sessionStore, filtersStore = filtersStore.takeIf { withFilters }) }

        onNodeWithTag(testTag = "action_expand").assertDoesNotExist()
        withFilters = true
        onNodeWithTag(testTag = "action_expand").assertIsDisplayed()
    }
}
