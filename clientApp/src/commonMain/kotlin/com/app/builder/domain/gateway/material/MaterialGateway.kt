package com.app.builder.domain.gateway.material

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
import com.app.builder.data.database.MaterialSchema
import com.app.builder.data.database.MaterialUserJoinSchema
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
import com.app.builder.domain.Material
import com.app.builder.domain.User
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.domain.scheduler.JobResult.NoOp.toJobResult
import com.app.builder.domain.scheduler.Scheduler
import com.app.builder.domain.scheduler.toHeaderMap
import database.AppDatabase

/**
 * Gateway implementation for Material Use Cases and Repository.
 *
 * @property user The current user.
 * @property database The SQLite database instance for material data.
 * @property httpClient The HTTP client used for network operations.
 * @property scheduler The job scheduler for background tasks.
 */
class MaterialGateway(
    private val user: User,
    private val database: AppDatabase,
    private val httpClient: HttpClient,
    private val scheduler: Scheduler
): MaterialUseCases, MaterialRepository {

    /** Query interface for the materials table. */
    private val materialDao = database.materialQueries
    /** Query interface for the material-user join table. */
    private val materialUserJoinDao = database.materialUserJoinQueries

    override fun observeMaterials(): Flow<List<Material>> = if (!user.hasPermission(entityType = EntityType.MATERIAL, permission = Permission.READ)) {
        val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.MATERIAL}")
        Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
        return emptyFlow()
    } else materialDao.getMaterials(userUuid = user.uuid)
        .asFlow { it.toMaterial() }
        .onStart {
            val job = Job(
                userUuid = user.uuid,
                entityType = EntityType.MATERIAL,
                type = Job.Type.GET,
            )
            scheduler.queue(job = job)
        }
        .flowOn(context = Dispatcher.IO)
        .catch {
            Telemetry.error(tag = TAG, message = "Unable to observe materials", throwable = it)
            emit(value = emptyList())
        }

    override fun observeMaterial(uuid: Uuid): Flow<Material?> = if (!user.hasPermission(entityType = EntityType.MATERIAL, permission = Permission.READ)) {
        val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.MATERIAL}")
        Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
        return emptyFlow()
    } else materialDao.getMaterial(userUuid = user.uuid, materialUuid = uuid)
        .asFlow { it.toMaterial() }
        .map { it.firstOrNull() }
        .catch {
            Telemetry.error(tag = TAG, message = "Unable to observe material", throwable = it)
            emit(value = null)
        }

    override suspend fun upsertMaterial(material: Material) = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.MATERIAL, permission = Permission.WRITE)) {
            val throwable = Throwable(message = "User does not have ${Permission.WRITE} permission for ${EntityType.MATERIAL}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext
        }

        database.safeTransaction {
            upsertMaterialInDatabase(material = material)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert material", throwable = it)
        }
        val job = Job(
            userUuid = user.uuid,
            entityUuid = material.uuid,
            entityType = EntityType.MATERIAL,
            type = Job.Type.POST,
            payload = encode(value = material)
        )
        scheduler.queue(job = job)
    }

    override suspend fun syncMaterials() = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.MATERIAL, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.MATERIAL}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext
        }

        val job = Job(
            userUuid = user.uuid,
            entityType = EntityType.MATERIAL,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun syncMaterial(uuid: Uuid) = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.MATERIAL, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.MATERIAL}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext
        }

        val job = Job(
            userUuid = user.uuid,
            entityUuid = uuid,
            entityType = EntityType.MATERIAL,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun executeSyncMaterials(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.MATERIAL, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.MATERIAL}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        val syncStartUtc = now().toString()
        val sync = AppFile.Sync.cache().value.orEmpty()
        val lastSync = sync[AppFile.Sync.Key.MATERIALS_LAST_SYNC_UTC]
        val queryMap: Map<Query, String> = buildMap { if (!lastSync.isNullOrBlank()) put(key = Query.LastSyncUtc, value = lastSync) }

        val result = httpClient.getPaginated<Material>(
            request = HttpRequest(
                url = URL.Materials,
                headerMap = job.toHeaderMap(),
                queryMap = queryMap
            ),
            pageSize = ServerConfigs.configs.pageSize,
            cursorOf = { it.modifiedAt.toString() to it.uuid.toString() }
        ) { materials ->
            database.safeTransaction {
                syncMaterialsInDatabase(materials = materials)
            }.fold(onSuccess = { HttpResult.Success(data = Unit) }, onFailure = { HttpResult.Error(error = it) })
        }

        when (result) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to sync materials", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                AppFile.Sync.save { it.orEmpty().plus(pair = (AppFile.Sync.Key.MATERIALS_LAST_SYNC_UTC to syncStartUtc)) }
                Telemetry.info(tag = TAG, message = "Successfully fetched materials")
                JobResult.Success
            }
        }
    }

    override suspend fun executeSyncMaterial(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.MATERIAL, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.MATERIAL}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        val entityUuid = job.entityUuid ?: return@withContext Throwable(message = "Missing entity uuid").toJobResult()

        when (val result = httpClient.get<Material>(
            request = HttpRequest(
                url = URL.Materials,
                headerMap = job.toHeaderMap()
            ),
            uuid = entityUuid
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to get remote material $entityUuid", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncMaterialInDatabase(material = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync material $entityUuid", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully fetched material $entityUuid")
                JobResult.Success
            }
        }
    }

    override suspend fun executeUpsertMaterial(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.MATERIAL, permission = Permission.WRITE)) {
            val throwable = Throwable(message = "User does not have ${Permission.WRITE} permission for ${EntityType.MATERIAL}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        val material = job.payload?.let { decode<Material>(value = it) } ?: run {
            val throwable = Throwable(message = "Unable to decode material")
            Telemetry.error(tag = TAG, message = "Invalid material payload", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        when (val result = httpClient.post<Material, Material>(
            request = HttpRequest(
                url = URL.Materials,
                headerMap = job.toHeaderMap(),
            ),
            body = material
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to post material", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncMaterialInDatabase(material = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync material", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully upserted material")
                JobResult.Success
            }
        }
    }

    /**
     * Synchronizes a single remote material with the local database.
     * This function performs a conflict-aware update by fetching the local equivalent of the material and comparing their `modifiedAt` timestamps.
     * The material is only saved to the database if it does not exist locally, or if the remote version is not older than the local one.
     *
     * @param material The remote [Material] domain model to evaluate and potentially save.
     */
    private suspend fun syncMaterialInDatabase(material: Material) {
        val localMaterial = materialDao.getMaterial(userUuid = user.uuid, materialUuid = material.uuid).awaitAsOneOrNull()
        if (localMaterial == null || material.modifiedAt >= localMaterial.modifiedAt) upsertMaterialInDatabase(material = material)
    }

    /**
     * Synchronizes a batch of remote materials with the local database.
     * This function performs an optimized, conflict-aware synchronization.
     * It first queries the local database in chunks to find existing materials.
     * It then compares the `modifiedAt` timestamps in memory, keeping any remote material that is not older than its local counterpart.
     * Finally, it upserts the outdated materials.
     *
     * @param materials The list of remote [Material] objects to synchronize locally.
     */
    private suspend fun syncMaterialsInDatabase(materials: List<Material>) {
        val localMaterialsMap = getMaterialsByUuidsFromDatabase(uuids = materials.map { it.uuid }).associateBy { it.uuid }
        materials.forEach { material ->
            val localMaterial = localMaterialsMap[material.uuid]
            if (localMaterial == null || material.modifiedAt >= localMaterial.modifiedAt) upsertMaterialInDatabase(material = material)
        }
    }

    /**
     * Retrieves materials matching the given UUIDs from the local database, in chunks.
     * SQLite limits the number of bound parameters allowed in a single statement, so large lists are split into batches before querying.
     *
     * @param uuids The material UUIDs to look up, scoped to the currently active user.
     * @return The matching [MaterialSchema] entities, merged across all chunks.
     */
    private suspend fun getMaterialsByUuidsFromDatabase(uuids: List<Uuid>): List<MaterialSchema> = buildList {
        uuids.chunked(size = ClientConfigs.configs.batchSize).forEach { uuidChunk ->
            addAll(elements = materialDao.getMaterialsByUuids(userUuid = user.uuid, materialUuids = uuidChunk).awaitAsList())
        }
    }

    /**
     * Inserts or updates a material and its user association in the local database.
     *
     * @param material The [Material] domain model containing the details to save.
     * @return `true` if the material and its join record were successfully upserted; `false` if no rows were affected.
     */
    private suspend fun upsertMaterialInDatabase(material: Material): Boolean =
        (materialDao.upsertMaterial(Material = material.toMaterialSchema())
                + materialUserJoinDao.upsertMaterialUserJoin(MaterialUserJoin = MaterialUserJoinSchema(materialUuid = material.uuid, userUuid = user.uuid))) > 0

    companion object {
        private const val TAG = "MaterialGateway"
    }
}
