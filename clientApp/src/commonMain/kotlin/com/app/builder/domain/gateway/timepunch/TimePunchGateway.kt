package com.app.builder.domain.gateway.timepunch

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.now
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.TimePunchSchema
import com.app.builder.data.database.asFlow
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.HttpResult
import com.app.builder.data.http.Query
import com.app.builder.data.http.URL
import com.app.builder.data.http.get
import com.app.builder.data.http.getPaginated
import com.app.builder.data.http.post
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Account
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.TimePunch
import com.app.builder.domain.gateway.account.validatePermission
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.domain.scheduler.JobResult.NoOp.toJobResult
import com.app.builder.domain.scheduler.Scheduler
import com.app.builder.domain.scheduler.toHeaderMap
import database.AppDatabase

/**
 * Gateway implementation for TimePunch Use Cases and Repository.
 * An account reads the time punches of every user with the [Permission.READ] permission and its own without it.
 * It writes any time punch with the [Permission.WRITE] permission and only creates its own without it.
 *
 * @property account The account of the current user.
 * @property database The SQLite database instance for time punch data.
 * @property httpClient The HTTP client used for network operations.
 * @property scheduler The job scheduler for background tasks.
 */
class TimePunchGateway(
    private val account: Account,
    private val database: AppDatabase,
    private val httpClient: HttpClient,
    private val scheduler: Scheduler
): TimePunchUseCases, TimePunchRepository {

    /** Query interface for the time punches table. */
    private val timePunchDao = database.timePunchQueries

    override fun observeTimePunches(): Flow<List<TimePunch>> {
        val query = when {
            account.hasPermission(entityType = EntityType.TIME_PUNCH, permission = Permission.READ) -> timePunchDao.getTimePunches(mapper = ::TimePunchSchema)
            else -> timePunchDao.getTimePunchesOfUser(userUuid = account.userUuid, mapper = ::TimePunchSchema)
        }

        return query
            .asFlow { it.toTimePunch() }
            .onStart {
                val job = Job(
                    userUuid = account.userUuid,
                    entityType = EntityType.TIME_PUNCH,
                    type = Job.Type.GET,
                )
                scheduler.queue(job = job)
            }
            .flowOn(context = Dispatcher.IO)
            .catch {
                Telemetry.error(tag = TAG, message = "Unable to observe time punches", throwable = it)
                emit(value = emptyList())
            }
    }

    override fun observeTimePunch(uuid: Uuid): Flow<TimePunch?> =
        timePunchDao.getTimePunch(timePunchUuid = uuid)
            .asFlow { it.toTimePunch() }
            .map { timePunches -> timePunches.firstOrNull()?.takeIf { account.hasPermission(entityType = EntityType.TIME_PUNCH, permission = Permission.READ) || it.userUuid == account.userUuid } }
            .flowOn(context = Dispatcher.IO)
            .catch {
                Telemetry.error(tag = TAG, message = "Unable to observe time punch", throwable = it)
                emit(value = null)
            }

    override suspend fun upsertTimePunch(timePunch: TimePunch) = withContext(context = Dispatcher.IO) {
        val created = timePunch.userUuid == account.userUuid && timePunch.deletedAt == null && timePunchDao.getTimePunch(timePunchUuid = timePunch.uuid).awaitAsOneOrNull() == null
        if (!created) account.validatePermission(tag = TAG, entityType = EntityType.TIME_PUNCH, permission = Permission.WRITE)?.let { return@withContext }

        database.safeTransaction {
            upsertTimePunchInDatabase(timePunch = timePunch)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert time punch", throwable = it)
        }
        val job = Job(
            userUuid = account.userUuid,
            entityUuid = timePunch.uuid,
            entityType = EntityType.TIME_PUNCH,
            type = Job.Type.POST,
            payload = encode(value = timePunch)
        )
        scheduler.queue(job = job)
    }

    override suspend fun syncTimePunches() = withContext(context = Dispatcher.IO) {
        val job = Job(
            userUuid = account.userUuid,
            entityType = EntityType.TIME_PUNCH,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun syncTimePunch(uuid: Uuid) = withContext(context = Dispatcher.IO) {
        val job = Job(
            userUuid = account.userUuid,
            entityUuid = uuid,
            entityType = EntityType.TIME_PUNCH,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun executeSyncTimePunches(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        val syncStartUtc = now().toString()
        val sync = AppFile.Sync.cache().value.orEmpty()
        val lastSyncKey = when {
            account.hasPermission(entityType = EntityType.TIME_PUNCH, permission = Permission.READ) -> AppFile.Sync.Key.TIME_PUNCHES_LAST_SYNC_UTC
            else -> AppFile.Sync.Key.OWN_TIME_PUNCHES_LAST_SYNC_UTC
        }
        val lastSync = sync[lastSyncKey]
        val queryMap: Map<Query, String> = buildMap { if (!lastSync.isNullOrBlank()) put(key = Query.LastSyncUtc, value = lastSync) }

        val result = httpClient.getPaginated<TimePunch>(
            request = HttpRequest(
                url = URL.TimePunches,
                headerMap = job.toHeaderMap(),
                queryMap = queryMap
            ),
            pageSize = ServerConfigs.value.pageSize,
            cursorOf = { it.modifiedAt.toString() to it.uuid.toString() }
        ) { timePunches ->
            database.safeTransaction {
                syncTimePunchesInDatabase(timePunches = timePunches)
            }.fold(onSuccess = { HttpResult.Success(data = Unit) }, onFailure = { HttpResult.Error(error = it) })
        }

        when (result) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to sync time punches", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                AppFile.Sync.save { it.orEmpty().plus(pair = (lastSyncKey to syncStartUtc)) }
                Telemetry.info(tag = TAG, message = "Successfully fetched time punches")
                JobResult.Success
            }
        }
    }

    override suspend fun executeSyncTimePunch(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        val entityUuid = job.entityUuid ?: return@withContext Throwable(message = "Missing entity uuid").toJobResult()

        when (val result = httpClient.get<TimePunch>(
            request = HttpRequest(
                url = URL.TimePunches,
                headerMap = job.toHeaderMap()
            ),
            uuid = entityUuid
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to get remote time punch $entityUuid", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncTimePunchInDatabase(timePunch = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync time punch $entityUuid", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully fetched time punch $entityUuid")
                JobResult.Success
            }
        }
    }

    override suspend fun executeUpsertTimePunch(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        val timePunch = job.payload?.let { decode<TimePunch>(value = it) } ?: run {
            val throwable = Throwable(message = "Unable to decode time punch")
            Telemetry.error(tag = TAG, message = "Invalid time punch payload", throwable = throwable)
            return@withContext throwable.toJobResult()
        }
        if (timePunch.userUuid != account.userUuid || timePunch.deletedAt != null) {
            account.validatePermission(tag = TAG, entityType = EntityType.TIME_PUNCH, permission = Permission.WRITE)?.let { return@withContext it.toJobResult() }
        }

        when (val result = httpClient.post<TimePunch, TimePunch>(
            request = HttpRequest(
                url = URL.TimePunches,
                headerMap = job.toHeaderMap(),
            ),
            body = timePunch
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to post time punch", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncTimePunchInDatabase(timePunch = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync time punch", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully upserted time punch")
                JobResult.Success
            }
        }
    }

    /**
     * Synchronizes a single remote time punch with the local database.
     * This function performs a conflict-aware update by fetching the local equivalent of the time punch and comparing their `modifiedAt` timestamps.
     * The time punch is only saved to the database if it does not exist locally, or if the remote version is not older than the local one.
     *
     * @param timePunch The remote [TimePunch] domain model to evaluate and potentially save.
     */
    private suspend fun syncTimePunchInDatabase(timePunch: TimePunch) {
        val localTimePunch = timePunchDao.getTimePunch(timePunchUuid = timePunch.uuid).awaitAsOneOrNull()
        if (localTimePunch == null || timePunch.modifiedAt >= localTimePunch.modifiedAt) upsertTimePunchInDatabase(timePunch = timePunch)
    }

    /**
     * Synchronizes a batch of remote time punches with the local database.
     * This function performs an optimized, conflict-aware synchronization.
     * It first queries the local database in chunks to find existing timePunches.
     * It then compares the `modifiedAt` timestamps in memory, keeping any remote time punch that is not older than its local counterpart.
     * Finally, it upserts the outdated timePunches.
     *
     * @param timePunches The list of remote [TimePunch] objects to synchronize locally.
     */
    private suspend fun syncTimePunchesInDatabase(timePunches: List<TimePunch>) {
        val localTimePunchesMap = getTimePunchesByUuidsFromDatabase(uuids = timePunches.map { it.uuid }).associateBy { it.uuid }
        timePunches.forEach { timePunch ->
            val localTimePunch = localTimePunchesMap[timePunch.uuid]
            if (localTimePunch == null || timePunch.modifiedAt >= localTimePunch.modifiedAt) upsertTimePunchInDatabase(timePunch = timePunch)
        }
    }

    /**
     * Retrieves time punches matching the given UUIDs from the local database, in chunks.
     * SQLite limits the number of bound parameters allowed in a single statement, so large lists are split into batches before querying.
     *
     * @param uuids The time punch UUIDs to look up.
     * @return The matching [TimePunchSchema] entities, merged across all chunks.
     */
    private suspend fun getTimePunchesByUuidsFromDatabase(uuids: List<Uuid>): List<TimePunchSchema> = buildList {
        uuids.chunked(size = ClientConfigs.value.batchSize).forEach { uuidChunk ->
            addAll(elements = timePunchDao.getTimePunchesByUuids(timePunchUuids = uuidChunk).awaitAsList())
        }
    }

    /**
     * Inserts or updates a time punch in the local database.
     *
     * @param timePunch The [TimePunch] domain model containing the details to save.
     * @return `true` if the time punch was successfully upserted; `false` if no rows were affected.
     */
    private suspend fun upsertTimePunchInDatabase(timePunch: TimePunch): Boolean =
        timePunchDao.upsertTimePunch(TimePunch = timePunch.toTimePunchSchema()) > 0

    companion object {
        private const val TAG = "TimePunchGateway"
    }
}
