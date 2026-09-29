package com.app.builder.domain.gateway.equipment

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
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
import com.app.builder.data.database.EquipmentSchema
import com.app.builder.data.database.EquipmentUserJoinSchema
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
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.Equipment
import com.app.builder.domain.User
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.domain.scheduler.JobResult.NoOp.toJobResult
import com.app.builder.domain.scheduler.Scheduler
import com.app.builder.domain.scheduler.toHeaderMap
import database.AppDatabase

/**
 * Gateway implementation for Equipment Use Cases and Repository.
 *
 * @property user The current user.
 * @property database The SQLite database instance for equipment data.
 * @property httpClient The HTTP client used for network operations.
 * @property scheduler The job scheduler for background tasks.
 */
class EquipmentGateway(
    private val user: User,
    private val database: AppDatabase,
    private val httpClient: HttpClient,
    private val scheduler: Scheduler
): EquipmentUseCases, EquipmentRepository {

    /** Query interface for the equipments table. */
    private val equipmentDao = database.equipmentQueries
    /** Query interface for the equipment-user join table. */
    private val equipmentUserJoinDao = database.equipmentUserJoinQueries

    override fun observeEquipments(): Flow<List<Equipment>> = if (!user.hasPermission(entityType = EntityType.EQUIPMENT, permission = Permission.READ)) {
        val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.EQUIPMENT}")
        Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
        return emptyFlow()
    } else equipmentDao.getEquipments(userUuid = user.uuid)
        .asFlow { it.toEquipment() }
        .onStart {
            val job = Job(
                userUuid = user.uuid,
                entityType = EntityType.EQUIPMENT,
                type = Job.Type.GET,
            )
            scheduler.queue(job = job)
        }
        .flowOn(context = Dispatcher.IO)
        .catch {
            Telemetry.error(tag = TAG, message = "Unable to observe equipments", throwable = it)
            emit(value = emptyList())
        }

    override fun observeEquipment(uuid: Uuid): Flow<Equipment?> = if (!user.hasPermission(entityType = EntityType.EQUIPMENT, permission = Permission.READ)) {
        val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.EQUIPMENT}")
        Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
        return emptyFlow()
    } else equipmentDao.getEquipment(userUuid = user.uuid, equipmentUuid = uuid)
        .asFlow { it.toEquipment() }
        .map { it.firstOrNull() }
        .catch {
            Telemetry.error(tag = TAG, message = "Unable to observe equipment", throwable = it)
            emit(value = null)
        }

    override suspend fun upsertEquipment(equipment: Equipment) = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.EQUIPMENT, permission = Permission.WRITE)) {
            val throwable = Throwable(message = "User does not have ${Permission.WRITE} permission for ${EntityType.EQUIPMENT}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext
        }

        database.safeTransaction {
            upsertEquipmentInDatabase(equipment = equipment)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert equipment", throwable = it)
        }
        val job = Job(
            userUuid = user.uuid,
            entityUuid = equipment.uuid,
            entityType = EntityType.EQUIPMENT,
            type = Job.Type.POST,
            payload = encode(value = equipment)
        )
        scheduler.queue(job = job)
    }

    override suspend fun syncEquipments() = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.EQUIPMENT, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.EQUIPMENT}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext
        }

        val job = Job(
            userUuid = user.uuid,
            entityType = EntityType.EQUIPMENT,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun syncEquipment(uuid: Uuid) = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.EQUIPMENT, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.EQUIPMENT}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext
        }

        val job = Job(
            userUuid = user.uuid,
            entityUuid = uuid,
            entityType = EntityType.EQUIPMENT,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun executeSyncEquipments(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.EQUIPMENT, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.EQUIPMENT}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        val syncStartUtc = now().toString()
        val sync = AppFile.Sync.cache().value.orEmpty()
        val lastSync = sync[AppFile.Sync.Key.EQUIPMENTS_LAST_SYNC_UTC]
        val queryMap: Map<Query, String> = buildMap { if (!lastSync.isNullOrBlank()) put(key = Query.LastSyncUtc, value = lastSync) }

        val result = httpClient.getPaginated<Equipment>(
            request = HttpRequest(
                url = URL.Equipments,
                headerMap = job.toHeaderMap(),
                queryMap = queryMap
            ),
            pageSize = ServerConfigs.configs.pageSize,
            cursorOf = { it.modifiedAt.toString() to it.uuid.toString() }
        ) { equipments ->
            database.safeTransaction {
                syncEquipmentsInDatabase(equipments = equipments)
            }.fold(onSuccess = { HttpResult.Success(data = Unit) }, onFailure = { HttpResult.Error(error = it) })
        }

        when (result) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to sync equipments", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                AppFile.Sync.save { it.orEmpty().plus(pair = (AppFile.Sync.Key.EQUIPMENTS_LAST_SYNC_UTC to syncStartUtc)) }
                Telemetry.info(tag = TAG, message = "Successfully fetched equipments")
                JobResult.Success
            }
        }
    }

    override suspend fun executeSyncEquipment(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.EQUIPMENT, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.EQUIPMENT}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        val entityUuid = job.entityUuid ?: return@withContext Throwable(message = "Missing entity uuid").toJobResult()

        when (val result = httpClient.get<Equipment>(
            request = HttpRequest(
                url = URL.Equipments,
                headerMap = job.toHeaderMap()
            ),
            uuid = entityUuid
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to get remote equipment $entityUuid", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncEquipmentInDatabase(equipment = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync equipment $entityUuid", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully fetched equipment $entityUuid")
                JobResult.Success
            }
        }
    }

    override suspend fun executeUpsertEquipment(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.EQUIPMENT, permission = Permission.WRITE)) {
            val throwable = Throwable(message = "User does not have ${Permission.WRITE} permission for ${EntityType.EQUIPMENT}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        val equipment = job.payload?.let { decode<Equipment>(value = it) } ?: run {
            val throwable = Throwable(message = "Unable to decode equipment")
            Telemetry.error(tag = TAG, message = "Invalid equipment payload", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        when (val result = httpClient.post<Equipment, Equipment>(
            request = HttpRequest(
                url = URL.Equipments,
                headerMap = job.toHeaderMap(),
            ),
            body = equipment
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to post equipment", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncEquipmentInDatabase(equipment = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync equipment", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully upserted equipment")
                JobResult.Success
            }
        }
    }

    /**
     * Synchronizes a single remote equipment with the local database.
     * This function performs a conflict-aware update by fetching the local equivalent of the equipment and comparing their `modifiedAt` timestamps.
     * The equipment is only saved to the database if it does not exist locally, or if the remote version is not older than the local one.
     *
     * @param equipment The remote [Equipment] domain model to evaluate and potentially save.
     */
    private suspend fun syncEquipmentInDatabase(equipment: Equipment) {
        val localEquipment = equipmentDao.getEquipment(userUuid = user.uuid, equipmentUuid = equipment.uuid).awaitAsOneOrNull()
        if (localEquipment == null || equipment.modifiedAt >= localEquipment.modifiedAt) upsertEquipmentInDatabase(equipment = equipment)
    }

    /**
     * Synchronizes a batch of remote equipments with the local database.
     * This function performs an optimized, conflict-aware synchronization.
     * It first queries the local database in chunks to find existing equipments.
     * It then compares the `modifiedAt` timestamps in memory, keeping any remote equipment that is not older than its local counterpart.
     * Finally, it upserts the outdated equipments.
     *
     * @param equipments The list of remote [Equipment] objects to synchronize locally.
     */
    private suspend fun syncEquipmentsInDatabase(equipments: List<Equipment>) {
        val localEquipmentsMap = getEquipmentsByUuidsFromDatabase(uuids = equipments.map { it.uuid }).associateBy { it.uuid }
        equipments.forEach { equipment ->
            val localEquipment = localEquipmentsMap[equipment.uuid]
            if (localEquipment == null || equipment.modifiedAt >= localEquipment.modifiedAt) upsertEquipmentInDatabase(equipment = equipment)
        }
    }

    /**
     * Retrieves equipments matching the given UUIDs from the local database, in chunks.
     * SQLite limits the number of bound parameters allowed in a single statement, so large lists are split into batches before querying.
     *
     * @param uuids The equipment UUIDs to look up, scoped to the currently active user.
     * @return The matching [EquipmentSchema] entities, merged across all chunks.
     */
    private suspend fun getEquipmentsByUuidsFromDatabase(uuids: List<Uuid>): List<EquipmentSchema> = buildList {
        uuids.chunked(size = ClientConfigs.configs.batchSize).forEach { uuidChunk ->
            addAll(elements = equipmentDao.getEquipmentsByUuids(userUuid = user.uuid, equipmentUuids = uuidChunk).awaitAsList())
        }
    }

    /**
     * Inserts or updates a equipment and its user association in the local database.
     *
     * @param equipment The [Equipment] domain model containing the details to save.
     * @return `true` if the equipment and its join record were successfully upserted; `false` if no rows were affected.
     */
    private suspend fun upsertEquipmentInDatabase(equipment: Equipment): Boolean =
        (equipmentDao.upsertEquipment(Equipment = equipment.toEquipmentSchema())
                + equipmentUserJoinDao.upsertEquipmentUserJoin(EquipmentUserJoin = EquipmentUserJoinSchema(equipmentUuid = equipment.uuid, userUuid = user.uuid))) > 0

    companion object {
        private const val TAG = "EquipmentGateway"
    }
}
