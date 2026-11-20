package com.app.builder.domain.gateway.equipmentuser

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
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
import com.app.builder.data.database.EquipmentSchema
import com.app.builder.data.database.EquipmentUserSchema
import com.app.builder.data.database.UserSchema
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
import com.app.builder.domain.Equipment
import com.app.builder.domain.EquipmentUser
import com.app.builder.domain.Permission
import com.app.builder.domain.User
import com.app.builder.domain.gateway.account.validatePermission
import com.app.builder.domain.gateway.authentication.toUser
import com.app.builder.domain.gateway.equipment.toEquipment
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.domain.scheduler.JobResult.NoOp.toJobResult
import com.app.builder.domain.scheduler.Scheduler
import com.app.builder.domain.scheduler.toHeaderMap
import database.AppDatabase

/**
 * Gateway implementation for Equipment User Use Cases and Repository.
 *
 * @property account The account of the current user.
 * @property database The SQLite database instance for equipment user data.
 * @property httpClient The HTTP client used for network operations.
 * @property scheduler The job scheduler for background equipment users.
 */
class EquipmentUserGateway(
    private val account: Account,
    private val database: AppDatabase,
    private val httpClient: HttpClient,
    private val scheduler: Scheduler
): EquipmentUserUseCases, EquipmentUserRepository {

    /** Query interface for the equipment users table. */
    private val equipmentUserDao = database.equipmentUserQueries

    override fun observeUsersOfEquipment(equipmentUuid: Uuid): Flow<List<User>> {
        account.validatePermission(tag = TAG, entityType = EntityType.EQUIPMENT_USER, permission = Permission.READ)?.let { return emptyFlow() }

        return equipmentUserDao.getUsersOfEquipment(equipmentUuid = equipmentUuid, mapper = ::UserSchema)
            .asFlow { it.toUser() }
            .onStart { queueSync() }
            .flowOn(context = Dispatcher.IO)
            .catch {
                Telemetry.error(tag = TAG, message = "Unable to observe the users of equipment $equipmentUuid", throwable = it)
                emit(value = emptyList())
            }
    }

    override fun observeEquipmentsOfUser(userUuid: Uuid): Flow<List<Equipment>> {
        account.validatePermission(tag = TAG, entityType = EntityType.EQUIPMENT_USER, permission = Permission.READ)?.let { return emptyFlow() }

        return equipmentUserDao.getEquipmentsOfUser(userUuid = userUuid, mapper = ::EquipmentSchema)
            .asFlow { it.toEquipment() }
            .onStart { queueSync() }
            .flowOn(context = Dispatcher.IO)
            .catch {
                Telemetry.error(tag = TAG, message = "Unable to observe the equipments of user $userUuid", throwable = it)
                emit(value = emptyList())
            }
    }

    override suspend fun upsertEquipmentUser(equipmentUser: EquipmentUser) = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.EQUIPMENT_USER, permission = Permission.WRITE)?.let { return@withContext }

        database.safeTransaction {
            upsertEquipmentUserInDatabase(equipmentUser = equipmentUser)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert equipment user", throwable = it)
        }
        val job = Job(
            userUuid = account.userUuid,
            entityUuid = equipmentUser.uuid,
            entityType = EntityType.EQUIPMENT_USER,
            type = Job.Type.POST,
            payload = encode(value = equipmentUser)
        )
        scheduler.queue(job = job)
    }

    override suspend fun syncEquipmentUsers() = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.EQUIPMENT_USER, permission = Permission.READ)?.let { return@withContext }

        queueSync()
    }

    override suspend fun syncEquipmentUser(uuid: Uuid) = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.EQUIPMENT_USER, permission = Permission.READ)?.let { return@withContext }

        val job = Job(
            userUuid = account.userUuid,
            entityUuid = uuid,
            entityType = EntityType.EQUIPMENT_USER,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun executeSyncEquipmentUsers(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.EQUIPMENT_USER, permission = Permission.READ)?.let { return@withContext it.toJobResult() }

        val syncStartUtc = now().toString()
        val sync = AppFile.Sync.cache().value.orEmpty()
        val lastSync = sync[AppFile.Sync.Key.EQUIPMENT_USERS_LAST_SYNC_UTC]
        val queryMap: Map<Query, String> = buildMap { if (!lastSync.isNullOrBlank()) put(key = Query.LastSyncUtc, value = lastSync) }

        val result = httpClient.getPaginated<EquipmentUser>(
            request = HttpRequest(
                url = URL.EquipmentUsers,
                headerMap = job.toHeaderMap(),
                queryMap = queryMap
            ),
            pageSize = ServerConfigs.value.pageSize,
            cursorOf = { it.modifiedAt.toString() to it.uuid.toString() }
        ) { equipmentUsers ->
            database.safeTransaction {
                syncEquipmentUsersInDatabase(equipmentUsers = equipmentUsers)
            }.fold(onSuccess = { HttpResult.Success(data = Unit) }, onFailure = { HttpResult.Error(error = it) })
        }

        when (result) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to sync equipment users", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                AppFile.Sync.save { it.orEmpty().plus(pair = (AppFile.Sync.Key.EQUIPMENT_USERS_LAST_SYNC_UTC to syncStartUtc)) }
                Telemetry.info(tag = TAG, message = "Successfully fetched equipment users")
                JobResult.Success
            }
        }
    }

    override suspend fun executeSyncEquipmentUser(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.EQUIPMENT_USER, permission = Permission.READ)?.let { return@withContext it.toJobResult() }

        val entityUuid = job.entityUuid ?: return@withContext Throwable(message = "Missing entity uuid").toJobResult()

        when (val result = httpClient.get<EquipmentUser>(
            request = HttpRequest(
                url = URL.EquipmentUsers,
                headerMap = job.toHeaderMap()
            ),
            uuid = entityUuid
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to get remote equipment user $entityUuid", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncEquipmentUserInDatabase(equipmentUser = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync equipment user $entityUuid", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully fetched equipment user $entityUuid")
                JobResult.Success
            }
        }
    }

    override suspend fun executeUpsertEquipmentUser(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.EQUIPMENT_USER, permission = Permission.WRITE)?.let { return@withContext it.toJobResult() }

        val equipmentUser = job.payload?.let { decode<EquipmentUser>(value = it) } ?: run {
            val throwable = Throwable(message = "Unable to decode equipment user")
            Telemetry.error(tag = TAG, message = "Invalid equipment user payload", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        when (val result = httpClient.post<EquipmentUser, EquipmentUser>(
            request = HttpRequest(
                url = URL.EquipmentUsers,
                headerMap = job.toHeaderMap(),
            ),
            body = equipmentUser
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to post equipment user", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncEquipmentUserInDatabase(equipmentUser = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync equipment user", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully upserted equipment user")
                JobResult.Success
            }
        }
    }

    /** Queues a job fetching the equipment users changed since the last sync. */
    private suspend fun queueSync() {
        val job = Job(
            userUuid = account.userUuid,
            entityType = EntityType.EQUIPMENT_USER,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    /**
     * Synchronizes a single remote equipment user with the local database.
     * This function performs a conflict-aware update by fetching the local equivalent of the equipment user and comparing their `modifiedAt` timestamps.
     * The equipment user is only saved to the database if it does not exist locally, or if the remote version is not older than the local one.
     *
     * @param equipmentUser The remote [EquipmentUser] domain model to evaluate and potentially save.
     */
    private suspend fun syncEquipmentUserInDatabase(equipmentUser: EquipmentUser) {
        val localEquipmentUser = equipmentUserDao.getEquipmentUser(equipmentUserUuid = equipmentUser.uuid).awaitAsOneOrNull()
        if (localEquipmentUser == null || equipmentUser.modifiedAt >= localEquipmentUser.modifiedAt) upsertEquipmentUserInDatabase(equipmentUser = equipmentUser)
    }

    /**
     * Synchronizes a batch of remote equipment users with the local database.
     * This function performs an optimized, conflict-aware synchronization.
     * It first queries the local database in chunks to find existing equipment users.
     * It then compares the `modifiedAt` timestamps in memory, keeping any remote equipment user that is not older than its local counterpart.
     * Finally, it upserts the outdated equipment users.
     *
     * @param equipmentUsers The list of remote [EquipmentUser] objects to synchronize locally.
     */
    private suspend fun syncEquipmentUsersInDatabase(equipmentUsers: List<EquipmentUser>) {
        val localEquipmentUsersMap = getEquipmentUsersByUuidsFromDatabase(uuids = equipmentUsers.map { it.uuid }).associateBy { it.uuid }
        equipmentUsers.forEach { equipmentUser ->
            val localEquipmentUser = localEquipmentUsersMap[equipmentUser.uuid]
            if (localEquipmentUser == null || equipmentUser.modifiedAt >= localEquipmentUser.modifiedAt) upsertEquipmentUserInDatabase(equipmentUser = equipmentUser)
        }
    }

    /**
     * Retrieves equipment users matching the given UUIDs from the local database, in chunks.
     * SQLite limits the number of bound parameters allowed in a single statement, so large lists are split into batches before querying.
     *
     * @param uuids The equipment user UUIDs to look up.
     * @return The matching [EquipmentUserSchema] entities, merged across all chunks.
     */
    private suspend fun getEquipmentUsersByUuidsFromDatabase(uuids: List<Uuid>): List<EquipmentUserSchema> = buildList {
        uuids.chunked(size = ClientConfigs.value.batchSize).forEach { uuidChunk ->
            addAll(elements = equipmentUserDao.getEquipmentUsersByUuids(equipmentUserUuids = uuidChunk).awaitAsList())
        }
    }

    /**
     * Inserts or updates an equipment user in the local database.
     *
     * @param equipmentUser The [EquipmentUser] domain model containing the details to save.
     * @return `true` if the equipment user was successfully upserted; `false` if no rows were affected.
     */
    private suspend fun upsertEquipmentUserInDatabase(equipmentUser: EquipmentUser): Boolean =
        equipmentUserDao.upsertEquipmentUser(EquipmentUser = equipmentUser.toEquipmentUserSchema()) > 0

    companion object {
        private const val TAG = "EquipmentUserGateway"
    }
}
