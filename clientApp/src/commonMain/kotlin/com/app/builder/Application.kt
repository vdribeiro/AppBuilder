package com.app.builder

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.flow.LazyData
import com.app.builder.core.nfc.TagProvider
import com.app.builder.core.platform.appDataPath
import com.app.builder.core.platform.developmentMode
import com.app.builder.core.platform.platform
import com.app.builder.core.telemetry.Console
import com.app.builder.core.telemetry.PlatformLogger
import com.app.builder.core.telemetry.SentryLogger
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.DatabaseFactory.Companion.DATABASE_FILE
import com.app.builder.data.database.NoOpSqlDriver
import com.app.builder.data.database.createSqlDriver
import com.app.builder.data.database.reset
import com.app.builder.data.http.NoOpHttpClientEngine
import com.app.builder.data.http.createHttpEngine
import com.app.builder.data.storage.clearCache
import com.app.builder.data.storage.files
import com.app.builder.data.storage.logFiles
import com.app.builder.data.translation.TranslationCache
import com.app.builder.domain.deeplink.DeepLink
import com.app.builder.domain.push.PushProvider
import com.app.builder.ui.core.image.clearImageCache
import com.app.builder.ui.push.RegisterBroadcastLifecycle
import com.app.builder.ui.push.RegisterPushLifecycle
import database.AppDatabase

/** Singleton entry point that initializes the dependencies graph, telemetry and services. */
object Application {

    private const val TAG = "Application"

    /** Scope for application-wide background tasks. */
    private val scope by lazy { CoroutineScope(context = SupervisorJob()) }

    /** Guards concurrent start()/stop() transitions. */
    private val mutex by lazy { Mutex() }

    /** The currently active job. */
    private var job: Job? = null

    /** Backing state for the [Dependency.AppGraph] graph. */
    private val _dependency: MutableStateFlow<Dependency.AppGraph?> = MutableStateFlow(value = null)
    /** Flow emitting the initialized [Dependency.AppGraph] graph. */
    val dependency: StateFlow<Dependency.AppGraph?> = _dependency.asStateFlow()

    /** Backing state for the [Dependency.UserGraph] graph. */
    private val _userDependency: MutableStateFlow<Dependency.UserGraph?> = MutableStateFlow(value = null)
    /** Flow emitting the initialized [Dependency.UserGraph] graph. */
    val userDependency: StateFlow<Dependency.UserGraph?> = _userDependency.asStateFlow()

    /** Lazily initializes the [App] info. */
    private val app by lazy {
        App.setInfo(
            id = AppInfo.ID,
            name = AppInfo.NAME,
            version = AppInfo.VERSION
        )
    }

    /** Lazily initializes the telemetry engines. */
    private val telemetry by lazy {
        Telemetry.engines.apply {
            clear()
            if (developmentMode) add(element = PlatformLogger)
            if (!developmentMode && SentryConfig.dsn.isNotEmpty()) {
                val sentryLogger = SentryLogger.init(
                    options = SentryLogger.Options(
                        dsn = SentryConfig.dsn.decodeToString(),
                        release = "${AppInfo.NAME.lowercase().replace(regex = "\\s+".toRegex(), replacement = "")}@${AppInfo.VERSION}"
                    )
                )
                add(element = sentryLogger)
            }
            if (ClientFlags.flags.console) add(element = Console)
        }
        Telemetry.info(tag = TAG, message = "Telemetry set")
    }

    /** Dependency app graph. */
    private val appGraph: LazyData<Dependency.AppGraph> = LazyData { createDependency() }

    /** Resets the application. */
    suspend fun reset() = withContext(context = Dispatcher.IO) {
        Telemetry.info(tag = TAG, message = "Resetting application")
        stop()
        appGraph.get().reset()
        start()
    }

    /** Clears the application. */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal suspend fun Dependency.AppGraph.reset() {
        Dependency.getAll().forEach { it.stop() }
        Dependency.clearAll()
        database.reset()
        files.forEach { it.reset() }
        ClientFlags.reset()
        ClientConfigs.reset()
        PushProvider.reset()
        TagProvider.reset()
        DeepLink.reset()
        clearImageCache()
        clearCache()
        TranslationCache.set(translations = emptyMap())
    }

    /** Stops all application services, suspending until they have fully canceled. */
    suspend fun stop() = withContext(context = Dispatcher.IO) {
        mutex.withLock {
            job?.cancelAndJoin()
            job = null
            _userDependency.value?.stop()
            _userDependency.update { null }
            _dependency.value?.stop()
            _dependency.update { null }
        }
    }

    /** Starts all application services. */
    fun start() {
        if (!mutex.tryLock()) return
        try {
            app; telemetry
            startAppJob()
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Suspends until the dependencies are fully initialized, or reaches the timeout.
     *
     * @param timeout the time to wait until the graph is initialized.
     * @return true if the graph was initialized in time, false otherwise.
     */
    suspend fun await(timeout: Duration = 15.seconds): Boolean =
        withTimeoutOrNull(timeout = timeout) {
            val dependency = dependency.filterNotNull().first()
            dependency.userGraph.useCases.authenticationUseCases
                .observeCurrentUser().firstOrNull() ?: return@withTimeoutOrNull true
            userDependency.filterNotNull().first()
        } != null

    /**
     * Start app job.
     * If the job is already running, this is a no-op.
     */
    private fun startAppJob() {
        if (job?.isActive != true) job = scope.launch(context = Dispatcher.IO) {
            _dependency.value?.stop()
            Telemetry.info(tag = TAG, message = "Development mode: $developmentMode")
            Telemetry.info(tag = TAG, message = "Platform: $platform")
            Telemetry.info(tag = TAG, message = "Setting app path: $appDataPath")
            Telemetry.info(tag = TAG, message = logFiles())

            Telemetry.info(tag = TAG, message = "Initializing dependencies")
            val dependency = appGraph.get().apply { start(); observeCurrentUser(scope = this@launch) }
            Telemetry.info(tag = TAG, message = "Setting dependencies")
            _dependency.update { dependency }
        }
    }

    /**
     * Observes the current user, and sets the authenticated dependency accordingly.
     *
     * @param scope The scope to collect on.
     */
    private fun Dependency.AppGraph.observeCurrentUser(scope: CoroutineScope) {
        scope.launch {
            Telemetry.info(tag = TAG, message = "Observing user")
            userGraph.useCases.authenticationUseCases.observeCurrentUser().collect { user ->
                _userDependency.value?.stop()
                when (user) {
                    null -> {
                        Telemetry.info(tag = TAG, message = "Clearing authenticated dependencies")
                        _userDependency.update { null }
                    }

                    else -> {
                        Telemetry.info(tag = TAG, message = "Initializing authenticated dependencies for user ${user.uuid}")
                        val authenticatedDependency = getUserDependency(user = user).apply { start() }
                        Telemetry.info(tag = TAG, message = "Setting authenticated dependencies for user ${user.uuid}")
                        _userDependency.update { authenticatedDependency }
                    }
                }
            }
        }
    }

    /** Stops [Dependency.AppGraph] services. */
    private suspend fun Dependency.AppGraph.stop() {
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

    /** Starts [Dependency.AppGraph] services. */
    private suspend fun Dependency.AppGraph.start() {
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

    /** Load feature flags and configs. */
    private suspend fun Dependency.AppGraph.loadRemoteConfigs() {
        Telemetry.info(tag = TAG, message = "Loading remote configs")
        userGraph.repositories.configRepository.load()
        userGraph.repositories.configRepository.syncFeatureFlags()
        userGraph.repositories.configRepository.syncConfigs()
        Telemetry.info(tag = TAG, message = "Feature Flags: ${ClientFlags.flags}")
        Telemetry.info(tag = TAG, message = "Configs: ${ClientConfigs.configs}")
    }

    /** Stops [Dependency.UserGraph] services. */
    private suspend fun Dependency.UserGraph.stop() {
        Telemetry.info(tag = TAG, message = "Stopping push service for user ${user.uuid}")
        pushService.stop()
    }

    /** Starts [Dependency.UserGraph] services. */
    private suspend fun Dependency.UserGraph.start() {
        sync()
        Telemetry.info(tag = TAG, message = "Starting push service for user ${user.uuid}")
        pushService.start()
    }

    /** Request syncs. */
    private suspend fun Dependency.UserGraph.sync() {
        Telemetry.info(tag = TAG, message = "Requesting sync for user ${user.uuid}")
        repositories.userRepository.syncUsers()
        repositories.registryRepository.syncRegistries()
        if (ClientFlags.flags.tasks) repositories.taskRepository.syncTasks()
    }

    /** Register lifecycles of services. */
    @Composable
    fun RegisterLifecycles() {
        val dependency by dependency.collectAsState()
        val userDependency by userDependency.collectAsState()

        val broadcastService = dependency?.broadcastService
        val pushService = userDependency?.pushService

        RegisterBroadcastLifecycle(broadcastService = broadcastService)
        RegisterPushLifecycle(pushService = pushService)
    }

    /**
     * Creates the [Dependency.AppGraph] graph, using No-Op implementations if platform engines fail to initialize.
     *
     * @return The populated [Dependency.AppGraph] instance.
     */
    private suspend fun createDependency(): Dependency.AppGraph {
        val sqlDriver = runCatching {
            createSqlDriver(
                name = DATABASE_FILE,
                schema = AppDatabase.Schema
            ).also { Telemetry.info(tag = TAG, message = "Sql Driver created") }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to create the Sql Driver", throwable = it)
        }.getOrDefault(defaultValue = NoOpSqlDriver)

        val httpEngine = runCatching {
            createHttpEngine().also { Telemetry.info(tag = TAG, message = "Http Engine created") }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to create the Http Engine", throwable = it)
        }.getOrDefault(defaultValue = NoOpHttpClientEngine)

        return Dependency.AppGraph(
            sqlDriver = sqlDriver,
            httpClientEngine = httpEngine,
        ).also { Telemetry.info(tag = TAG, message = "Dependency Index complete") }
    }
}
