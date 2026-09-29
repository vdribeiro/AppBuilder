package com.app.builder.domain.gateway.building

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
import com.app.builder.data.database.BuildingSchema
import com.app.builder.data.database.BuildingUserJoinSchema
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
import com.app.builder.domain.Building
import com.app.builder.domain.User
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.domain.scheduler.JobResult.NoOp.toJobResult
import com.app.builder.domain.scheduler.Scheduler
import com.app.builder.domain.scheduler.toHeaderMap
import database.AppDatabase

/**
 * Gateway implementation for Building Use Cases and Repository.
 *
 * @property user The current user.
 * @property database The SQLite database instance for building data.
 * @property httpClient The HTTP client used for network operations.
 * @property scheduler The job scheduler for background tasks.
 */
class BuildingGateway(
    private val user: User,
    private val database: AppDatabase,
    private val httpClient: HttpClient,
    private val scheduler: Scheduler
): BuildingUseCases, BuildingRepository {

    /** Query interface for the buildings table. */
    private val buildingDao = database.buildingQueries
    /** Query interface for the building-user join table. */
    private val buildingUserJoinDao = database.buildingUserJoinQueries

    override fun observeBuildings(): Flow<List<Building>> = if (!user.hasPermission(entityType = EntityType.BUILDING, permission = Permission.READ)) {
        val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.BUILDING}")
        Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
        return emptyFlow()
    } else buildingDao.getBuildings(userUuid = user.uuid)
        .asFlow { it.toBuilding() }
        .onStart {
            val job = Job(
                userUuid = user.uuid,
                entityType = EntityType.BUILDING,
                type = Job.Type.GET,
            )
            scheduler.queue(job = job)
        }
        .flowOn(context = Dispatcher.IO)
        .catch {
            Telemetry.error(tag = TAG, message = "Unable to observe buildings", throwable = it)
            emit(value = emptyList())
        }

    override fun observeBuilding(uuid: Uuid): Flow<Building?> = if (!user.hasPermission(entityType = EntityType.BUILDING, permission = Permission.READ)) {
        val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.BUILDING}")
        Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
        return emptyFlow()
    } else buildingDao.getBuilding(userUuid = user.uuid, buildingUuid = uuid)
        .asFlow { it.toBuilding() }
        .map { it.firstOrNull() }
        .catch {
            Telemetry.error(tag = TAG, message = "Unable to observe building", throwable = it)
            emit(value = null)
        }

    override suspend fun upsertBuilding(building: Building) = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.BUILDING, permission = Permission.WRITE)) {
            val throwable = Throwable(message = "User does not have ${Permission.WRITE} permission for ${EntityType.BUILDING}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext
        }

        database.safeTransaction {
            upsertBuildingInDatabase(building = building)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert building", throwable = it)
        }
        val job = Job(
            userUuid = user.uuid,
            entityUuid = building.uuid,
            entityType = EntityType.BUILDING,
            type = Job.Type.POST,
            payload = encode(value = building)
        )
        scheduler.queue(job = job)
    }

    override suspend fun syncBuildings() = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.BUILDING, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.BUILDING}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext
        }

        val job = Job(
            userUuid = user.uuid,
            entityType = EntityType.BUILDING,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun syncBuilding(uuid: Uuid) = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.BUILDING, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.BUILDING}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext
        }

        val job = Job(
            userUuid = user.uuid,
            entityUuid = uuid,
            entityType = EntityType.BUILDING,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun executeSyncBuildings(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.BUILDING, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.BUILDING}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        val syncStartUtc = now().toString()
        val sync = AppFile.Sync.cache().value.orEmpty()
        val lastSync = sync[AppFile.Sync.Key.BUILDINGS_LAST_SYNC_UTC]
        val queryMap: Map<Query, String> = buildMap { if (!lastSync.isNullOrBlank()) put(key = Query.LastSyncUtc, value = lastSync) }

        val result = httpClient.getPaginated<Building>(
            request = HttpRequest(
                url = URL.Buildings,
                headerMap = job.toHeaderMap(),
                queryMap = queryMap
            ),
            pageSize = ServerConfigs.configs.pageSize,
            cursorOf = { it.modifiedAt.toString() to it.uuid.toString() }
        ) { buildings ->
            database.safeTransaction {
                syncBuildingsInDatabase(buildings = buildings)
            }.fold(onSuccess = { HttpResult.Success(data = Unit) }, onFailure = { HttpResult.Error(error = it) })
        }

        when (result) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to sync buildings", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                AppFile.Sync.save { it.orEmpty().plus(pair = (AppFile.Sync.Key.BUILDINGS_LAST_SYNC_UTC to syncStartUtc)) }
                Telemetry.info(tag = TAG, message = "Successfully fetched buildings")
                JobResult.Success
            }
        }
    }

    override suspend fun executeSyncBuilding(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.BUILDING, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.BUILDING}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        val entityUuid = job.entityUuid ?: return@withContext Throwable(message = "Missing entity uuid").toJobResult()

        when (val result = httpClient.get<Building>(
            request = HttpRequest(
                url = URL.Buildings,
                headerMap = job.toHeaderMap()
            ),
            uuid = entityUuid
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to get remote building $entityUuid", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncBuildingInDatabase(building = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync building $entityUuid", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully fetched building $entityUuid")
                JobResult.Success
            }
        }
    }

    override suspend fun executeUpsertBuilding(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.BUILDING, permission = Permission.WRITE)) {
            val throwable = Throwable(message = "User does not have ${Permission.WRITE} permission for ${EntityType.BUILDING}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        val building = job.payload?.let { decode<Building>(value = it) } ?: run {
            val throwable = Throwable(message = "Unable to decode building")
            Telemetry.error(tag = TAG, message = "Invalid building payload", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        when (val result = httpClient.post<Building, Building>(
            request = HttpRequest(
                url = URL.Buildings,
                headerMap = job.toHeaderMap(),
            ),
            body = building
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to post building", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncBuildingInDatabase(building = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync building", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully upserted building")
                JobResult.Success
            }
        }
    }

    /**
     * Synchronizes a single remote building with the local database.
     * This function performs a conflict-aware update by fetching the local equivalent of the building and comparing their `modifiedAt` timestamps.
     * The building is only saved to the database if it does not exist locally, or if the remote version is not older than the local one.
     *
     * @param building The remote [Building] domain model to evaluate and potentially save.
     */
    private suspend fun syncBuildingInDatabase(building: Building) {
        val localBuilding = buildingDao.getBuilding(userUuid = user.uuid, buildingUuid = building.uuid).awaitAsOneOrNull()
        if (localBuilding == null || building.modifiedAt >= localBuilding.modifiedAt) upsertBuildingInDatabase(building = building)
    }

    /**
     * Synchronizes a batch of remote buildings with the local database.
     * This function performs an optimized, conflict-aware synchronization.
     * It first queries the local database in chunks to find existing buildings.
     * It then compares the `modifiedAt` timestamps in memory, keeping any remote building that is not older than its local counterpart.
     * Finally, it upserts the outdated buildings.
     *
     * @param buildings The list of remote [Building] objects to synchronize locally.
     */
    private suspend fun syncBuildingsInDatabase(buildings: List<Building>) {
        val localBuildingsMap = getBuildingsByUuidsFromDatabase(uuids = buildings.map { it.uuid }).associateBy { it.uuid }
        buildings.forEach { building ->
            val localBuilding = localBuildingsMap[building.uuid]
            if (localBuilding == null || building.modifiedAt >= localBuilding.modifiedAt) upsertBuildingInDatabase(building = building)
        }
    }

    /**
     * Retrieves buildings matching the given UUIDs from the local database, in chunks.
     * SQLite limits the number of bound parameters allowed in a single statement, so large lists are split into batches before querying.
     *
     * @param uuids The building UUIDs to look up, scoped to the currently active user.
     * @return The matching [BuildingSchema] entities, merged across all chunks.
     */
    private suspend fun getBuildingsByUuidsFromDatabase(uuids: List<Uuid>): List<BuildingSchema> = buildList {
        uuids.chunked(size = ClientConfigs.configs.batchSize).forEach { uuidChunk ->
            addAll(elements = buildingDao.getBuildingsByUuids(userUuid = user.uuid, buildingUuids = uuidChunk).awaitAsList())
        }
    }

    /**
     * Inserts or updates a building and its user association in the local database.
     *
     * @param building The [Building] domain model containing the details to save.
     * @return `true` if the building and its join record were successfully upserted; `false` if no rows were affected.
     */
    private suspend fun upsertBuildingInDatabase(building: Building): Boolean =
        (buildingDao.upsertBuilding(Building = building.toBuildingSchema())
                + buildingUserJoinDao.upsertBuildingUserJoin(BuildingUserJoin = BuildingUserJoinSchema(buildingUuid = building.uuid, userUuid = user.uuid))) > 0

    companion object {
        private const val TAG = "BuildingGateway"
    }
}
