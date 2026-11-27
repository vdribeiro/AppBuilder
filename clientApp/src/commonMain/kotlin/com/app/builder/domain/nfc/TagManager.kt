package com.app.builder.domain.nfc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.app.builder.Dependency
import com.app.builder.Dependency.getUserGraph
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.flow.runSuspendCatching
import com.app.builder.core.locale.getLocalDateTime
import com.app.builder.core.nfc.NfcRecord
import com.app.builder.core.nfc.NfcTag
import com.app.builder.core.nfc.TagProvider
import com.app.builder.core.security.uuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.Building
import com.app.builder.domain.EntityType
import com.app.builder.domain.Equipment
import com.app.builder.domain.Material
import com.app.builder.domain.Task
import com.app.builder.domain.TimePunch
import com.app.builder.domain.User
import com.app.builder.domain.deeplink.DeepLink
import com.app.builder.domain.gateway.UseCases

/**
 * Manager implementation for NFC tags processing.
 * Written entities always use the current or guest, if none is signed in, user's use cases, since a tag carries no user context of its own.
 *
 * @property appGraph dependency index.
 * @property tags The tag reads to process.
 */
class TagManager(
    private val appGraph: Dependency.AppGraph,
    private val tags: Flow<NfcTag> = TagProvider.tags,
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
            tags.collect { handleTag(tag = it) }
        }
    }

    /**
     * Handle a tag read.
     *
     * @param tag The tag to process.
     */
    private suspend fun handleTag(tag: NfcTag) = withContext(context = Dispatcher.IO) {
        runSuspendCatching {
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
        val useCases = appGraph.getUseCases()
        val dateTime = getLocalDateTime()
        val uuid = record.entityUuid ?: uuid()
        when (record.entityType) {
            EntityType.USER -> useCases.userUseCases.upsertUser(user = User(uuid = uuid, name = dateTime))
            EntityType.TASK -> useCases.taskUseCases.upsertTask(task = Task(uuid = uuid, title = dateTime))
            EntityType.TIME_PUNCH -> useCases.authenticationUseCases.observeCurrentUser().first()?.let { user ->
                useCases.timePunchUseCases.upsertTimePunch(timePunch = TimePunch(userUuid = user.uuid, source = TimePunch.Source.NFC))
            }

            EntityType.BUILDING -> useCases.buildingUseCases.upsertBuilding(building = Building(uuid = uuid, name = dateTime))
            EntityType.EQUIPMENT -> useCases.equipmentUseCases.upsertEquipment(equipment = Equipment(uuid = uuid, name = dateTime))
            EntityType.MATERIAL -> useCases.materialUseCases.upsertMaterial(material = Material(uuid = uuid, name = dateTime))
            EntityType.FILE,
            EntityType.CLIENT_FLAG,
            EntityType.CLIENT_CONFIG,
            EntityType.SERVER_FLAG,
            EntityType.SERVER_CONFIG,
            EntityType.REGISTRY,
            EntityType.NOTIFICATION,
            EntityType.TRANSLATION,
            EntityType.ACCOUNT,
            EntityType.DEVICE_LOCATION,
            EntityType.NFC,
            EntityType.BUILDING_EQUIPMENT,
            EntityType.BUILDING_MATERIAL,
            EntityType.BUILDING_USER,
            EntityType.EQUIPMENT_USER -> Unit
        }
    }

    /**
     * Get the use cases of the current user.
     *
     * @return A [UseCases] instance.
     */
    private suspend fun Dependency.AppGraph.getUseCases(): UseCases {
        val user = userGraph.useCases.authenticationUseCases.observeCurrentUser().first()
        return (getUserGraph(userUuid = user?.uuid) ?: userGraph).useCases
    }

    companion object {
        private const val TAG = "TagManager"
    }
}
