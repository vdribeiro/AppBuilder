package com.app.builder.domain.gateway.buildingmaterial

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
import com.app.builder.data.database.BuildingMaterialSchema
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
import com.app.builder.domain.BuildingMaterial
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.gateway.account.validatePermission
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.domain.scheduler.JobResult.NoOp.toJobResult
import com.app.builder.domain.scheduler.Scheduler
import com.app.builder.domain.scheduler.toHeaderMap
import database.AppDatabase

/**
 * Gateway implementation for Building Material Use Cases and Repository.
 *
 * @property account The account of the current user.
 * @property database The SQLite database instance for building material data.
 * @property httpClient The HTTP client used for network operations.
 * @property scheduler The job scheduler for background building materials.
 */
class BuildingMaterialGateway(
    private val account: Account,
    private val database: AppDatabase,
    private val httpClient: HttpClient,
    private val scheduler: Scheduler
): BuildingMaterialUseCases, BuildingMaterialRepository {

    /** Query interface for the building materials table. */
    private val buildingMaterialDao = database.buildingMaterialQueries

    override fun observeMaterialsInBuilding(buildingUuid: Uuid): Flow<List<MaterialInBuilding>> {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_MATERIAL, permission = Permission.READ)?.let { return emptyFlow() }

        return buildingMaterialDao.getMaterialsInBuilding(buildingUuid = buildingUuid)
            .asFlow { it.toMaterialInBuilding() }
            .onStart { queueSync() }
            .flowOn(context = Dispatcher.IO)
            .catch {
                Telemetry.error(tag = TAG, message = "Unable to observe the materials in building $buildingUuid", throwable = it)
                emit(value = emptyList())
            }
    }

    override fun observeBuildingsOfMaterial(materialUuid: Uuid): Flow<List<BuildingOfMaterial>> {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_MATERIAL, permission = Permission.READ)?.let { return emptyFlow() }

        return buildingMaterialDao.getBuildingsOfMaterial(materialUuid = materialUuid)
            .asFlow { it.toBuildingOfMaterial() }
            .onStart { queueSync() }
            .flowOn(context = Dispatcher.IO)
            .catch {
                Telemetry.error(tag = TAG, message = "Unable to observe the buildings of material $materialUuid", throwable = it)
                emit(value = emptyList())
            }
    }

    override suspend fun upsertBuildingMaterial(buildingMaterial: BuildingMaterial) = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_MATERIAL, permission = Permission.WRITE)?.let { return@withContext }

        database.safeTransaction {
            upsertBuildingMaterialInDatabase(buildingMaterial = buildingMaterial)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert building material", throwable = it)
        }
        val job = Job(
            userUuid = account.userUuid,
            entityUuid = buildingMaterial.uuid,
            entityType = EntityType.BUILDING_MATERIAL,
            type = Job.Type.POST,
            payload = encode(value = buildingMaterial)
        )
        scheduler.queue(job = job)
    }

    override suspend fun syncBuildingMaterials() = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_MATERIAL, permission = Permission.READ)?.let { return@withContext }

        queueSync()
    }

    override suspend fun syncBuildingMaterial(uuid: Uuid) = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_MATERIAL, permission = Permission.READ)?.let { return@withContext }

        val job = Job(
            userUuid = account.userUuid,
            entityUuid = uuid,
            entityType = EntityType.BUILDING_MATERIAL,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun executeSyncBuildingMaterials(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_MATERIAL, permission = Permission.READ)?.let { return@withContext it.toJobResult() }

        val syncStartUtc = now().toString()
        val sync = AppFile.Sync.cache().value.orEmpty()
        val lastSync = sync[AppFile.Sync.Key.BUILDING_MATERIALS_LAST_SYNC_UTC]
        val queryMap: Map<Query, String> = buildMap { if (!lastSync.isNullOrBlank()) put(key = Query.LastSyncUtc, value = lastSync) }

        val result = httpClient.getPaginated<BuildingMaterial>(
            request = HttpRequest(
                url = URL.BuildingMaterials,
                headerMap = job.toHeaderMap(),
                queryMap = queryMap
            ),
            pageSize = ServerConfigs.value.pageSize,
            cursorOf = { it.modifiedAt.toString() to it.uuid.toString() }
        ) { buildingMaterials ->
            database.safeTransaction {
                syncBuildingMaterialsInDatabase(buildingMaterials = buildingMaterials)
            }.fold(onSuccess = { HttpResult.Success(data = Unit) }, onFailure = { HttpResult.Error(error = it) })
        }

        when (result) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to sync building materials", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                AppFile.Sync.save { it.orEmpty().plus(pair = (AppFile.Sync.Key.BUILDING_MATERIALS_LAST_SYNC_UTC to syncStartUtc)) }
                Telemetry.info(tag = TAG, message = "Successfully fetched building materials")
                JobResult.Success
            }
        }
    }

    override suspend fun executeSyncBuildingMaterial(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_MATERIAL, permission = Permission.READ)?.let { return@withContext it.toJobResult() }

        val entityUuid = job.entityUuid ?: return@withContext Throwable(message = "Missing entity uuid").toJobResult()

        when (val result = httpClient.get<BuildingMaterial>(
            request = HttpRequest(
                url = URL.BuildingMaterials,
                headerMap = job.toHeaderMap()
            ),
            uuid = entityUuid
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to get remote building material $entityUuid", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncBuildingMaterialInDatabase(buildingMaterial = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync building material $entityUuid", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully fetched building material $entityUuid")
                JobResult.Success
            }
        }
    }

    override suspend fun executeUpsertBuildingMaterial(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_MATERIAL, permission = Permission.WRITE)?.let { return@withContext it.toJobResult() }

        val buildingMaterial = job.payload?.let { decode<BuildingMaterial>(value = it) } ?: run {
            val throwable = Throwable(message = "Unable to decode building material")
            Telemetry.error(tag = TAG, message = "Invalid building material payload", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        when (val result = httpClient.post<BuildingMaterial, BuildingMaterial>(
            request = HttpRequest(
                url = URL.BuildingMaterials,
                headerMap = job.toHeaderMap(),
            ),
            body = buildingMaterial
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to post building material", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncBuildingMaterialInDatabase(buildingMaterial = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync building material", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully upserted building material")
                JobResult.Success
            }
        }
    }

    /** Queues a job fetching the building materials changed since the last sync. */
    private suspend fun queueSync() {
        val job = Job(
            userUuid = account.userUuid,
            entityType = EntityType.BUILDING_MATERIAL,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    /**
     * Synchronizes a single remote building material with the local database.
     * This function performs a conflict-aware update by fetching the local equivalent of the building material and comparing their `modifiedAt` timestamps.
     * The building material is only saved to the database if it does not exist locally, or if the remote version is not older than the local one.
     *
     * @param buildingMaterial The remote [BuildingMaterial] domain model to evaluate and potentially save.
     */
    private suspend fun syncBuildingMaterialInDatabase(buildingMaterial: BuildingMaterial) {
        val localBuildingMaterial = buildingMaterialDao.getBuildingMaterial(buildingMaterialUuid = buildingMaterial.uuid).awaitAsOneOrNull()
        if (localBuildingMaterial == null || buildingMaterial.modifiedAt >= localBuildingMaterial.modifiedAt) upsertBuildingMaterialInDatabase(buildingMaterial = buildingMaterial)
    }

    /**
     * Synchronizes a batch of remote building materials with the local database.
     * This function performs an optimized, conflict-aware synchronization.
     * It first queries the local database in chunks to find existing building materials.
     * It then compares the `modifiedAt` timestamps in memory, keeping any remote building material that is not older than its local counterpart.
     * Finally, it upserts the outdated building materials.
     *
     * @param buildingMaterials The list of remote [BuildingMaterial] objects to synchronize locally.
     */
    private suspend fun syncBuildingMaterialsInDatabase(buildingMaterials: List<BuildingMaterial>) {
        val localBuildingMaterialsMap = getBuildingMaterialsByUuidsFromDatabase(uuids = buildingMaterials.map { it.uuid }).associateBy { it.uuid }
        buildingMaterials.forEach { buildingMaterial ->
            val localBuildingMaterial = localBuildingMaterialsMap[buildingMaterial.uuid]
            if (localBuildingMaterial == null || buildingMaterial.modifiedAt >= localBuildingMaterial.modifiedAt) upsertBuildingMaterialInDatabase(buildingMaterial = buildingMaterial)
        }
    }

    /**
     * Retrieves building materials matching the given UUIDs from the local database, in chunks.
     * SQLite limits the number of bound parameters allowed in a single statement, so large lists are split into batches before querying.
     *
     * @param uuids The building material UUIDs to look up.
     * @return The matching [BuildingMaterialSchema] entities, merged across all chunks.
     */
    private suspend fun getBuildingMaterialsByUuidsFromDatabase(uuids: List<Uuid>): List<BuildingMaterialSchema> = buildList {
        uuids.chunked(size = ClientConfigs.value.batchSize).forEach { uuidChunk ->
            addAll(elements = buildingMaterialDao.getBuildingMaterialsByUuids(buildingMaterialUuids = uuidChunk).awaitAsList())
        }
    }

    /**
     * Inserts or updates a building material in the local database.
     *
     * @param buildingMaterial The [BuildingMaterial] domain model containing the details to save.
     * @return `true` if the building material was successfully upserted; `false` if no rows were affected.
     */
    private suspend fun upsertBuildingMaterialInDatabase(buildingMaterial: BuildingMaterial): Boolean =
        buildingMaterialDao.upsertBuildingMaterial(BuildingMaterial = buildingMaterial.toBuildingMaterialSchema()) > 0

    companion object {
        private const val TAG = "BuildingMaterialGateway"
    }
}
