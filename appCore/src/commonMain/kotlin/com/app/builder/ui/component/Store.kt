package com.app.builder.ui.component

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.builder.core.flow.Dispatcher

/**
 * The central state coordinator for a screen, following the MVI (Model-View-Intent) pattern.
 * The [Store] acts as the single source of truth for the UI.
 * It interprets [Action]s (Intents), executes business logic, and produces a new [State] (Model) for the UI (View) to observe.
 *
 * ### Key Principles:
 * 1. **Unidirectional Flow:** Actions flow in; State flows out.
 * 2. **Reactive:** The UI observes the [stateFlow] and never modifies state directly.
 * 3. **Lifecycle-Aware:** Managed coroutines automatically clean up when the screen is destroyed.
 *
 * @param State An immutable data class representing the UI's current information.
 * @param Action A sealed class/interface representing all possible user interactions.
 * @param initialState The initial baseline state of the UI before any actions are processed.
 */
open class Store<State, Action>(initialState: State): ViewModel() {

    /** Backing property for the reactive UI state. */
    private val _stateFlow: MutableStateFlow<State> = MutableStateFlow(value = initialState)
    /** A read-only [StateFlow] that the UI observes to render its elements. */
    val stateFlow: StateFlow<State> = _stateFlow.asStateFlow()
    /** Provides the current, non-reactive snapshot of the [State]. */
    val state: State get() = stateFlow.value

    /** Registry of active background [Job]s, keyed by a unique identifier to prevent leaks or redundant executions. */
    private val activeJobs = mutableMapOf<String, Job>()

    /**
     * Dispatches a user [Action] to the [reducer].
     *
     * @param action The action to be processed.
     */
    fun send(action: Action) = reducer(state = _stateFlow.value, action = action)

    /**
     * Core logic handler. Subclasses must override this to interpret [Action]s and call [updateState] or launch background tasks.
     *
     * @param state The snapshot of the [State] at the moment the action was sent.
     * @param action The specific [Action] to be processed and reacted to.
     */
    protected open fun reducer(state: State, action: Action) {}

    /**
     * Atomically updates the UI [State].
     *
     * @param body A lambda that accepts the current state and returns the updated state.
     * @return The updated [State] instance.
     */
    protected fun updateState(body: (State) -> State): State = _stateFlow.updateAndGet(function = body)

    /**
     * Converts a cold [Flow] into a hot [StateFlow] scoped to the [viewModelScope].
     *
     * @param <T> The type of data emitted by the flow.
     * @param started Strategy that controls when sharing starts and stops.
     * @param initialValue The initial value of the state flow.
     * @return A [StateFlow] representing the stream of data.
     */
    protected fun <T> Flow<T>.toStateFlow(
        started: SharingStarted = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue: T
    ): StateFlow<T> = stateIn(
        scope = viewModelScope,
        started = started,
        initialValue = initialValue
    )

    /**
     * Manages the lifecycle of a [Job] associated with a unique [id].
     * If a job with [id] does not exist, a new one is created and added to the [activeJobs] map.
     * If a job with the same [id] is already active:
     * - If [replace] is `true`, the existing job is canceled and a new one starts.
     * - If [replace] is `false`, the existing job is preserved and the new request is ignored.
     *
     * @param id Job identifier.
     * @param replace Whether to cancel an existing job with the same ID.
     * @param block The suspending code to execute that produces a new [Job].
     * @return The active [Job], either the newly created one or the existing one.
     */
    private fun launchJob(
        id: String,
        replace: Boolean,
        block: () -> Job
    ): Job {
        val job = activeJobs[id]?.takeIf { it.isActive }
        return if (job != null && !replace) job else {
            job?.cancel()
            block().also { job ->
                activeJobs[id] = job
                job.invokeOnCompletion { if (activeJobs[id] === job) activeJobs.remove(key = id) }
            }
        }
    }

    /**
     * Launches a coroutine in the [viewModelScope] tied to an [id].
     *
     * @param id Job identifier.
     * @param replace If `true`, cancels the previous execution of this ID before starting. Defaults to `true`.
     * @param context The [CoroutineContext] for execution. Defaults to [Dispatcher.Default].
     * @param block The suspending code to execute.
     * @return The [Job] representing this execution.
     */
    protected fun launch(
        id: String,
        replace: Boolean = true,
        context: CoroutineContext = Dispatcher.Default,
        block: suspend CoroutineScope.() -> Unit
    ): Job = launchJob(
        id = id,
        replace = replace,
    ) { viewModelScope.launch(context = context, block = block) }

    /**
     * Collects the upstream [Flow] in a lifecycle-aware manner, tied to UI subscription.
     * Execution starts when the UI begins observing the [stateFlow] and pauses after [timeout] milliseconds once the UI stops observing.
     * This prevents unnecessary restarts during configuration changes (screen rotation, configuration changes, rapid navigation, etc...).
     *
     * @param <T> The type of data emitted by the flow.
     * @param id Job identifier.
     * @param replace If `true`, replaces any existing observation with the same [id].
     * @param context The [CoroutineContext] for execution. Defaults to [Dispatcher.Default].
     * @param timeout Grace period in milliseconds to keep the flow active after the UI unbinds.
     * @param block Lambda triggered for every emitted value [T].
     * @return A [Job] managed by the [viewModelScope].
     */
    protected fun <T> Flow<T>.observe(
        id: String,
        replace: Boolean = true,
        context: CoroutineContext = Dispatcher.Default,
        timeout: Long = 5000L,
        block: suspend (T) -> Unit
    ): Job = launchJob(
        id = id,
        replace = replace,
    ) {
        _stateFlow.subscriptionCount
            .map { count -> count > 0 }
            .distinctUntilChanged()
            .transformLatest { isVisible ->
                if (isVisible) emit(value = true) else {
                    delay(timeMillis = timeout)
                    emit(value = false)
                }
            }
            .flatMapLatest { isVisible -> if (isVisible) this@observe else emptyFlow() }
            .onEach { data -> block(data) }
            .flowOn(context = context)
            .launchIn(scope = viewModelScope)
    }
}