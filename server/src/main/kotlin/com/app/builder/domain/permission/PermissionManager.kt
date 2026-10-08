package com.app.builder.domain.permission

import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.security.uuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.data.signal.InstanceSignal

/**
 * Manages cache for user authorization state to allow changes to take effect immediately, without a database lookup on every authenticated request.
 *
 * @property instanceSignal Relays payloads to every server instance.
 */
class PermissionManager(
    private val instanceSignal: InstanceSignal,
): PermissionService {

    /** Scope for the instance signal. */
    private val scope = CoroutineScope(context = SupervisorJob())

    /** Guards concurrent start()/stop() transitions. */
    private val mutex = Mutex()

    /** Identifies this instance to prevent echoing signals. */
    private val instanceId = uuid()

    /** Tracks the signal observation applying permission updates from other instances to this instance's cache. */
    private var listenJob: Job? = null

    /** Cache for user authorization state. */
    private val cache = ConcurrentHashMap<Uuid, UserAccess>()

    override suspend fun stop() = withContext(context = Dispatcher.IO) {
        mutex.withLock {
            listenJob?.cancelAndJoin()
            listenJob = null
            cache.clear()
        }
    }

    override fun start() {
        if (!mutex.tryLock()) return
        try {
            startListenJob()
        } finally {
            mutex.unlock()
        }
    }

    override suspend fun set(userUuid: Uuid, access: UserAccess?) {
        if (access != null) cache[userUuid] = access else cache.remove(key = userUuid)
        val payload = encode<Pair<Uuid, UserAccess?>>(value = userUuid to access) ?: run {
            Telemetry.error(tag = TAG, message = "Unable to encode user access")
            return
        }
        instanceSignal.notify(channel = CHANNEL, payload = payload)
    }

    override fun get(userUuid: Uuid): UserAccess? = cache[userUuid]

    /**
     * Observes other instances.
     * If the job is already running, this is a no-op.
     */
    private fun startListenJob() {
        if (listenJob?.isActive != true) listenJob = scope.launch(context = Dispatcher.IO) {
            instanceSignal.observe(channel = CHANNEL).collect { envelope ->
                if (envelope.substringBefore(delimiter = ENVELOPE_SEPARATOR) == instanceId.toString()) return@collect
                val payload = envelope.substringAfter(delimiter = ENVELOPE_SEPARATOR)
                val userAccess = decode<Pair<Uuid, UserAccess?>>(value = payload) ?: return@collect
                val access = userAccess.second
                if (access != null) cache[userAccess.first] = access else cache.remove(key = userAccess.first)
            }
        }
    }

    companion object {
        private const val TAG = "PermissionManager"

        /** The signal channel relaying encoded payloads. */
        private const val CHANNEL = "permissions"

        /** Separates the originating instance id from the payload. */
        private const val ENVELOPE_SEPARATOR = '|'
    }
}
