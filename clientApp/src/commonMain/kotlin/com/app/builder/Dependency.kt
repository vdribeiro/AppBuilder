package com.app.builder

import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.db.SqlDriver
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.locale.SelectedLanguage
import com.app.builder.core.locale.now
import com.app.builder.core.nfc.TagProvider
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.DatabaseFactory
import com.app.builder.data.database.reset
import com.app.builder.data.http.Configurations
import com.app.builder.data.http.HttpClientFactory
import com.app.builder.data.storage.clearCache
import com.app.builder.data.storage.files
import com.app.builder.data.theme.ThemeCache
import com.app.builder.data.translation.TranslationCache
import com.app.builder.domain.Account
import com.app.builder.domain.clock.ClockManager
import com.app.builder.domain.clock.ClockService
import com.app.builder.domain.deeplink.DeepLink
import com.app.builder.domain.gateway.Gateways
import com.app.builder.domain.gateway.Repositories
import com.app.builder.domain.gateway.UseCases
import com.app.builder.domain.gateway.authentication.toAccount
import com.app.builder.domain.gateway.authentication.toBearerTokens
import com.app.builder.domain.nfc.TagManager
import com.app.builder.domain.nfc.TagService
import com.app.builder.domain.push.BroadcastManager
import com.app.builder.domain.push.BroadcastService
import com.app.builder.domain.push.PayloadManager
import com.app.builder.domain.push.PayloadService
import com.app.builder.domain.push.PushManager
import com.app.builder.domain.push.PushProvider
import com.app.builder.domain.push.PushService
import com.app.builder.domain.scheduler.JobFactory
import com.app.builder.domain.scheduler.JobProvider
import com.app.builder.domain.scheduler.JobScheduler
import com.app.builder.domain.scheduler.JobSchedulerStore
import com.app.builder.domain.scheduler.Scheduler
import com.app.builder.domain.translation.TranslationManager
import com.app.builder.domain.translation.TranslationService
import com.app.builder.ui.core.image.clearImageCache
import database.AppDatabase

/** Collection of application wide dependency index and user dependency index. */
object Dependency {

    private const val TAG = "Dependency"

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
            account = guestAccount,
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

        /** Drops and recreates the tables of the [sqlDriver] database. */
        suspend fun reset() = sqlDriver.reset()

        /** Loads the feature flags and configs, then starts the clock, scheduler, translations, tags, payloads and broadcast services. */
        suspend fun start() {
            loadRemoteConfigs()
            Telemetry.info(tag = TAG, message = "Starting clock service")
            clockService.start()
            Telemetry.info(tag = TAG, message = "Starting scheduler service")
            scheduler.start()
            Telemetry.info(tag = TAG, message = "Starting translations service")
            translationService.start()
            Telemetry.info(tag = TAG, message = "Starting tags service")
            tagService.start()
            Telemetry.info(tag = TAG, message = "Starting payloads service")
            payloadService.start()
            Telemetry.info(tag = TAG, message = "Starting broadcast service")
            broadcastService.start()
        }

        /** Stops the broadcast, payloads, tags, translations, scheduler and clock services. */
        suspend fun stop() {
            Telemetry.info(tag = TAG, message = "Stopping broadcast service")
            broadcastService.stop()
            Telemetry.info(tag = TAG, message = "Stopping payloads service")
            payloadService.stop()
            Telemetry.info(tag = TAG, message = "Stopping tags service")
            tagService.stop()
            Telemetry.info(tag = TAG, message = "Stopping translations service")
            translationService.stop()
            Telemetry.info(tag = TAG, message = "Stopping scheduler service")
            scheduler.stop()
            Telemetry.info(tag = TAG, message = "Stopping clock service")
            clockService.stop()
        }

        /** Load feature flags and configs. */
        private suspend fun loadRemoteConfigs() {
            Telemetry.info(tag = TAG, message = "Loading remote configs")
            userGraph.repositories.configRepository.load()
            userGraph.repositories.configRepository.syncFeatureFlags()
            userGraph.repositories.configRepository.syncConfigs()
            Telemetry.info(tag = TAG, message = "Feature Flags: ${ClientFlags.value}")
            Telemetry.info(tag = TAG, message = "Configs: ${ClientConfigs.value}")
        }
    }

    /**
     * User dependency graph.
     *
     * @property account The account of the user the graph is for.
     * @property dependency The [AppGraph] graph.
     */
    class UserGraph(
        val account: Account,
        val dependency: AppGraph,
    ) {
        /** HTTP client configured with bearer token handling. */
        val httpClient: HttpClient = HttpClientFactory(
            engine = dependency.httpClientEngine,
            configurations = Configurations(
                loadTokens = { repositories.authenticationRepository.getBearerToken(userUuid = account.userUuid)?.toBearerTokens() },
                refreshTokens = { oldTokens ->
                    val refreshToken = oldTokens?.refreshToken ?: return@Configurations null
                    repositories.authenticationRepository.refreshBearerToken(userUuid = account.userUuid, refreshToken = refreshToken)?.toBearerTokens()
                }
            )
        ).httpClient

        /** Gateways. */
        private val gateways = Gateways(
            account = account,
            database = dependency.database,
            plainHttpClient = dependency.httpClient,
            httpClient = httpClient,
            scheduler = dependency.scheduler,
            clockService = dependency.clockService
        )
        /** Use cases. */
        val useCases: UseCases = gateways
        /** Repositories. */
        val repositories: Repositories = gateways

        /** Push service. */
        val pushService: PushService = PushManager(
            userUuid = account.userUuid,
            httpClient = httpClient,
            sessionActive = repositories.authenticationRepository.observeSessionActive(userUuid = account.userUuid),
        )

        /** The job requesting syncs. */
        private var syncJob: Job? = null

        /**
         * Launches the job requesting syncs as the client feature flags change, and starts the push service.
         *
         * @param scope The scope to collect on.
         */
        fun start(scope: CoroutineScope) {
            syncJob = scope.launch {
                var previous: ClientFlags? = null
                ClientFlags.flow.collect {
                    sync(flags = it, previous = previous)
                    previous = it
                }
            }
            Telemetry.info(tag = TAG, message = "Starting push service for user ${account.userUuid}")
            pushService.start()
        }

        /** Cancels the job requesting syncs, and stops the push service. */
        suspend fun stop() {
            syncJob?.cancelAndJoin()
            syncJob = null
            Telemetry.info(tag = TAG, message = "Stopping push service for user ${account.userUuid}")
            pushService.stop()
        }

        /**
         * Requests the sync of each entity whose client feature flag turned on.
         *
         * @param flags The current client feature flags.
         * @param previous The client feature flags of the last request, or `null` on the first one, which also syncs the users.
         */
        private suspend fun sync(flags: ClientFlags, previous: ClientFlags?) {
            if (previous == null) {
                Telemetry.info(tag = TAG, message = "Requesting sync for user ${account.userUuid}")
                repositories.userRepository.syncUsers()
            }
            if (flags.registries && previous?.registries != true) repositories.registryRepository.syncRegistries()
            if (flags.tasks && previous?.tasks != true) repositories.taskRepository.syncTasks()
            if (flags.buildings && previous?.buildings != true) repositories.buildingRepository.syncBuildings()
            if (flags.equipments && previous?.equipments != true) repositories.equipmentRepository.syncEquipments()
            if (flags.buildingEquipments && previous?.buildingEquipments != true) repositories.buildingEquipmentRepository.syncBuildingEquipments()
            if (flags.materials && previous?.materials != true) repositories.materialRepository.syncMaterials()
            if (flags.buildingMaterials && previous?.buildingMaterials != true) repositories.buildingMaterialRepository.syncBuildingMaterials()
            if (flags.buildingUsers && previous?.buildingUsers != true) repositories.buildingUserRepository.syncBuildingUsers()
            if (flags.equipmentUsers && previous?.equipmentUsers != true) repositories.equipmentUserRepository.syncEquipmentUsers()
        }
    }

    /** The account of the default guest user, without any permission. */
    val guestAccount: Account by lazy {
        Account(
            userUuid = Uuid.NIL,
            modifiedAt = now(),
            deletedAt = null,
            username = "guest",
            permissions = emptyMap()
        )
    }

    /** Central cache preserving singleton references to initialized [UserGraph] nodes. */
    private val instanceMap = mutableMapOf<Uuid, UserGraph>()
    /** Mutex guarding concurrent factory allocation requests to ensure instance uniqueness. */
    private val instanceMutex = Mutex()

    /**
     * Retrieves or instantiates a unique, thread-safe [UserGraph] instance tied to the account's user.
     * The cached instance is rebuilt when the account's permissions or disabled state differ from the ones it was built with.
     *
     * @param account The account to tie the dependencies with.
     * @return A [UserGraph] instance.
     */
    suspend fun AppGraph.getUserDependency(account: Account): UserGraph = instanceMutex.withLock {
        instanceMap[account.userUuid]
            ?.takeIf { it.account.permissions == account.permissions && it.account.deletedAt == account.deletedAt }
            ?: UserGraph(account = account, dependency = this).also { instanceMap[account.userUuid] = it }
    }

    /**
     * Resolves the [UserGraph] of the user with [userUuid].
     *
     * @param userUuid The uuid of the user, or `null` for the guest.
     * @return The guest [AppGraph.userGraph] for a `null` or guest uuid, the user's [UserGraph] if its account is stored, or `null` otherwise.
     */
    suspend fun AppGraph.getUserGraph(userUuid: Uuid?): UserGraph? {
        if (userUuid == null || userUuid == guestAccount.userUuid) return userGraph
        val account = database.accountQueries.getAccount(userUuid = userUuid).awaitAsOneOrNull()?.toAccount() ?: return null
        return getUserDependency(account = account)
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

    /** Stops every cached [UserGraph] and clears them, then resets the files, flags, configs, providers, caches and selected language shared by every [AppGraph]. */
    suspend fun resetGlobals() {
        getAll().forEach { it.stop() }
        clearAll()
        files.forEach { it.reset() }
        ClientFlags.reset()
        ClientConfigs.reset()
        PushProvider.reset()
        TagProvider.reset()
        DeepLink.reset()
        clearImageCache()
        clearCache()
        TranslationCache.reset()
        ThemeCache.reset()
        SelectedLanguage.reset()
    }
}