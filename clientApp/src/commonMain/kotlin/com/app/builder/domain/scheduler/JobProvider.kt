package com.app.builder.domain.scheduler

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.builder.Dependency
import com.app.builder.Dependency.getUserDependency
import com.app.builder.domain.EntityType
import com.app.builder.domain.gateway.Repositories
import com.app.builder.domain.gateway.authentication.toUser

/**
 * Factory responsible for mapping background jobs to their corresponding executable domain logic.
 *
 * @property appGraph dependency index.
 */
class JobProvider(
    private val appGraph: Dependency.AppGraph
): JobFactory {

    /**
     * Resolves the provided [job] into an executable lambda block based on its designated type.
     *
     * @param job The scheduled job configuration detailing the operation to perform.
     * @return A function that, when invoked, executes the mapped use case and returns the updated [Job] state.
     */
    override fun resolve(job: Job): suspend () -> JobResult = {
        when (job.type) {
            Job.Type.GET if job.entityUuid == null -> handleSync(job = job)
            Job.Type.GET -> handleGet(job = job)
            Job.Type.POST -> handlePost(job = job)
            Job.Type.DELETE -> handleDelete(job = job)
        }
    }

    /**
     * Get the repositories of a user.
     *
     * @param job to use.
     * @return A [Repositories] instance, or null if the user is not recognized.
     */
    private suspend fun Dependency.AppGraph.getRepositories(job: Job): Repositories? {
        val userUuid = job.userUuid
        if (userUuid == Dependency.guestUser.uuid) return userGraph.repositories
        val user = database.userQueries.getUser(userUuid = userUuid).awaitAsOneOrNull()?.toUser() ?: return null
        return getUserDependency(user = user).repositories
    }

    /**
     * Dispatches a full-sync [job] to the use case responsible for syncing all entities of its [Job.entityType].
     *
     * @param job The scheduled job configuration detailing the entity type to sync.
     * @return The [JobResult] of the sync operation, or [JobResult.NoOp] if the entity type has no sync handler.
     */
    private suspend fun handleSync(job: Job): JobResult = appGraph.getRepositories(job = job)?.run {
        when (job.entityType) {
            EntityType.CLIENT_FLAG -> configRepository.executeSyncFeatureFlags(job = job)
            EntityType.CLIENT_CONFIG -> configRepository.executeSyncConfigs(job = job)
            EntityType.REGISTRY -> registryRepository.executeSyncRegistries(job = job)
            EntityType.TRANSLATION -> translationRepository.executeSyncTranslations(job = job)
            EntityType.DEVICE_LOCATION -> deviceLocationRepository.executeSyncDeviceLocations(job = job)
            EntityType.USER -> userRepository.executeSyncUsers(job = job)
            EntityType.TASK -> taskRepository.executeSyncTasks(job = job)
            else -> null
        }
    } ?: JobResult.NoOp

    /**
     * Dispatches a fetch [job] for a single entity to the use case responsible for its [Job.entityType].
     *
     * @param job The scheduled job configuration detailing the entity to fetch.
     * @return The [JobResult] of the fetch operation, or [JobResult.NoOp] if the entity type has no get handler.
     */
    private suspend fun handleGet(job: Job): JobResult = appGraph.getRepositories(job = job)?.run {
        when (job.entityType) {
            EntityType.USER -> userRepository.executeSyncUser(job = job)
            EntityType.TASK -> taskRepository.executeSyncTask(job = job)
            else -> null
        }
    } ?: JobResult.NoOp

    /**
     * Dispatches a creation or update [job] to the use case responsible for its [Job.entityType].
     *
     * @param job The scheduled job configuration detailing the entity to create or update.
     * @return The [JobResult] of the post operation, or [JobResult.NoOp] if the entity type has no post handler.
     */
    private suspend fun handlePost(job: Job): JobResult = appGraph.getRepositories(job = job)?.run {
        when (job.entityType) {
            EntityType.SESSION -> authenticationRepository.executeLogin(job = job)
            EntityType.USER -> userRepository.executeUpsertUser(job = job)
            EntityType.TASK -> taskRepository.executeUpsertTask(job = job)
            else -> null
        }
    } ?: JobResult.NoOp

    /**
     * Dispatches a deletion [job] to the use case responsible for its [Job.entityType].
     *
     * @param job The scheduled job configuration detailing the entity to delete.
     * @return The [JobResult] of the delete operation, or [JobResult.NoOp] if the entity type has no delete handler.
     */
    private suspend fun handleDelete(job: Job): JobResult = appGraph.getRepositories(job = job)?.run {
        when (job.entityType) {
            EntityType.SESSION -> authenticationRepository.executeLogout(job = job)
            else -> null
        }
    } ?: JobResult.NoOp
}
