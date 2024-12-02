package com.app.builder.domain.clock

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.observeClockChanges
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.URL
import com.app.builder.data.http.head
import com.app.builder.data.storage.CoreFile

/**
 * Manager that uses these signals to feed into the clock's trust state:
 * - Locally stored clock offset entry: updated by the network layer's NTP-style recalibration on every response, and observed here to calculate trust against the clock drift tolerance.
 * - OS-level clock change notifications: invalidates trust if the clock is tampered with.
 * - A network probe: to recalculate the offset when the clock is untrusted.
 *
 * @property httpClient The HTTP client used for network operations.
 */
class ClockManager(
    private val httpClient: HttpClient,
): ClockService {

    /** Scope for the offset, clock-change and probe loops. */
    private val scope = CoroutineScope(context = SupervisorJob())

    /** Guards concurrent start()/stop() transitions. */
    private val mutex = Mutex()

    /** Tracks the currently active clock offset observation loop. */
    private var offsetJob: Job? = null
    /** Tracks the currently active OS clock-change observation loop. */
    private var clockChangeJob: Job? = null
    /** Tracks the currently active network probe loop. */
    private var probeJob: Job? = null

    /** Backing state for [trusted]. */
    private val _trusted = MutableStateFlow(value = false)
    override val trusted: StateFlow<Boolean> = _trusted.asStateFlow()

    override suspend fun stop() = withContext(context = Dispatcher.IO) {
        mutex.withLock {
            offsetJob?.cancelAndJoin()
            offsetJob = null
            clockChangeJob?.cancelAndJoin()
            clockChangeJob = null
            probeJob?.cancelAndJoin()
            probeJob = null
        }
    }

    override fun start() {
        if (!mutex.tryLock()) return
        try {
            startOffsetJob()
            startClockChangeJob()
            startProbeJob()
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Observes the locally stored clock offset and updates [trusted] based on whether the drift is within tolerance.
     * If the job is already running, this is a no-op.
     */
    private fun startOffsetJob() {
        if (offsetJob?.isActive != true) offsetJob = scope.launch(context = Dispatcher.IO) {
            CoreFile.Device.cache()
                .map { cache -> cache.orEmpty()[CoreFile.Device.Key.CLOCK_OFFSET] }
                .distinctUntilChanged()
                .collectLatest {
                    val offset = it?.let { Duration.parseOrNull(value = it) }
                    val isTrusted = offset != null && offset.absoluteValue <= ClientConfigs.configs.clockDriftTolerance.milliseconds
                    when {
                        isTrusted -> Telemetry.info(tag = TAG, message = "Clock drift within tolerance")
                        it == null -> Telemetry.info(tag = TAG, message = "No clock offset recorded yet")
                        offset == null -> Telemetry.error(tag = TAG, message = "Clock offset value is corrupted: $it")
                        else -> Telemetry.error(tag = TAG, message = "Clock drift exceeds tolerance: $offset")
                    }
                    _trusted.value = isTrusted
                }
        }
    }

    /**
     * Observes OS-level clock change notifications and invalidates [trusted] whenever one is detected.
     * If the job is already running, this is a no-op.
     */
    private fun startClockChangeJob() {
        if (clockChangeJob?.isActive != true) clockChangeJob = scope.launch(context = Dispatcher.IO) {
            observeClockChanges().collectLatest {
                _trusted.value = false
                Telemetry.error(tag = TAG, message = "Clock change detected")
            }
        }
    }

    /**
     * While [trusted] is false, periodically probes the network to trigger offset recalibration.
     * If the job is already running, this is a no-op.
     */
    private fun startProbeJob() {
        if (probeJob?.isActive != true) probeJob = scope.launch(context = Dispatcher.IO) {
            _trusted.collect { isTrusted ->
                if (isTrusted) return@collect
                while (!_trusted.value) {
                    httpClient.head(request = HttpRequest(url = URL.Probe))
                    if (_trusted.value) return@collect
                    delay(timeMillis = ClientConfigs.configs.clockProbeInterval)
                }
            }
        }
    }

    companion object {
        private const val TAG = "ClockManager"
    }
}