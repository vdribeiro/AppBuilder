package com.app.builder.domain.gateway.buildinguser

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
import com.app.builder.data.database.BuildingSchema
import com.app.builder.data.database.BuildingUserSchema
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
import com.app.builder.domain.Building
import com.app.builder.domain.BuildingUser
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.User
import com.app.builder.domain.gateway.account.validatePermission
import com.app.builder.domain.gateway.authentication.toUser
import com.app.builder.domain.gateway.building.toBuilding
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.domain.scheduler.JobResult.NoOp.toJobResult
import com.app.builder.domain.scheduler.Scheduler
import com.app.builder.domain.scheduler.toHeaderMap
import database.AppDatabase

/**
 * Gateway implementation for Building User Use Cases and Repository.
 *
 * @property account The account of the current user.
 * @property database The SQLite database instance for building user data.
 * @property httpClient The HTTP client used for network operations.
 * @property scheduler The job scheduler for background building users.
 */
class BuildingUserGateway(
    private val account: Account,
    private val database: AppDatabase,
    private val httpClient: HttpClient,
    private val scheduler: Scheduler
): BuildingUserUseCases, BuildingUserRepository {

    /** Query interface for the building users table. */
    private val buildingUserDao = database.buildingUserQueries

    override fun observeUsersInBuilding(buildingUuid: Uuid): Flow<List<User>> {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_USER, permission = Permission.READ)?.let { return emptyFlow() }

        return buildingUserDao.getUsersInBuilding(buildingUuid = buildingUuid, mapper = ::UserSchema)
            .asFlow { it.toUser() }
            .onStart { queueSync() }
            .flowOn(context = Dispatcher.IO)
            .catch {
                Telemetry.error(tag = TAG, message = "Unable to observe the users in building $buildingUuid", throwable = it)
                emit(value = emptyList())
            }
    }

    override fun observeBuildingsOfUser(userUuid: Uuid): Flow<List<Building>> {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_USER, permission = Permission.READ)?.let { return emptyFlow() }

        return buildingUserDao.getBuildingsOfUser(userUuid = userUuid, mapper = ::BuildingSchema)
            .asFlow { it.toBuilding() }
            .onStart { queueSync() }
            .flowOn(context = Dispatcher.IO)
            .catch {
                Telemetry.error(tag = TAG, message = "Unable to observe the buildings of user $userUuid", throwable = it)
                emit(value = emptyList())
            }
    }

    override suspend fun upsertBuildingUser(buildingUser: BuildingUser) = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_USER, permission = Permission.WRITE)?.let { return@withContext }

        database.safeTransaction {
            upsertBuildingUserInDatabase(buildingUser = buildingUser)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert building user", throwable = it)
        }
        val job = Job(
            userUuid = account.userUuid,
            entityUuid = buildingUser.uuid,
            entityType = EntityType.BUILDING_USER,
            type = Job.Type.POST,
            payload = encode(value = buildingUser)
        )
        scheduler.queue(job = job)
    }

    override suspend fun syncBuildingUsers() = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_USER, permission = Permission.READ)?.let { return@withContext }

        queueSync()
    }

    override suspend fun syncBuildingUser(uuid: Uuid) = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_USER, permission = Permission.READ)?.let { return@withContext }

        val job = Job(
            userUuid = account.userUuid,
            entityUuid = uuid,
            entityType = EntityType.BUILDING_USER,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun executeSyncBuildingUsers(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_USER, permission = Permission.READ)?.let { return@withContext it.toJobResult() }

        val syncStartUtc = now().toString()
        val sync = AppFile.Sync.cache().value.orEmpty()
        val lastSync = sync[AppFile.Sync.Key.BUILDING_USERS_LAST_SYNC_UTC]
        val queryMap: Map<Query, String> = buildMap { if (!lastSync.isNullOrBlank()) put(key = Query.LastSyncUtc, value = lastSync) }

        val result = httpClient.getPaginated<BuildingUser>(
            request = HttpRequest(
                url = URL.BuildingUsers,
                headerMap = job.toHeaderMap(),
                queryMap = queryMap
            ),
            pageSize = ServerConfigs.value.pageSize,
            cursorOf = { it.modifiedAt.toString() to it.uuid.toString() }
        ) { buildingUsers ->
            database.safeTransaction {
                syncBuildingUsersInDatabase(buildingUsers = buildingUsers)
            }.fold(onSuccess = { HttpResult.Success(data = Unit) }, onFailure = { HttpResult.Error(error = it) })
        }

        when (result) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to sync building users", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                AppFile.Sync.save { it.orEmpty().plus(pair = (AppFile.Sync.Key.BUILDING_USERS_LAST_SYNC_UTC to syncStartUtc)) }
                Telemetry.info(tag = TAG, message = "Successfully fetched building users")
                JobResult.Success
            }
        }
    }

    override suspend fun executeSyncBuildingUser(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_USER, permission = Permission.READ)?.let { return@withContext it.toJobResult() }

        val entityUuid = job.entityUuid ?: return@withContext Throwable(message = "Missing entity uuid").toJobResult()

        when (val result = httpClient.get<BuildingUser>(
            request = HttpRequest(
                url = URL.BuildingUsers,
                headerMap = job.toHeaderMap()
            ),
            uuid = entityUuid
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to get remote building user $entityUuid", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncBuildingUserInDatabase(buildingUser = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync building user $entityUuid", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully fetched building user $entityUuid")
                JobResult.Success
            }
        }
    }

    override suspend fun executeUpsertBuildingUser(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.BUILDING_USER, permission = Permission.WRITE)?.let { return@withContext it.toJobResult() }

        val buildingUser = job.payload?.let { decode<BuildingUser>(value = it) } ?: run {
            val throwable = Throwable(message = "Unable to decode building user")
            Telemetry.error(tag = TAG, message = "Invalid building user payload", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        when (val result = httpClient.post<BuildingUser, BuildingUser>(
            request = HttpRequest(
                url = URL.BuildingUsers,
                headerMap = job.toHeaderMap(),
            ),
            body = buildingUser
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to post building user", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncBuildingUserInDatabase(buildingUser = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync building user", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully upserted building user")
                JobResult.Success
            }
        }
    }

    /** Queues a job fetching the building users changed since the last sync. */
    private suspend fun queueSync() {
        val job = Job(
            userUuid = account.userUuid,
            entityType = EntityType.BUILDING_USER,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    /**
     * Synchronizes a single remote building user with the local database.
     * This function performs a conflict-aware update by fetching the local equivalent of the building user and comparing their `modifiedAt` timestamps.
     * The building user is only saved to the database if it does not exist locally, or if the remote version is not older than the local one.
     *
     * @param buildingUser The remote [BuildingUser] domain model to evaluate and potentially save.
     */
    private suspend fun syncBuildingUserInDatabase(buildingUser: BuildingUser) {
        val localBuildingUser = buildingUserDao.getBuildingUser(buildingUserUuid = buildingUser.uuid).awaitAsOneOrNull()
        if (localBuildingUser == null || buildingUser.modifiedAt >= localBuildingUser.modifiedAt) upsertBuildingUserInDatabase(buildingUser = buildingUser)
    }

    /**
     * Synchronizes a batch of remote building users with the local database.
     * This function performs an optimized, conflict-aware synchronization.
     * It first queries the local database in chunks to find existing building users.
     * It then compares the `modifiedAt` timestamps in memory, keeping any remote building user that is not older than its local counterpart.
     * Finally, it upserts the outdated building users.
     *
     * @param buildingUsers The list of remote [BuildingUser] objects to synchronize locally.
     */
    private suspend fun syncBuildingUsersInDatabase(buildingUsers: List<BuildingUser>) {
        val localBuildingUsersMap = getBuildingUsersByUuidsFromDatabase(uuids = buildingUsers.map { it.uuid }).associateBy { it.uuid }
        buildingUsers.forEach { buildingUser ->
            val localBuildingUser = localBuildingUsersMap[buildingUser.uuid]
            if (localBuildingUser == null || buildingUser.modifiedAt >= localBuildingUser.modifiedAt) upsertBuildingUserInDatabase(buildingUser = buildingUser)
        }
    }

    /**
     * Retrieves building users matching the given UUIDs from the local database, in chunks.
     * SQLite limits the number of bound parameters allowed in a single statement, so large lists are split into batches before querying.
     *
     * @param uuids The building user UUIDs to look up.
     * @return The matching [BuildingUserSchema] entities, merged across all chunks.
     */
    private suspend fun getBuildingUsersByUuidsFromDatabase(uuids: List<Uuid>): List<BuildingUserSchema> = buildList {
        uuids.chunked(size = ClientConfigs.value.batchSize).forEach { uuidChunk ->
            addAll(elements = buildingUserDao.getBuildingUsersByUuids(buildingUserUuids = uuidChunk).awaitAsList())
        }
    }

    /**
     * Inserts or updates a building user in the local database.
     *
     * @param buildingUser The [BuildingUser] domain model containing the details to save.
     * @return `true` if the building user was successfully upserted; `false` if no rows were affected.
     */
    private suspend fun upsertBuildingUserInDatabase(buildingUser: BuildingUser): Boolean =
        buildingUserDao.upsertBuildingUser(BuildingUser = buildingUser.toBuildingUserSchema()) > 0

    companion object {
        private const val TAG = "BuildingUserGateway"
    }
}
