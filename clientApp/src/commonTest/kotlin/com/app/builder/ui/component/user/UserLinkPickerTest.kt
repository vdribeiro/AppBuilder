package com.app.builder.ui.component.user

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.storage.AppFile
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.link.LinkPickerAction

class UserLinkPickerTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(http = true) }
    }

    /** Verifies that the picker applies the user list filters, searching and leaving out the deleted users, and ticks the picked users. */
    @Test
    fun pickerAppliesUserFilters() = runUnitTest {
        val useCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        useCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        val other = FakeData.user.copy(uuid = uuid(), name = "Jane Doe")
        val deleted = FakeData.user.copy(uuid = uuid(), name = "Jane Gone", deletedAt = now())
        listOf(FakeData.user, other, deleted).forEach { useCases.userUseCases.upsertUser(user = it) }
        AppFile.UserPreferences.save { userFilterDefaults.copy(search = "jane", showDeleted = true) }
        val store = userLinkPickerStore(userUseCases = useCases.userUseCases)

        val state = store.stateFlow.first { state -> state.candidates.any { it.uuid == other.uuid } && state.filters.search == "jane" }
        assertEquals(expected = listOf(other.uuid.toString()), actual = state.items.map { it.uuid })
        assertEquals(expected = listOf<Boolean?>(false), actual = state.items.map { it.checked })

        store.send(action = LinkPickerAction.Open(picked = setOf(other.uuid)))
        assertEquals(expected = listOf<Boolean?>(true), actual = store.state.items.map { it.checked })
        assertEquals(expected = listOf(element = other), actual = store.state.pickedCandidates)
    }
}
