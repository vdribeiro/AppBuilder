package com.app.builder.ui.store

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import com.app.builder.test.TestCase

class StoreTest: TestCase() {

    /** Verifies that sending an action runs the reducer and updates the exposed state. */
    @Test
    fun sendDispatchesActionThroughReducerAndUpdatesState() = runUnitTest {
        val store = CounterStore()

        store.send(action = CounterAction.Increment(amount = 3))
        store.send(action = CounterAction.Increment(amount = 4))

        assertEquals(expected = 7, actual = store.state.count)
        assertEquals(expected = store.state, actual = store.stateFlow.value)

        store.send(action = CounterAction.Reset)

        assertEquals(expected = CounterState(), actual = store.state)
    }

    /** Verifies that [Store.launch] executes its block and completes the returned job. */
    @Test
    fun launchExecutesBlockAndCompletes() = runUnitTest { testDispatcher ->
        val store = CounterStore()
        var executed = false

        val job = store.runLaunch(id = JOB_ID) { executed = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(actual = executed)
        assertTrue(actual = job.isCompleted)
    }

    /** Verifies that launching with replace cancels the previous active job for the same id. */
    @Test
    fun launchWithReplaceCancelsPreviousActiveJob() = runUnitTest { testDispatcher ->
        val store = CounterStore()
        var firstCancelled = false
        var secondRan = false

        val firstJob = store.runLaunch(id = JOB_ID) {
            try {
                awaitCancellation()
            } catch (exception: CancellationException) {
                firstCancelled = true
                throw exception
            }
        }

        val secondJob = store.runLaunch(id = JOB_ID, replace = true) { secondRan = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotSame(illegal = firstJob, actual = secondJob)
        assertTrue(actual = firstJob.isCancelled)
        assertTrue(actual = firstCancelled)
        assertTrue(actual = secondRan)
    }

    /** Verifies that launching without replace ignores a new request while the existing job is active. */
    @Test
    fun launchWithoutReplaceIgnoresNewRequestWhileJobIsActive() = runUnitTest {
        val store = CounterStore()
        var secondRan = false

        val firstJob = store.runLaunch(id = JOB_ID) { awaitCancellation() }
        val secondJob = store.runLaunch(id = JOB_ID, replace = false) { secondRan = true }

        assertSame(expected = firstJob, actual = secondJob)
        assertFalse(actual = secondRan)

        firstJob.cancel()
    }

    /** Verifies that launching without replace starts a new job once the previous job under the same id has already completed. */
    @Test
    fun launchWithoutReplaceStartsNewJobAfterPreviousJobCompletes() = runUnitTest { testDispatcher ->
        val store = CounterStore()
        var firstRan = false
        var secondRan = false

        val firstJob = store.runLaunch(id = JOB_ID) { firstRan = true }
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(actual = firstJob.isCompleted)

        val secondJob = store.runLaunch(id = JOB_ID, replace = false) { secondRan = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotSame(illegal = firstJob, actual = secondJob)
        assertTrue(actual = firstRan)
        assertTrue(actual = secondRan)
    }

    /** Verifies that [Store.toStateFlow] exposes the initial value until the upstream flow emits. */
    @Test
    fun toStateFlowExposesInitialValueUntilUpstreamEmits() = runUnitTest { testDispatcher ->
        val store = CounterStore()
        val upstream = flow {
            delay(timeMillis = 1000)
            emit(value = 42)
        }

        val result = store.runToStateFlow(source = upstream, initialValue = -1, started = SharingStarted.Eagerly)
        assertEquals(expected = -1, actual = result.value)

        testDispatcher.scheduler.advanceTimeBy(delayTimeMillis = 1001)
        testDispatcher.scheduler.runCurrent()

        assertEquals(expected = 42, actual = result.value)
    }

    /** Verifies that [Store.observe] only forwards values while the UI is subscribed. */
    @Test
    fun observeOnlyForwardsValuesWhileUIIsSubscribed() = runUnitTest { testDispatcher ->
        val store = CounterStore()
        val source = MutableSharedFlow<Int>(extraBufferCapacity = 10)
        val received = mutableListOf<Int>()

        val uiJob = backgroundScope.launch(context = testDispatcher) { store.stateFlow.collect {} }
        store.runObserve(source = source, id = JOB_ID, timeout = TIMEOUT_MILLIS) { value -> received.add(element = value) }

        source.emit(value = 1)
        assertEquals(expected = listOf(1), actual = received)

        uiJob.cancel()
        testDispatcher.scheduler.runCurrent()

        source.emit(value = 2)
        assertEquals(
            expected = listOf(1, 2),
            actual = received,
            message = "Values emitted before the pause timeout elapses should still be delivered",
        )

        testDispatcher.scheduler.advanceTimeBy(delayTimeMillis = TIMEOUT_MILLIS + 100)
        testDispatcher.scheduler.runCurrent()

        source.emit(value = 3)
        assertEquals(
            expected = listOf(1, 2),
            actual = received,
            message = "Values emitted after the UI unsubscribes and the timeout elapses should be dropped",
        )
    }

    /** Verifies that [Store.observe] resumes forwarding values when the UI resubscribes after a pause. */
    @Test
    fun observeResumesForwardingValuesWhenUIResubscribesAfterPause() = runUnitTest { testDispatcher ->
        val store = CounterStore()
        val source = MutableSharedFlow<Int>(extraBufferCapacity = 10)
        val received = mutableListOf<Int>()

        val firstUIJob = backgroundScope.launch(context = testDispatcher) { store.stateFlow.collect {} }
        store.runObserve(source = source, id = JOB_ID, timeout = TIMEOUT_MILLIS) { value -> received.add(element = value) }

        firstUIJob.cancel()
        testDispatcher.scheduler.advanceTimeBy(delayTimeMillis = TIMEOUT_MILLIS + 100)
        testDispatcher.scheduler.runCurrent()

        backgroundScope.launch(context = testDispatcher) { store.stateFlow.collect {} }
        testDispatcher.scheduler.runCurrent()

        source.emit(value = 5)

        assertEquals(expected = listOf(5), actual = received)
    }

    /** Verifies that observing without replace ignores a new observation request while the existing one is active. */
    @Test
    fun observeWithoutReplaceIgnoresNewObservationWhileActive() = runUnitTest { testDispatcher ->
        val store = CounterStore()
        val source = MutableSharedFlow<Int>(extraBufferCapacity = 10)
        val received = mutableListOf<Int>()

        val uiJob = backgroundScope.launch(context = testDispatcher) { store.stateFlow.collect {} }
        val firstJob = store.runObserve(source = source, id = JOB_ID, timeout = TIMEOUT_MILLIS) { value -> received.add(element = value) }
        val secondJob = store.runObserve(source = source, id = JOB_ID, replace = false, timeout = TIMEOUT_MILLIS) {}

        assertSame(expected = firstJob, actual = secondJob)

        source.emit(value = 1)
        assertEquals(expected = listOf(1), actual = received)

        uiJob.cancel()
    }

    private companion object {
        /** The job id fixture used by the launch and observe tests. */
        private const val JOB_ID = "job"
        /** The observe pause timeout fixture, in milliseconds, used by the observe tests. */
        private const val TIMEOUT_MILLIS = 200L
    }
}

/**
 * A test fixture state for [CounterStore].
 *
 * @property count The current counter value.
 */
private data class CounterState(val count: Int = 0)

/** A test fixture action for [CounterStore]. */
private sealed interface CounterAction {
    /**
     * A test fixture action that increments the counter by [amount].
     *
     * @property amount The amount to increment the counter by.
     */
    data class Increment(val amount: Int): CounterAction
    /** A test fixture action that resets the counter. */
    data object Reset: CounterAction
}

/** A test fixture [Store] used to exercise launch, observe and state flow behavior. */
private class CounterStore: Store<CounterState, CounterAction>(initialState = CounterState()) {

    override fun reducer(state: CounterState, action: CounterAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is CounterAction.Increment -> updateState { it.copy(count = it.count + action.amount) }
            CounterAction.Reset -> updateState { CounterState() }
        }
    }

    /**
     * Exposes [Store.launch] for testing.
     *
     * @param id Identifier used to key the launched job.
     * @param replace Whether an existing job under [id] should be cancelled and replaced.
     * @param block The suspending work to run.
     */
    fun runLaunch(
        id: String,
        replace: Boolean = true,
        block: suspend CoroutineScope.() -> Unit
    ): Job = launch(id = id, replace = replace, block = block)

    /**
     * Exposes [Store.observe] for testing.
     *
     * @param source The flow to observe.
     * @param id Identifier used to key the observing job.
     * @param replace Whether an existing job under [id] should be cancelled and replaced.
     * @param timeout Milliseconds to keep observing after the last collector disappears.
     * @param block Invoked with every value emitted by [source].
     */
    fun runObserve(
        source: Flow<Int>,
        id: String,
        replace: Boolean = true,
        timeout: Long = 5000L,
        block: (Int) -> Unit
    ): Job = source.observe(id = id, replace = replace, timeout = timeout) { value -> block(value) }

    /**
     * Exposes [Store.toStateFlow] for testing.
     *
     * @param source The flow to convert.
     * @param initialValue The value the resulting [StateFlow] holds before [source] emits.
     * @param started Strategy controlling when [source] is actively collected.
     * @return The resulting [StateFlow].
     */
    fun runToStateFlow(
        source: Flow<Int>,
        initialValue: Int,
        started: SharingStarted = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000)
    ): StateFlow<Int> = source.toStateFlow(started = started, initialValue = initialValue)
}
