package com.app.builder.domain.gateway.user

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
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.User
import com.app.builder.domain.gateway.authentication.toUser
import com.app.builder.domain.gateway.authentication.toUserSchema
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.domain.scheduler.JobResult.NoOp.toJobResult
import com.app.builder.domain.scheduler.Scheduler
import com.app.builder.domain.scheduler.toHeaderMap
import database.AppDatabase

/**
 * Gateway implementation for User Use Cases and Repository.
 *
 * @property user The current user.
 * @property database The SQLite database instance for user data.
 * @property httpClient The HTTP client used for network operations.
 * @property scheduler The job scheduler for background tasks.
 */
class UserGateway(
    private val user: User,
    private val database: AppDatabase,
    private val httpClient: HttpClient,
    private val scheduler: Scheduler
): UserUseCases, UserRepository {

    /** Query interface for the users table. */
    private val userDao = database.userQueries

    override fun observeUsers(): Flow<List<User>> = if (!user.hasPermission(entityType = EntityType.USER, permission = Permission.READ)) {
        val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.USER}")
        Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
        return emptyFlow()
    } else userDao.getUsers()
        .asFlow { it.toUser() }
        .onStart {
            val job = Job(
                userUuid = user.uuid,
                entityType = EntityType.USER,
                type = Job.Type.GET,
            )
            scheduler.queue(job = job)
        }
        .flowOn(context = Dispatcher.IO)
        .catch {
            Telemetry.error(tag = TAG, message = "Unable to observe users", throwable = it)
            emit(value = emptyList())
        }

    override fun observeUser(uuid: Uuid): Flow<User?> = if (!user.hasPermission(entityType = EntityType.USER, permission = Permission.READ)) {
        val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.USER}")
        Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
        return emptyFlow()
    } else userDao.getUser(userUuid = uuid)
        .asFlow { it.toUser() }
        .map { it.firstOrNull() }
        .catch {
            Telemetry.error(tag = TAG, message = "Unable to observe user", throwable = it)
            emit(value = null)
        }

    override suspend fun upsertUser(user: User) = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.USER, permission = Permission.WRITE)) {
            val throwable = Throwable(message = "User does not have ${Permission.WRITE} permission for ${EntityType.USER}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext
        }

        database.safeTransaction {
            upsertUserInDatabase(user = user)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert user", throwable = it)
            return@withContext
        }
        val job = Job(
            userUuid = user.uuid,
            entityUuid = user.uuid,
            entityType = EntityType.USER,
            type = Job.Type.POST,
            payload = encode(value = user)
        )
        scheduler.queue(job = job)
    }

    override suspend fun syncUsers() = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.USER, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.USER}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext
        }

        val job = Job(
            userUuid = user.uuid,
            entityType = EntityType.USER,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun syncUser(uuid: Uuid) = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.USER, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.USER}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext
        }

        val job = Job(
            userUuid = user.uuid,
            entityUuid = uuid,
            entityType = EntityType.USER,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun executeSyncUsers(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.USER, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.USER}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        val syncStartUtc = now().toString()
        val sync = AppFile.Sync.cache().value.orEmpty()
        val lastSync = sync[AppFile.Sync.Key.USERS_LAST_SYNC_UTC]
        val queryMap: Map<Query, String> = buildMap { if (!lastSync.isNullOrBlank()) put(key = Query.LastSyncUtc, value = lastSync) }

        val result = httpClient.getPaginated<User>(
            request = HttpRequest(
                url = URL.Users,
                headerMap = job.toHeaderMap(),
                queryMap = queryMap
            ),
            pageSize = ServerConfigs.configs.pageSize,
            cursorOf = { it.modifiedAt.toString() to it.uuid.toString() }
        ) { users ->
            database.safeTransaction {
                syncUsersInDatabase(users = users)
            }.fold(onSuccess = { HttpResult.Success(data = Unit) }, onFailure = { HttpResult.Error(error = it) })
        }

        when (result) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to sync users", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                AppFile.Sync.save { it.orEmpty().plus(pair = (AppFile.Sync.Key.USERS_LAST_SYNC_UTC to syncStartUtc)) }
                Telemetry.info(tag = TAG, message = "Successfully fetched users")
                JobResult.Success
            }
        }
    }

    override suspend fun executeSyncUser(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.USER, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.USER}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        val entityUuid = job.entityUuid ?: return@withContext Throwable(message = "Missing entity uuid").toJobResult()

        when (val result = httpClient.get<User>(
            request = HttpRequest(
                url = URL.Users,
                headerMap = job.toHeaderMap()
            ),
            uuid = entityUuid
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to get remote user $entityUuid", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncUserInDatabase(user = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync user $entityUuid", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully fetched user $entityUuid")
                JobResult.Success
            }
        }
    }

    override suspend fun executeUpsertUser(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.USER, permission = Permission.WRITE)) {
            val throwable = Throwable(message = "User does not have ${Permission.WRITE} permission for ${EntityType.USER}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        val user = job.payload?.let { decode<User>(value = it) } ?: run {
            val throwable = Throwable(message = "Unable to decode user")
            Telemetry.error(tag = TAG, message = "Invalid user payload", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        when (val result = httpClient.post<User, User>(
            request = HttpRequest(
                url = URL.Users,
                headerMap = job.toHeaderMap(),
            ),
            body = user
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to post user", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncUserInDatabase(user = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync user", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully upserted user")
                JobResult.Success
            }
        }
    }

    /**
     * Synchronizes a single remote user with the local database.
     * This function performs a conflict-aware update by fetching the local equivalent of the user and comparing their `modifiedAt` timestamps.
     * The user is only saved to the database if it does not exist locally, or if the remote version is not older than the local one.
     *
     * @param user The remote [User] domain model to evaluate and potentially save.
     */
    private suspend fun syncUserInDatabase(user: User) {
        val localUser = userDao.getUser(userUuid = user.uuid).awaitAsOneOrNull()
        if (localUser == null || user.modifiedAt >= localUser.modifiedAt) upsertUserInDatabase(user = user)
    }

    /**
     * Synchronizes a batch of remote users with the local database.
     * This function performs an optimized, conflict-aware synchronization.
     * It first queries the local database in chunks to find existing users.
     * It then compares the `modifiedAt` timestamps in memory, keeping any remote user that is not older than its local counterpart.
     * Finally, it upserts the outdated users.
     *
     * @param users The list of remote [User] objects to synchronize locally.
     */
    private suspend fun syncUsersInDatabase(users: List<User>) {
        val localUsersMap = getUsersByUuidsFromDatabase(uuids = users.map { it.uuid }).associateBy { it.uuid }
        for (user in users) {
            val localUser = localUsersMap[user.uuid]
            if (localUser == null || user.modifiedAt >= localUser.modifiedAt) upsertUserInDatabase(user = user)
        }
    }

    /**
     * Retrieves users matching the given UUIDs from the local database, in chunks.
     * SQLite limits the number of bound parameters allowed in a single statement,
     * so large lists are split into batches before querying.
     *
     * @param uuids The user UUIDs to look up. Unlike the task equivalent, this lookup is not scoped to the current user.
     * @return The matching [UserSchema] entities, merged across all chunks.
     */
    private suspend fun getUsersByUuidsFromDatabase(uuids: List<Uuid>): List<UserSchema> = buildList {
        uuids.chunked(size = ClientConfigs.configs.batchSize).forEach { uuidChunk ->
            addAll(elements = userDao.getUsersByUuids(userUuids = uuidChunk).awaitAsList())
        }
    }

    /**
     * Inserts or updates a user in the local database.
     *
     * @param user The [User] domain model containing the details to save.
     * @return `true` if the user record was successfully upserted; `false` if no rows were affected.
     */
    private suspend fun upsertUserInDatabase(user: User): Boolean =
        userDao.upsertUser(User = user.toUserSchema()) > 0

    companion object {
        private const val TAG = "UserGateway"
    }
}
