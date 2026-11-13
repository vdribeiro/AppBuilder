package com.app.builder.domain.gateway.buildingequipment

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
import com.app.builder.data.database.BuildingEquipmentSchema
import com.app.builder.data.database.BuildingSchema
import com.app.builder.data.database.EquipmentSchema
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
import com.app.builder.domain.Building
import com.app.builder.domain.BuildingEquipment
import com.app.builder.domain.EntityType
import com.app.builder.domain.Equipment
import com.app.builder.domain.Permission
import com.app.builder.domain.gateway.account.validatePermission
import com.app.builder.domain.gateway.building.toBuilding
import com.app.builder.domain.gateway.equipment.toEquipment
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.domain.scheduler.JobResult.NoOp.toJobResult
import com.app.builder.domain.scheduler.Scheduler
import com.app.builder.domain.scheduler.toHeaderMap
import database.AppDatabase

/**
 * Gateway implementation for Building Equipment Use Cases and Repository.
 *
 * @property account The account of the current user.
 * @property database The SQLite database instance for building equipment data.
 * @property httpClient The HTTP client used for network operations.
 * @property scheduler The job scheduler for background building equipments.
 */
class BuildingEquipmentGateway(
    private val account: Account,
    private val database: AppDatabase,
    private val httpClient: HttpClient,
    private val scheduler: Scheduler
): BuildingEquipmentUseCases, BuildingEquipmentRepository {

    /** Query interface for the building equipments table. */
    private val buildingEquipmentDao = database.buildingEquipmentQueries

    override fun observeEquipmentsInBuilding(buildingUuid: Uuid): Flow<List<Equipment>> {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_EQUIPMENT, permission = Permission.READ)?.let { return emptyFlow() }

        return buildingEquipmentDao.getEquipmentsInBuilding(buildingUuid = buildingUuid, mapper = ::EquipmentSchema)
            .asFlow { it.toEquipment() }
            .onStart { queueSync() }
            .flowOn(context = Dispatcher.IO)
            .catch {
                Telemetry.error(tag = TAG, message = "Unable to observe the equipments in building $buildingUuid", throwable = it)
                emit(value = emptyList())
            }
    }

    override fun observeBuildingsOfEquipment(equipmentUuid: Uuid): Flow<List<Building>> {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_EQUIPMENT, permission = Permission.READ)?.let { return emptyFlow() }

        return buildingEquipmentDao.getBuildingsOfEquipment(equipmentUuid = equipmentUuid, mapper = ::BuildingSchema)
            .asFlow { it.toBuilding() }
            .onStart { queueSync() }
            .flowOn(context = Dispatcher.IO)
            .catch {
                Telemetry.error(tag = TAG, message = "Unable to observe the buildings of equipment $equipmentUuid", throwable = it)
                emit(value = emptyList())
            }
    }

    override suspend fun upsertBuildingEquipment(buildingEquipment: BuildingEquipment) = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_EQUIPMENT, permission = Permission.WRITE)?.let { return@withContext }

        database.safeTransaction {
            upsertBuildingEquipmentInDatabase(buildingEquipment = buildingEquipment)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert building equipment", throwable = it)
        }
        val job = Job(
            userUuid = account.userUuid,
            entityUuid = buildingEquipment.uuid,
            entityType = EntityType.BUILDING_EQUIPMENT,
            type = Job.Type.POST,
            payload = encode(value = buildingEquipment)
        )
        scheduler.queue(job = job)
    }

    override suspend fun syncBuildingEquipments() = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_EQUIPMENT, permission = Permission.READ)?.let { return@withContext }

        queueSync()
    }

    override suspend fun syncBuildingEquipment(uuid: Uuid) = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_EQUIPMENT, permission = Permission.READ)?.let { return@withContext }

        val job = Job(
            userUuid = account.userUuid,
            entityUuid = uuid,
            entityType = EntityType.BUILDING_EQUIPMENT,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun executeSyncBuildingEquipments(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_EQUIPMENT, permission = Permission.READ)?.let { return@withContext it.toJobResult() }

        val syncStartUtc = now().toString()
        val sync = AppFile.Sync.cache().value.orEmpty()
        val lastSync = sync[AppFile.Sync.Key.BUILDING_EQUIPMENTS_LAST_SYNC_UTC]
        val queryMap: Map<Query, String> = buildMap { if (!lastSync.isNullOrBlank()) put(key = Query.LastSyncUtc, value = lastSync) }

        val result = httpClient.getPaginated<BuildingEquipment>(
            request = HttpRequest(
                url = URL.BuildingEquipments,
                headerMap = job.toHeaderMap(),
                queryMap = queryMap
            ),
            pageSize = ServerConfigs.value.pageSize,
            cursorOf = { it.modifiedAt.toString() to it.uuid.toString() }
        ) { buildingEquipments ->
            database.safeTransaction {
                syncBuildingEquipmentsInDatabase(buildingEquipments = buildingEquipments)
            }.fold(onSuccess = { HttpResult.Success(data = Unit) }, onFailure = { HttpResult.Error(error = it) })
        }

        when (result) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to sync building equipments", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                AppFile.Sync.save { it.orEmpty().plus(pair = (AppFile.Sync.Key.BUILDING_EQUIPMENTS_LAST_SYNC_UTC to syncStartUtc)) }
                Telemetry.info(tag = TAG, message = "Successfully fetched building equipments")
                JobResult.Success
            }
        }
    }

    override suspend fun executeSyncBuildingEquipment(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_EQUIPMENT, permission = Permission.READ)?.let { return@withContext it.toJobResult() }

        val entityUuid = job.entityUuid ?: return@withContext Throwable(message = "Missing entity uuid").toJobResult()

        when (val result = httpClient.get<BuildingEquipment>(
            request = HttpRequest(
                url = URL.BuildingEquipments,
                headerMap = job.toHeaderMap()
            ),
            uuid = entityUuid
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to get remote building equipment $entityUuid", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncBuildingEquipmentInDatabase(buildingEquipment = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync building equipment $entityUuid", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully fetched building equipment $entityUuid")
                JobResult.Success
            }
        }
    }

    override suspend fun executeUpsertBuildingEquipment(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_EQUIPMENT, permission = Permission.WRITE)?.let { return@withContext it.toJobResult() }

        val buildingEquipment = job.payload?.let { decode<BuildingEquipment>(value = it) } ?: run {
            val throwable = Throwable(message = "Unable to decode building equipment")
            Telemetry.error(tag = TAG, message = "Invalid building equipment payload", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        when (val result = httpClient.post<BuildingEquipment, BuildingEquipment>(
            request = HttpRequest(
                url = URL.BuildingEquipments,
                headerMap = job.toHeaderMap(),
            ),
            body = buildingEquipment
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to post building equipment", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncBuildingEquipmentInDatabase(buildingEquipment = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync building equipment", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully upserted building equipment")
                JobResult.Success
            }
        }
    }

    /** Queues a job fetching the building equipments changed since the last sync. */
    private suspend fun queueSync() {
        val job = Job(
            userUuid = account.userUuid,
            entityType = EntityType.BUILDING_EQUIPMENT,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    /**
     * Synchronizes a single remote building equipment with the local database.
     * This function performs a conflict-aware update by fetching the local equivalent of the building equipment and comparing their `modifiedAt` timestamps.
     * The building equipment is only saved to the database if it does not exist locally, or if the remote version is not older than the local one.
     *
     * @param buildingEquipment The remote [BuildingEquipment] domain model to evaluate and potentially save.
     */
    private suspend fun syncBuildingEquipmentInDatabase(buildingEquipment: BuildingEquipment) {
        val localBuildingEquipment = buildingEquipmentDao.getBuildingEquipment(buildingEquipmentUuid = buildingEquipment.uuid).awaitAsOneOrNull()
        if (localBuildingEquipment == null || buildingEquipment.modifiedAt >= localBuildingEquipment.modifiedAt) upsertBuildingEquipmentInDatabase(buildingEquipment = buildingEquipment)
    }

    /**
     * Synchronizes a batch of remote building equipments with the local database.
     * This function performs an optimized, conflict-aware synchronization.
     * It first queries the local database in chunks to find existing building equipments.
     * It then compares the `modifiedAt` timestamps in memory, keeping any remote building equipment that is not older than its local counterpart.
     * Finally, it upserts the outdated building equipments.
     *
     * @param buildingEquipments The list of remote [BuildingEquipment] objects to synchronize locally.
     */
    private suspend fun syncBuildingEquipmentsInDatabase(buildingEquipments: List<BuildingEquipment>) {
        val localBuildingEquipmentsMap = getBuildingEquipmentsByUuidsFromDatabase(uuids = buildingEquipments.map { it.uuid }).associateBy { it.uuid }
        buildingEquipments.forEach { buildingEquipment ->
            val localBuildingEquipment = localBuildingEquipmentsMap[buildingEquipment.uuid]
            if (localBuildingEquipment == null || buildingEquipment.modifiedAt >= localBuildingEquipment.modifiedAt) upsertBuildingEquipmentInDatabase(buildingEquipment = buildingEquipment)
        }
    }

    /**
     * Retrieves building equipments matching the given UUIDs from the local database, in chunks.
     * SQLite limits the number of bound parameters allowed in a single statement, so large lists are split into batches before querying.
     *
     * @param uuids The building equipment UUIDs to look up.
     * @return The matching [BuildingEquipmentSchema] entities, merged across all chunks.
     */
    private suspend fun getBuildingEquipmentsByUuidsFromDatabase(uuids: List<Uuid>): List<BuildingEquipmentSchema> = buildList {
        uuids.chunked(size = ClientConfigs.value.batchSize).forEach { uuidChunk ->
            addAll(elements = buildingEquipmentDao.getBuildingEquipmentsByUuids(buildingEquipmentUuids = uuidChunk).awaitAsList())
        }
    }

    /**
     * Inserts or updates a building equipment in the local database.
     *
     * @param buildingEquipment The [BuildingEquipment] domain model containing the details to save.
     * @return `true` if the building equipment was successfully upserted; `false` if no rows were affected.
     */
    private suspend fun upsertBuildingEquipmentInDatabase(buildingEquipment: BuildingEquipment): Boolean =
        buildingEquipmentDao.upsertBuildingEquipment(BuildingEquipment = buildingEquipment.toBuildingEquipmentSchema()) > 0

    companion object {
        private const val TAG = "BuildingEquipmentGateway"
    }
}
