package com.app.builder.domain.gateway.config

import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.HttpResult
import com.app.builder.data.http.URL
import com.app.builder.data.http.get
import com.app.builder.data.http.post
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.User
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.domain.scheduler.JobResult.NoOp.toJobResult
import com.app.builder.domain.scheduler.Scheduler
import com.app.builder.domain.scheduler.toHeaderMap

/**
 * Gateway implementation for Config Use Cases and Repository.
 *
 * @property user The current user.
 * @property httpClient The HTTP client used for network operations.
 * @property scheduler The job scheduler for background tasks.
 */
class ConfigGateway(
    private val user: User,
    private val httpClient: HttpClient,
    private val scheduler: Scheduler
): ConfigUseCases, ConfigRepository {

    override suspend fun getServerFeatureFlags(): ServerFlags? = withContext(context = Dispatcher.IO) {
        when (val result = httpClient.get<ServerFlags>(request = HttpRequest(url = URL.ServerFlags))) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to get server feature flags", throwable = result.error)
                null
            }

            is HttpResult.Success -> result.data.also { Telemetry.info(tag = TAG, message = "Successfully fetched server feature flags: $it") }
        }
    }

    override suspend fun getServerConfigs(): ServerConfigs? = withContext(context = Dispatcher.IO) {
        when (val result = httpClient.get<ServerConfigs>(request = HttpRequest(url = URL.ServerConfigs))) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to get server configs", throwable = result.error)
                null
            }

            is HttpResult.Success -> result.data.also { Telemetry.info(tag = TAG, message = "Successfully fetched server configs: $it") }
        }
    }

    override suspend fun updateFeatureFlags(flags: ClientFlags): ClientFlags? = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.CLIENT_FLAG, permission = Permission.WRITE)) {
            Telemetry.info(tag = TAG, message = "User does not have ${Permission.WRITE} permission for ${EntityType.CLIENT_FLAG}")
            return@withContext null
        }

        when (val result = httpClient.post<ClientFlags, ClientFlags>(request = HttpRequest(url = URL.ClientFlags), body = flags)) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to update client feature flags", throwable = result.error)
                null
            }

            is HttpResult.Success -> {
                AppFile.ClientFeatureFlags.save { result.data }
                ClientFlags.set { result.data }
                Telemetry.info(tag = TAG, message = "Successfully updated client feature flags: ${result.data}")
                result.data
            }
        }
    }

    override suspend fun updateConfigs(configs: ClientConfigs): ClientConfigs? = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.CLIENT_CONFIG, permission = Permission.WRITE)) {
            Telemetry.info(tag = TAG, message = "User does not have ${Permission.WRITE} permission for ${EntityType.CLIENT_CONFIG}")
            return@withContext null
        }

        when (val result = httpClient.post<ClientConfigs, ClientConfigs>(request = HttpRequest(url = URL.ClientConfigs), body = configs)) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to update client configs", throwable = result.error)
                null
            }

            is HttpResult.Success -> {
                AppFile.ClientRemoteConfigs.save { result.data }
                ClientConfigs.set { result.data }
                Telemetry.info(tag = TAG, message = "Successfully updated client configs: ${result.data}")
                result.data
            }
        }
    }

    override suspend fun updateServerFeatureFlags(flags: ServerFlags): ServerFlags? = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.SERVER_FLAG, permission = Permission.WRITE)) {
            Telemetry.info(tag = TAG, message = "User does not have ${Permission.WRITE} permission for ${EntityType.SERVER_FLAG}")
            return@withContext null
        }

        when (val result = httpClient.post<ServerFlags, ServerFlags>(request = HttpRequest(url = URL.ServerFlags), body = flags)) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to update server feature flags", throwable = result.error)
                null
            }

            is HttpResult.Success -> {
                AppFile.ServerFeatureFlags.save { result.data }
                ServerFlags.set { result.data }
                Telemetry.info(tag = TAG, message = "Successfully updated server feature flags: ${result.data}")
                result.data
            }
        }
    }

    override suspend fun updateServerConfigs(configs: ServerConfigs): ServerConfigs? = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.SERVER_CONFIG, permission = Permission.WRITE)) {
            Telemetry.info(tag = TAG, message = "User does not have ${Permission.WRITE} permission for ${EntityType.SERVER_CONFIG}")
            return@withContext null
        }

        when (val result = httpClient.post<ServerConfigs, ServerConfigs>(request = HttpRequest(url = URL.ServerConfigs), body = configs)) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to update server configs", throwable = result.error)
                null
            }

            is HttpResult.Success -> {
                AppFile.ServerRemoteConfigs.save { result.data }
                ServerConfigs.set { result.data }
                Telemetry.info(tag = TAG, message = "Successfully updated server configs: ${result.data}")
                result.data
            }
        }
    }

    override suspend fun load() = withContext(context = Dispatcher.IO) {
        AppFile.ClientFeatureFlags.load()?.run { ClientFlags.set { this } }
        AppFile.ClientRemoteConfigs.load()?.run { ClientConfigs.set { this } }
        AppFile.ServerFeatureFlags.load()?.run { ServerFlags.set { this } }
        AppFile.ServerRemoteConfigs.load()?.run { ServerConfigs.set { this } }
        Unit
    }

    override suspend fun syncFeatureFlags() = withContext(context = Dispatcher.IO) {
        val job = Job(
            userUuid = user.uuid,
            entityType = EntityType.CLIENT_FLAG,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun syncConfigs() = withContext(context = Dispatcher.IO) {
        val job = Job(
            userUuid = user.uuid,
            entityType = EntityType.CLIENT_CONFIG,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun executeSyncFeatureFlags(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        when (val result = httpClient.get<ClientFlags>(
            request = HttpRequest(
                url = URL.ClientFlags,
                headerMap = job.toHeaderMap(),
            )
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to get client feature flags", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                AppFile.ClientFeatureFlags.save { result.data }
                ClientFlags.set { result.data }
                Telemetry.info(tag = TAG, message = "Successfully fetched client feature flags: ${result.data}")
                JobResult.Success
            }
        }
    }

    override suspend fun executeSyncConfigs(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        when (val result = httpClient.get<ClientConfigs>(
            request = HttpRequest(
                url = URL.ClientConfigs,
                headerMap = job.toHeaderMap(),
            )
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to get client configs", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                AppFile.ClientRemoteConfigs.save { result.data }
                ClientConfigs.set { result.data }
                Telemetry.info(tag = TAG, message = "Successfully fetched client configs: ${result.data}")
                JobResult.Success
            }
        }
    }

    companion object {
        private const val TAG = "ConfigGateway"
    }
}
