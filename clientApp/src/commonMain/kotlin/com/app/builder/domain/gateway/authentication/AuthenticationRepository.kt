package com.app.builder.domain.gateway.authentication

import kotlin.uuid.Uuid
import com.app.builder.domain.BearerToken
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult

/** Defines the contract for session login and logout. */
interface AuthenticationRepository {

    /**
     * Attempts to log in.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeLogin(job: Job): JobResult

    /**
     * Logs out.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeLogout(job: Job): JobResult

    /**
     * Retrieves the cryptographic bearer token pair for a specific authenticated user context.
     *
     * @param userUuid The unique tracking identifier of the target user session.
     * @return [BearerToken] structure carrying the active access and refresh tokens, or null on failure or if no valid session record exists.
     */
    suspend fun getBearerToken(userUuid: Uuid): BearerToken?

    /**
     * Reconciles and exchanges a long-lived refresh token for a brand new, valid authentication token pair.
     *
     * @param userUuid The unique tracking user identifier string used to map and bind the renewed token block to the proper database session record.
     * @param refreshToken The raw, un-expired long-lived token string submitted as proof of authorization to obtain the new access pair.
     * @return A newly minted [BearerToken] structure carrying active access and refresh strings, or null if the server rejects the exchange request or the underlying refresh token has been permanently invalidated.
     */
    suspend fun refreshBearerToken(userUuid: Uuid, refreshToken: String): BearerToken?
}