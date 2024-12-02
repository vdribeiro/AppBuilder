package com.app.builder.domain.push

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import androidx.annotation.VisibleForTesting
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.builder.Application
import com.app.builder.Dependency
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.EntityType
import com.app.builder.domain.PushPayload
import com.app.builder.domain.gateway.Repositories
import com.app.builder.domain.gateway.authentication.toUser

/**
 * Manager implementation for push payloads collections.
 *
 * @property appGraph dependency index.
 */
class PayloadManager(
    private val appGraph: Dependency.AppGraph
): PayloadService {

    /** An isolated scope for the manager's coroutine operations. */
    private val scope = CoroutineScope(context = SupervisorJob())

    /** Guards concurrent start()/stop() transitions. */
    private val mutex = Mutex()

    /** Tracks the currently active loop. */
    private var job: Job? = null

    override suspend fun stop() = withContext(context = Dispatcher.IO) {
        mutex.withLock {
            job?.cancelAndJoin()
            job = null
        }
    }

    override fun start() {
        if (!mutex.tryLock()) return
        try {
            startJob()
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Listens for payloads.
     * If the job is already running, this is a no-op.
     */
    private fun startJob() {
        if (job?.isActive != true) job = scope.launch(context = Dispatcher.IO) {
            PushProvider.pushPayloads.collect { handlePushPayload(it) }
        }
    }

    /**
     * Handle push payloads.
     *
     * @param payload The push payload to process.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal suspend fun handlePushPayload(payload: PushPayload) = withContext(context = Dispatcher.IO) {
        runCatching {
            when (payload) {
                is PushPayload.Notification -> NotificationProvider.showNotification(notification = payload)
                is PushPayload.EntityNotification -> NotificationProvider.showNotification(notification = payload).also { sync(payload = payload) }
                is PushPayload.Flags -> appGraph.getRepositories(payload = payload).configRepository.syncFeatureFlags()
                is PushPayload.Configs -> appGraph.getRepositories(payload = payload).configRepository.syncConfigs()
                is PushPayload.Reset -> Application.reset()
            }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to process payload ${payload.uuid}", throwable = it)
        }
    }

    /**
     * Launches a data resync for the given entity.
     *
     * @param payload The push payload to process.
     */
    private suspend fun sync(payload: PushPayload.EntityNotification) {
        if (!ClientFlags.flags.pushSync) return
        val entityUuid = payload.entityUuid ?: return
        when (payload.entityType) {
            EntityType.USER -> appGraph.getRepositories(payload = payload).userRepository.syncUser(uuid = entityUuid)
            EntityType.TASK -> appGraph.getRepositories(payload = payload).taskRepository.syncTask(uuid = entityUuid)
            else -> Unit
        }
    }

    /**
     * Get the repositories of a user, or the guest if the provided user is not recognized.
     *
     * @param payload The push payload to use.
     * @return A [Repositories] instance.
     */
    private suspend fun Dependency.AppGraph.getRepositories(payload: PushPayload): Repositories {
        val userUuid = payload.userUuid
        if (userUuid == null || userUuid == Dependency.guestUser.uuid) return userGraph.repositories
        val user = database.userQueries.getUser(userUuid = userUuid).awaitAsOneOrNull()?.toUser() ?: return userGraph.repositories
        return getUserDependency(user = user).repositories
    }

    companion object {
        private const val TAG = "PayloadManager"
    }
}