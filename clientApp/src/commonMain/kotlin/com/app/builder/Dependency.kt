package com.app.builder

import kotlin.uuid.Uuid
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import app.cash.sqldelight.db.SqlDriver
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.locale.now
import com.app.builder.data.database.DatabaseFactory
import com.app.builder.data.http.Configurations
import com.app.builder.data.http.HttpClientFactory
import com.app.builder.domain.User
import com.app.builder.domain.clock.ClockManager
import com.app.builder.domain.clock.ClockService
import com.app.builder.domain.gateway.Gateways
import com.app.builder.domain.gateway.Repositories
import com.app.builder.domain.gateway.UseCases
import com.app.builder.domain.gateway.authentication.toBearerTokens
import com.app.builder.domain.nfc.TagManager
import com.app.builder.domain.nfc.TagService
import com.app.builder.domain.push.BroadcastManager
import com.app.builder.domain.push.BroadcastService
import com.app.builder.domain.push.PayloadManager
import com.app.builder.domain.push.PayloadService
import com.app.builder.domain.push.PushManager
import com.app.builder.domain.push.PushService
import com.app.builder.domain.scheduler.JobFactory
import com.app.builder.domain.scheduler.JobProvider
import com.app.builder.domain.scheduler.JobScheduler
import com.app.builder.domain.scheduler.JobSchedulerStore
import com.app.builder.domain.scheduler.Scheduler
import com.app.builder.domain.translation.TranslationManager
import com.app.builder.domain.translation.TranslationService
import database.AppDatabase

/** Collection of application wide dependency index and user dependency index. */
object Dependency {

    /**
     * Application dependency graph.
     *
     * @property sqlDriver The platform-specific SQL driver.
     * @property httpClientEngine The platform-specific HTTP client engine.
     */
    class AppGraph(
        val sqlDriver: SqlDriver,
        val httpClientEngine: HttpClientEngine,
    ) {
        /** The application database. */
        val database: AppDatabase = DatabaseFactory(
            driver = sqlDriver
        ).database

        /** A plain HTTP client. */
        val httpClient: HttpClient = HttpClientFactory(
            engine = httpClientEngine
        ).httpClient

        /** The clock service. */
        val clockService: ClockService = ClockManager(
            httpClient = httpClient
        )

        /** The factory to map background jobs to executables. */
        val jobFactory: JobFactory = JobProvider(
            appGraph = this
        )

        /** Scheduler to queue jobs. */
        val scheduler: Scheduler = JobScheduler(
            jobStore = JobSchedulerStore(database = database),
            jobFactory = jobFactory,
            clockService = clockService
        )

        /** Guest user graph. */
        val userGraph: UserGraph = UserGraph(
            user = guestUser,
            dependency = this
        )

        /** Payload service. */
        val payloadService: PayloadService = PayloadManager(
            appGraph = this
        )

        /** Tag service. */
        val tagService: TagService = TagManager(
            appGraph = this
        )

        /** Broadcast service. */
        val broadcastService: BroadcastService = BroadcastManager(
            httpClient = httpClient,
        )

        /** Translation service. */
        val translationService: TranslationService = TranslationManager(
            translationStore = userGraph.repositories.translationRepository
        )
    }

    /**
     * User dependency graph.
     *
     * @property user for the graph.
     * @property dependency The [AppGraph] graph.
     */
    class UserGraph(
        val user: User,
        val dependency: AppGraph,
    ) {
        /** The HTTP client configured with the authenticated user's bearer token handling. */
        val httpClient: HttpClient = HttpClientFactory(
            engine = dependency.httpClientEngine,
            configurations = Configurations(
                loadTokens = { repositories.authenticationRepository.getBearerToken(userUuid = user.uuid)?.toBearerTokens() },
                refreshTokens = { oldTokens ->
                    val refreshToken = oldTokens?.refreshToken ?: return@Configurations null
                    repositories.authenticationRepository.refreshBearerToken(userUuid = user.uuid, refreshToken = refreshToken)?.toBearerTokens()
                }
            )
        ).httpClient

        /** Gateways. */
        private val gateways = Gateways(
            user = user,
            database = dependency.database,
            httpClient = httpClient,
            scheduler = dependency.scheduler
        )
        /** Use cases. */
        val useCases: UseCases = gateways
        /** Repositories. */
        val repositories: Repositories = gateways

        /** Push service. */
        val pushService: PushService = PushManager(
            user = user,
            httpClient = httpClient,
        )
    }

    /** The default guest user. */
    val guestUser: User by lazy {
        User(
            uuid = Uuid.NIL,
            modifiedAt = now(),
            deletedAt = null,
            permissions = emptyMap(),
            name = "Guest",
            avatar = null
        )
    }

    /** Central cache preserving singleton references to initialized [UserGraph] nodes. */
    private val instanceMap = mutableMapOf<Uuid, UserGraph>()
    /** Mutex guarding concurrent factory allocation requests to ensure instance uniqueness. */
    private val instanceMutex = Mutex()

    /**
     * Retrieves or instantiates a unique, thread-safe [UserGraph] instance tied to the user.
     *
     * @param user The user to tie the dependencies with.
     * @return A [UserGraph] instance.
     */
    suspend fun AppGraph.getUserDependency(user: User): UserGraph = instanceMutex.withLock {
        instanceMap[user.uuid] ?: UserGraph(user = user, dependency = this).also { instanceMap[user.uuid] = it }
    }

    /**
     * Returns all cached [UserGraph]s in a thread-safe manner.
     *
     * @return all [UserGraph]s.
     */
    suspend fun getAll(): List<UserGraph> = instanceMutex.withLock {
        instanceMap.values.toList()
    }

    /** Removes all cached [UserGraph]s, so the next [getUserDependency] call rebuilds them. */
    suspend fun clearAll() = instanceMutex.withLock {
        instanceMap.clear()
    }
}