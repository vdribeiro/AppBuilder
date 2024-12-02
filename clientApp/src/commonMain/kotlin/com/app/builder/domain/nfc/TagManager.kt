package com.app.builder.domain.nfc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.app.builder.Dependency
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.now
import com.app.builder.core.nfc.NfcRecord
import com.app.builder.core.nfc.NfcTag
import com.app.builder.core.nfc.TagProvider
import com.app.builder.core.security.uuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.EntityType
import com.app.builder.domain.Task
import com.app.builder.domain.User
import com.app.builder.domain.deeplink.DeepLink

/**
 * Manager implementation for NFC tags processing.
 * Written entities always use the current (guest, if none is signed in) user's use cases, since a tag carries no user context of its own.
 *
 * @property appGraph dependency index.
 */
class TagManager(
    private val appGraph: Dependency.AppGraph
): TagService {

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
     * Listens for tags.
     * If the job is already running, this is a no-op.
     */
    private fun startJob() {
        if (job?.isActive != true) job = scope.launch(context = Dispatcher.IO) {
            TagProvider.tags.collect { handleTag(tag = it) }
        }
    }

    /**
     * Handle a tag read.
     *
     * @param tag The tag to process.
     */
    private suspend fun handleTag(tag: NfcTag) = withContext(context = Dispatcher.IO) {
        runCatching {
            tag.records.forEach { record ->
                when (record) {
                    is NfcRecord.Open -> DeepLink.send(deepLink = DeepLink.Entity(entityUuid = record.entityUuid, entityType = record.entityType))
                    is NfcRecord.Upsert -> upsert(record = record)
                }
            }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to process tag ${tag.id}", throwable = it)
        }
    }

    /**
     * Inserts a new entity or updates the existing one for [record].
     *
     * @param record The record to upsert.
     */
    private suspend fun upsert(record: NfcRecord.Upsert) {
        val useCases = appGraph.userGraph.useCases
        when (record.entityType) {
            EntityType.USER -> useCases.userUseCases.upsertUser(
                user = User(
                    uuid = record.entityUuid ?: uuid(),
                    modifiedAt = now(),
                    deletedAt = null,
                    permissions = emptyMap(),
                    name = now().toString(),
                    avatar = null
                )
            )

            EntityType.TASK -> useCases.taskUseCases.upsertTask(
                task = Task(
                    uuid = record.entityUuid ?: uuid(),
                    modifiedAt = now(),
                    deletedAt = null,
                    title = now().toString(),
                    description = "",
                    state = Task.State.TODO
                )
            )

            EntityType.CLIENT_FLAG,
            EntityType.CLIENT_CONFIG,
            EntityType.SERVER_FLAG,
            EntityType.SERVER_CONFIG,
            EntityType.REGISTRY,
            EntityType.NOTIFICATION,
            EntityType.TRANSLATION,
            EntityType.SESSION,
            EntityType.DEVICE_LOCATION,
            EntityType.FILE -> Unit
        }
    }

    companion object {
        private const val TAG = "TagManager"
    }
}
