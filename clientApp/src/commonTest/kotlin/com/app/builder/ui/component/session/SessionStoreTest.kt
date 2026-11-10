package com.app.builder.ui.component.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.status.StatusUseCases
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class SessionStoreTest: TestCase() {

    /**
     * Resolves the authentication use cases of the guest graph, the ones that observe whoever is logged in.
     *
     * @return The [AuthenticationUseCases] instance.
     */
    private suspend fun authenticationUseCases(): AuthenticationUseCases = dependency.get()
        .userGraph
        .useCases
        .authenticationUseCases

    /**
     * Keeps the store's state observed, as a rendered screen would.
     *
     * @param store The store to observe.
     * @return The collecting [Job].
     */
    private fun TestScope.subscribe(store: SessionStore): Job = backgroundScope.launch { store.stateFlow.collect {} }

    /** Verifies that a logged out session resolves without a user or account. */
    @Test
    fun loggedOut() = runUnitTest {
        val store = SessionStore(state = SessionState(), authenticationUseCases = authenticationUseCases())
        subscribe(store = store)

        val state = store.stateFlow.first { it.loaded }
        assertNull(actual = state.user)
        assertNull(actual = state.account)
    }

    /** Verifies that a logged in session resolves the user and their account. */
    @Test
    fun loggedIn() = runUnitTest {
        val authenticationUseCases = authenticationUseCases()
        authenticationUseCases.login(credentials = FakeData.adminCredentials)
        val store = SessionStore(state = SessionState(), authenticationUseCases = authenticationUseCases)
        subscribe(store = store)

        val state = store.stateFlow.first { it.loaded && it.user != null }
        assertEquals(expected = FakeData.adminUser.name, actual = state.user?.name)
        assertEquals(expected = FakeData.adminUser.uuid, actual = state.account?.userUuid)
    }

    /** Verifies that the session reports when the server refused the refresh token of the logged in user. */
    @Test
    fun sessionExpired() = runUnitTest {
        val authenticationUseCases = authenticationUseCases()
        authenticationUseCases.login(credentials = FakeData.adminCredentials)
        val store = SessionStore(state = SessionState(), authenticationUseCases = authenticationUseCases)
        subscribe(store = store)
        assertFalse(actual = store.stateFlow.first { it.loaded && it.user != null }.sessionExpired)

        dependency.get().userGraph.repositories.authenticationRepository.refreshBearerToken(userUuid = FakeData.adminUser.uuid, refreshToken = FakeData.expiredBearerToken.refreshToken)

        store.stateFlow.first { it.sessionExpired }
    }

    /** Verifies that the session follows the application status, and assumes it is fine without status use cases. */
    @Test
    fun status() = runUnitTest {
        val synced = MutableStateFlow(value = true)
        val clockTrusted = MutableStateFlow(value = true)
        val store = SessionStore(
            state = SessionState(),
            authenticationUseCases = authenticationUseCases(),
            statusUseCases = object: StatusUseCases {
                override fun observeSynced(): Flow<Boolean> = synced
                override fun observeClockTrusted(): Flow<Boolean> = clockTrusted
            },
        )
        subscribe(store = store)

        store.stateFlow.first { it.synced && it.clockTrusted }

        synced.value = false
        store.stateFlow.first { !it.synced }

        clockTrusted.value = false
        store.stateFlow.first { !it.clockTrusted }

        synced.value = true
        clockTrusted.value = true
        store.stateFlow.first { it.synced && it.clockTrusted }

        val withoutStatus = SessionStore(state = SessionState(), authenticationUseCases = authenticationUseCases())
        subscribe(store = withoutStatus)
        val withoutStatusState = withoutStatus.stateFlow.first { it.loaded }
        assertTrue(actual = withoutStatusState.synced)
        assertTrue(actual = withoutStatusState.clockTrusted)
    }
}
