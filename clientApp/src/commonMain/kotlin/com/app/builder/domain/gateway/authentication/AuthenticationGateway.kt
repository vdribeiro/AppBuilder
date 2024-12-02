package com.app.builder.domain.gateway.authentication

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.asFlow
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.HttpResult
import com.app.builder.data.http.URL
import com.app.builder.data.http.post
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Authentication
import com.app.builder.domain.BearerToken
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.User
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.UserSession
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.domain.scheduler.JobResult.NoOp.toJobResult
import com.app.builder.domain.scheduler.Scheduler
import com.app.builder.domain.scheduler.toHeaderMap
import database.AppDatabase

/**
 * Gateway implementation for Authentication Use Cases and Repository.
 *
 * @property user The current user.
 * @property database The SQLite database instance for user and session data.
 * @property httpClient The HTTP client used for network operations.
 * @property scheduler The job scheduler for background tasks.
 */
class AuthenticationGateway(
    private val user: User,
    private val database: AppDatabase,
    private val httpClient: HttpClient,
    private val scheduler: Scheduler
): AuthenticationUseCases, AuthenticationRepository {

    /** Query interface for the users table. */
    private val userDao = database.userQueries
    /** Query interface for the sessions table. */
    private val sessionDao = database.sessionQueries

    override fun observeCurrentUser(): Flow<User?> =
        flow { emitAll(flow = AppFile.Preferences.cache()) }
            .map { cache -> cache.orEmpty()[AppFile.Preferences.Key.CURRENT_USER] }
            .distinctUntilChanged()
            .map { it?.toUuid() }
            .flatMapLatest { userUuid ->
                when (userUuid) {
                    null -> flowOf(value = null)
                    else -> userDao.getUser(userUuid = userUuid)
                        .asFlow { it.toUser() }
                        .map { it.firstOrNull() }
                }
            }
            .distinctUntilChanged()
            .flowOn(context = Dispatcher.IO)
            .catch {
                Telemetry.error(tag = TAG, message = "Unable to observe current user", throwable = it)
                emit(value = null)
            }

    override suspend fun register(registrationForm: RegistrationForm): User? = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.SESSION, permission = Permission.WRITE)) {
            val throwable = Throwable(message = "User does not have ${Permission.WRITE} permission for ${EntityType.SESSION}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext null
        }

        val result = when (val result = httpClient.post<RegistrationForm, Authentication>(
            request = HttpRequest(url = URL.Register),
            body = registrationForm
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to register", throwable = result.error)
                return@withContext null
            }

            is HttpResult.Success -> result.data
        }
        Telemetry.info(tag = TAG, message = "Successfully registered user")
        val userSession = getUserSession(credentials = registrationForm.credentials, result = result) ?: run {
            Telemetry.error(tag = TAG, message = "Unable to get user session")
            return@withContext null
        }
        database.safeTransaction {
            saveUserAndSession(userSession = userSession)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to save user and session", throwable = it)
        }.getOrNull()
    }

    override suspend fun login(credentials: UserCredentials): User? = withContext(context = Dispatcher.IO) {
        val result = when (val result = httpClient.post<UserCredentials, Authentication>(
            request = HttpRequest(url = URL.Login),
            body = credentials
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to login", throwable = result.error)
                null
            }

            is HttpResult.Success -> result.data
        }
        if (result == null) {
            Telemetry.info(tag = TAG, message = "Trying offline login")
            return@withContext loginOffline(credentials = credentials)
        }

        Telemetry.info(tag = TAG, message = "Successfully logged in")
        val userSession = getUserSession(credentials = credentials, result = result) ?: run {
            Telemetry.error(tag = TAG, message = "Unable to get user session")
            return@withContext null
        }
        database.safeTransaction {
            saveUserAndSession(userSession = userSession)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to save user and session", throwable = it)
        }.getOrNull().also { swapUser(userUuid = result.user.uuid.toString()) }
    }

    override suspend fun login(userUuid: Uuid): User? = withContext(context = Dispatcher.IO) {
        // TODO - Not yet implemented
        return@withContext null
    }

    override suspend fun logout(userUuid: Uuid) = withContext(context = Dispatcher.IO) {
        val job = Job(
            userUuid = userUuid,
            entityType = EntityType.SESSION,
            type = Job.Type.DELETE,
            conflictPolicy = Job.ConflictPolicy.REPLACE,
            payload = swapUser(userUuid = null) ?: return@withContext
        )
        scheduler.queue(job = job)
    }

    override suspend fun executeLogin(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        val credentials = job.payload?.let { decode<UserCredentials>(value = it) }
        if (credentials == null) {
            val throwable = Throwable(message = "Unable to decode credentials")
            Telemetry.error(tag = TAG, message = "Invalid credentials payload", throwable = throwable)
            return@withContext throwable.toJobResult()
        }
        when (val result = httpClient.post<UserCredentials, Authentication>(
            request = HttpRequest(
                url = URL.Login,
                headerMap = job.toHeaderMap(),
            ),
            body = credentials
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to post login", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                val userSession = getUserSession(credentials = credentials, result = result.data) ?: run {
                    val throwable = Throwable(message = "Error getting user session")
                    Telemetry.error(tag = TAG, message = "Unable to get user session", throwable = throwable)
                    return@withContext throwable.toJobResult()
                }
                database.safeTransaction {
                    saveUserAndSession(userSession = userSession)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to save user and session", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successful login")
                JobResult.Success
            }
        }
    }

    override suspend fun executeLogout(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        val userUuid = job.payload?.toUuid() ?: run {
            val throwable = Throwable(message = "Invalid payload")
            Telemetry.error(tag = TAG, message = "Unable to execute logout", throwable = throwable)
            return@withContext throwable.toJobResult()
        }
        val bearerToken = database.safeTransaction {
            getSessionBearerToken(userUuid = userUuid)
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get session", throwable = it)
            return@withContext it.toJobResult()
        } ?: run {
            val throwable = Throwable(message = "No token found")
            Telemetry.error(tag = TAG, message = "Unable to execute logout", throwable = throwable)
            return@withContext throwable.toJobResult()
        }
        val decryptedBearer = bearerToken.decrypt() ?: run {
            val throwable = Throwable(message = "Decryption failed")
            Telemetry.error(tag = TAG, message = "Unable to decrypt tokens", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        when (val result = httpClient.post<String, String>(
            request = HttpRequest(
                url = URL.Logout,
                headerMap = job.toHeaderMap(),
            ),
            body = decryptedBearer.refreshToken
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to post logout", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    deleteSession(userUuid = userUuid)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to delete session", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successful logout")
                JobResult.Success
            }
        }
    }

    override suspend fun getBearerToken(userUuid: Uuid): BearerToken? = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            getSessionBearerToken(userUuid = userUuid)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get token", throwable = it)
        }.getOrNull()?.decrypt()
    }

    override suspend fun refreshBearerToken(userUuid: Uuid, refreshToken: String): BearerToken? = withContext(context = Dispatcher.IO) {
        val bearer = when (val result = httpClient.post<String, BearerToken>(
            request = HttpRequest(url = URL.RefreshTokens),
            body = refreshToken
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to get new tokens", throwable = result.error)
                return@withContext null
            }

            is HttpResult.Success -> result.data
        }
        Telemetry.info(tag = TAG, message = "Successfully fetched new tokens")

        val encryptedBearer = bearer.encrypt() ?: run {
            Telemetry.error(tag = TAG, message = "Unable to encrypt tokens")
            return@withContext null
        }

        val result = database.safeTransaction {
            updateSessionTokens(
                bearerToken = encryptedBearer,
                userUuid = userUuid,
            )
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to update session tokens", throwable = it)
        }.getOrDefault(defaultValue = false)
        if (!result) return@withContext null

        Telemetry.info(tag = TAG, message = "Updated session for user $userUuid")
        bearer
    }

    /**
     * Enqueues a login on the [Scheduler] and performs a local identity verification lookup using a deterministic credential combination.
     * Hashes the incoming username and password arguments to query matching local persistent session tokens.
     * This isolates authentication validation locally, allowing cached user sessions to re-verify during offline connectivity gaps.
     *
     * @param credentials The user's login credentials to hash and verify against the local database.
     * @return The cached [User] if a match is found, or null otherwise.
     */
    private suspend fun loginOffline(credentials: UserCredentials): User? {
        val job = Job(
            userUuid = Uuid.NIL,
            entityType = EntityType.SESSION,
            type = Job.Type.POST,
            conflictPolicy = Job.ConflictPolicy.REPLACE,
            payload = encode(value = credentials)
        )
        scheduler.queue(job = job)
        val hash = credentials.hash() ?: run {
            Telemetry.error(tag = TAG, message = "Unable to hash credentials")
            return null
        }
        return getUserBySessionHash(hash = hash)?.also { user -> swapUser(userUuid = user.uuid.toString()) }
    }

    /**
     * Get a [UserSession], performing cryptographic transformations:
     * - Generates a non-reversible cryptographic hash combining user criteria inputs.
     * - Encrypts sensitive access and refresh token blocks.
     *
     * @param credentials The user's login credentials, used to generate a local session hash.
     * @param result The successful authentication result containing user details and tokens.
     * @return The [UserSession] instance if successful, or null if encryption fails.
     */
    private suspend fun getUserSession(credentials: UserCredentials, result: Authentication): UserSession? {
        val hash = credentials.hash() ?: run {
            Telemetry.error(tag = TAG, message = "Unable to hash credentials")
            return null
        }
        val encryptedBearer = result.bearer.encrypt() ?: run {
            Telemetry.error(tag = TAG, message = "Unable to encrypt tokens")
            return null
        }
        return UserSession(
            user = result.user,
            hash = hash,
            accessToken = encryptedBearer.accessToken,
            refreshToken = encryptedBearer.refreshToken
        )
    }

    /**
     * Persists an authenticated user session.
     *
     * @param userSession The user and session to persist.
     * @return The [User] instance if successful, or null if encryption or database operations fail.
     */
    private suspend fun saveUserAndSession(userSession: UserSession): User? {
        if (!upsertUserAndSession(userSession = userSession)) return null
        Telemetry.info(tag = TAG, message = "Upserted user and session for user ${userSession.user.uuid}")
        return userSession.user
    }

    /**
     * Retrieves a user's session token from the local database.
     *
     * @param userUuid The unique identifier of the user whose session token is to be retrieved.
     * @return The [BearerToken] instance if a match is found, or null if no match is found.
     */
    private suspend fun getSessionBearerToken(userUuid: Uuid): BearerToken? =
        sessionDao.getSession(userUuid = userUuid).awaitAsOneOrNull()?.toBearerToken()

    /**
     * Retrieves a user from the local database by matching a generated session hash.
     * This function calculates a hash from the provided username and password combination, then queries the local database to find a user matching that hash.
     *
     * @param hash The username and password hash.
     * @return The mapped [User] domain object if a matching hash is found in the database; `null` if no match is found.
     */
    private suspend fun getUserBySessionHash(hash: String): User? =
        userDao.getUserBySessionHash(hash = hash).awaitAsOneOrNull()?.toUser()

    /**
     * Deletes a user session from the local database.
     *
     * @param userUuid The unique identifier of the user whose session should be deleted.
     * @return `true` if the session was successfully removed from the database; `false` if no rows were affected.
     */
    private suspend fun deleteSession(userUuid: Uuid): Boolean =
        sessionDao.deleteSession(userUuid = userUuid) > 0

    /**
     * Upserts a user and a session in the local database.
     *
     * @param userSession The [UserSession] domain model containing the data to save.
     * @return `true` if the session was successfully upserted into the database; `false` if no rows were affected.
     */
    private suspend fun upsertUserAndSession(userSession: UserSession): Boolean =
        userDao.upsertUser(User = userSession.user.toUserSchema()) + sessionDao.upsertSession(Session = userSession.toSessionSchema()) > 0

    /**
     * Updates the authentication session tokens for a specific user in the local database.
     *
     * @param bearerToken The new tokens to store.
     * @param userUuid The unique identifier of the user whose session is being updated.
     * @return `true` if the tokens were successfully updated; `false` if no rows were affected.
     */
    private suspend fun updateSessionTokens(
        bearerToken: BearerToken,
        userUuid: Uuid
    ): Boolean = sessionDao.updateSessionTokens(
        accessToken = bearerToken.accessToken,
        refreshToken = bearerToken.refreshToken,
        userUuid = userUuid,
    ) > 0

    /**
     * Sets the authenticated user in storage.
     *
     * @param userUuid The unique identifier of the user to set as authenticated, or `null` to clear the current user.
     * @return The previous authenticated user UUID, or `null` if no user was previously authenticated.
     */
    private suspend fun swapUser(userUuid: String?): String? {
        val previousUser = AppFile.Preferences.cache().value.orEmpty()[AppFile.Preferences.Key.CURRENT_USER]
        when (userUuid) {
            null -> {
                AppFile.Preferences.save { it.orEmpty().minus(key = AppFile.Preferences.Key.CURRENT_USER) }
                Telemetry.info(tag = TAG, message = "Cleared authenticated user")
            }

            else -> {
                AppFile.Preferences.save { it.orEmpty().plus(pair = (AppFile.Preferences.Key.CURRENT_USER to userUuid)) }
                Telemetry.info(tag = TAG, message = "Set authenticated user: $userUuid")
            }
        }
        return previousUser
    }

    companion object {
        private const val TAG = "AuthenticationGateway"
    }
}