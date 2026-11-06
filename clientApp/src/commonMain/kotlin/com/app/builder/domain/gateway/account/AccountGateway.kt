package com.app.builder.domain.gateway.account

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.asFlow
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.http.Header
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.HttpResult
import com.app.builder.data.http.URL
import com.app.builder.data.http.get
import com.app.builder.data.http.post
import com.app.builder.domain.Account
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.gateway.authentication.toAccount
import com.app.builder.domain.gateway.authentication.toAccountSchema
import com.app.builder.domain.gateway.authentication.toUserSchema
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.domain.scheduler.JobResult.NoOp.toJobResult
import com.app.builder.domain.scheduler.Scheduler
import com.app.builder.domain.scheduler.toHeaderMap
import database.AppDatabase

/**
 * Gateway implementation for Account Use Cases and Repository.
 * An account is fetched in the background when it is observed, so only the accounts that were viewed are stored, while creating and updating one is an online call, since creating one carries a password.
 *
 * @property account The account of the current user.
 * @property database The SQLite database instance for account data.
 * @property httpClient The HTTP client used for network operations.
 * @property scheduler The job scheduler for background tasks.
 */
class AccountGateway(
    private val account: Account,
    private val database: AppDatabase,
    private val httpClient: HttpClient,
    private val scheduler: Scheduler
): AccountUseCases, AccountRepository {

    /** Query interface for the accounts table. */
    private val accountDao = database.accountQueries
    /** Query interface for the users table. */
    private val userDao = database.userQueries

    override fun observeAccount(userUuid: Uuid): Flow<Account?> {
        if (!account.hasPermission(entityType = EntityType.ACCOUNT, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.ACCOUNT}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return emptyFlow()
        }

        return accountDao.getAccount(userUuid = userUuid)
            .asFlow { it.toAccount() }
            .map { it.firstOrNull() }
            .onStart {
                val job = Job(
                    userUuid = account.userUuid,
                    entityUuid = userUuid,
                    entityType = EntityType.ACCOUNT,
                    type = Job.Type.GET,
                )
                scheduler.queue(job = job)
            }
            .flowOn(context = Dispatcher.IO)
            .catch {
                Telemetry.error(tag = TAG, message = "Unable to observe account", throwable = it)
                emit(value = null)
            }
    }

    override suspend fun createAccount(registrationForm: RegistrationForm): Account? = withContext(context = Dispatcher.IO) {
        if (!account.hasPermission(entityType = EntityType.ACCOUNT, permission = Permission.WRITE)) {
            val throwable = Throwable(message = "User does not have ${Permission.WRITE} permission for ${EntityType.ACCOUNT}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext null
        }

        val created = when (val result = httpClient.post<RegistrationForm, Account>(
            request = HttpRequest(url = URL.Register, headerMap = requestHeaderMap()),
            body = registrationForm
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to create account", throwable = result.error)
                return@withContext null
            }

            is HttpResult.Success -> result.data
        }

        database.safeTransaction {
            if (userDao.getUser(userUuid = registrationForm.user.uuid).awaitAsOneOrNull() == null) {
                userDao.upsertUser(User = registrationForm.user.toUserSchema())
            }
            upsertAccountInDatabase(account = created)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to save created account", throwable = it)
        }
        Telemetry.info(tag = TAG, message = "Successfully created account ${created.userUuid}")
        created
    }

    override suspend fun updateAccount(account: Account): Account? = withContext(context = Dispatcher.IO) {
        if (!this@AccountGateway.account.hasPermission(entityType = EntityType.ACCOUNT, permission = Permission.WRITE)) {
            val throwable = Throwable(message = "User does not have ${Permission.WRITE} permission for ${EntityType.ACCOUNT}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext null
        }

        val updated = when (val result = httpClient.post<Account, Account>(
            request = HttpRequest(url = URL.Accounts, headerMap = requestHeaderMap()),
            body = account
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to update account ${account.userUuid}", throwable = result.error)
                return@withContext null
            }

            is HttpResult.Success -> result.data
        }

        database.safeTransaction {
            upsertAccountInDatabase(account = updated)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to save updated account", throwable = it)
        }
        Telemetry.info(tag = TAG, message = "Successfully updated account ${updated.userUuid}")
        updated
    }

    override suspend fun executeSyncAccount(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        if (!account.hasPermission(entityType = EntityType.ACCOUNT, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.ACCOUNT}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        val entityUuid = job.entityUuid ?: return@withContext Throwable(message = "Missing entity uuid").toJobResult()

        when (val result = httpClient.get<Account>(
            request = HttpRequest(
                url = URL.Accounts,
                headerMap = job.toHeaderMap()
            ),
            uuid = entityUuid
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to get the remote account of user $entityUuid", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncAccountInDatabase(account = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync the account of user $entityUuid", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully fetched the account of user $entityUuid")
                JobResult.Success
            }
        }
    }

    /**
     * Builds the request headers of an online call, made at the current time.
     *
     * @return The header map identifying the request and when it was made.
     */
    private fun requestHeaderMap(): Map<Header, String> = mapOf(
        Header.RequestUuid to uuid().toString(),
        Header.RequestUtc to now().toString()
    )

    /**
     * Synchronizes a remote account with the local database, keeping the local account when it is newer.
     *
     * @param account The remote [Account] to synchronize locally.
     */
    private suspend fun syncAccountInDatabase(account: Account) {
        val localAccount = accountDao.getAccount(userUuid = account.userUuid).awaitAsOneOrNull()
        if (localAccount == null || account.modifiedAt >= localAccount.modifiedAt) upsertAccountInDatabase(account = account)
    }

    /**
     * Inserts or updates an account in the local database.
     *
     * @param account The [Account] domain model containing the details to save.
     * @return `true` if the account record was successfully upserted; `false` if no rows were affected.
     */
    private suspend fun upsertAccountInDatabase(account: Account): Boolean =
        accountDao.upsertAccount(Account = account.toAccountSchema()) > 0

    companion object {
        private const val TAG = "AccountGateway"
    }
}
