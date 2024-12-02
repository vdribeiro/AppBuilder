package com.app.builder.domain.gateway.registry

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.now
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.asFlow
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.HttpResult
import com.app.builder.data.http.Query
import com.app.builder.data.http.URL
import com.app.builder.data.http.getPaginated
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.Registry
import com.app.builder.domain.User
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.domain.scheduler.JobResult.NoOp.toJobResult
import com.app.builder.domain.scheduler.Scheduler
import com.app.builder.domain.scheduler.toHeaderMap
import database.AppDatabase

/**
 * Gateway implementation for Registry Use Cases and Repository.
 *
 * @property user The current user.
 * @property database The SQLite database instance for registry data.
 * @property httpClient The HTTP client used for network operations.
 * @property scheduler The job scheduler for background registries.
 */
class RegistryGateway(
    private val user: User,
    private val database: AppDatabase,
    private val httpClient: HttpClient,
    private val scheduler: Scheduler
): RegistryUseCases, RegistryRepository {

    /** Query interface for the registries table. */
    private val registryDao = database.registryQueries

    override fun observeRegistries(): Flow<List<Registry>> = if (!user.hasPermission(entityType = EntityType.REGISTRY, permission = Permission.READ)) {
        val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.REGISTRY}")
        Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
        return emptyFlow()
    } else registryDao.getRegistries()
        .asFlow { it.toRegistry() }
        .onStart {
            val job = Job(
                userUuid = user.uuid,
                entityType = EntityType.REGISTRY,
                type = Job.Type.GET,
            )
            scheduler.queue(job = job)
        }
        .flowOn(context = Dispatcher.IO)
        .catch {
            Telemetry.error(tag = TAG, message = "Unable to observe registries", throwable = it)
            emit(value = emptyList())
        }

    override fun observeRegistry(uuid: Uuid): Flow<Registry?> = if (!user.hasPermission(entityType = EntityType.REGISTRY, permission = Permission.READ)) {
        val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.REGISTRY}")
        Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
        return emptyFlow()
    } else registryDao.getRegistry(requestUuid = uuid)
        .asFlow { it.toRegistry() }
        .map { it.firstOrNull() }
        .catch {
            Telemetry.error(tag = TAG, message = "Unable to observe registry", throwable = it)
            emit(value = null)
        }

    override suspend fun syncRegistries() = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.REGISTRY, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.REGISTRY}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext
        }

        val job = Job(
            userUuid = user.uuid,
            entityType = EntityType.REGISTRY,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun executeSyncRegistries(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.REGISTRY, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.REGISTRY}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        val syncStartUtc = now().toString()
        val sync = AppFile.Sync.cache().value.orEmpty()
        val lastSync = sync[AppFile.Sync.Key.REGISTRIES_LAST_SYNC_UTC]
        val queryMap: Map<Query, String> = buildMap { if (!lastSync.isNullOrBlank()) put(key = Query.LastSyncUtc, value = lastSync) }

        val result = httpClient.getPaginated<Registry>(
            request = HttpRequest(
                url = URL.Registries,
                headerMap = job.toHeaderMap(),
                queryMap = queryMap
            ),
            pageSize = ServerConfigs.configs.pageSize,
            cursorOf = { it.requestUtc.toString() to it.requestUuid.toString() }
        ) { registries ->
            database.safeTransaction {
                registries.forEach { upsertRegistryInDatabase(registry = it) }
            }.fold(onSuccess = { HttpResult.Success(data = Unit) }, onFailure = { HttpResult.Error(error = it) })
        }

        when (result) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to sync registries", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                AppFile.Sync.save { it.orEmpty().plus(pair = (AppFile.Sync.Key.REGISTRIES_LAST_SYNC_UTC to syncStartUtc)) }
                Telemetry.info(tag = TAG, message = "Successfully fetched registries")
                JobResult.Success
            }
        }
    }

    /**
     * Inserts or updates a registry in the local database.
     *
     * @param registry The [Registry] domain model containing the details to save.
     * @return `true` if the registry record was successfully upserted; `false` if no rows were affected.
     */
    private suspend fun upsertRegistryInDatabase(registry: Registry): Boolean =
        registryDao.upsertRegistry(Registry = registry.toRegistrySchema()) > 0

    companion object {
        private const val TAG = "RegistryGateway"
    }
}
