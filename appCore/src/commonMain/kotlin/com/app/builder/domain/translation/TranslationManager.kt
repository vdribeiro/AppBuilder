package com.app.builder.domain.translation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.DEFAULT_LANGUAGE
import com.app.builder.core.locale.observeLocale
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.translation.TranslationCache

/**
 * Manager implementation for Translation workflows.
 * Manages the lifecycle of a reactive translation observer.
 *
 * @property translationStore The persistence coordinator used to observe translations.
 */
class TranslationManager(
    private val translationStore: TranslationStore
): TranslationService {

    /** An isolated scope for the manager's coroutine operations. */
    private val scope = CoroutineScope(context = SupervisorJob())

    /** Guards concurrent start()/stop() transitions. */
    private val mutex = Mutex()

    /** Tracks the currently active translation observer loop. */
    private var job: Job? = null

    override suspend fun stop() = withContext(context = Dispatcher.Default) {
        mutex.withLock {
            job?.cancelAndJoin()
            job = null
        }
    }

    override fun start() {
        if (!mutex.tryLock()) return
        try {
            startTranslationsJob()
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Syncs then observes translations pushing them to the cache.
     * If the job is already running, this is a no-op.
     */
    private fun startTranslationsJob() {
        if (job?.isActive != true) job = scope.launch(context = Dispatcher.Default) {
            withContext(context = Dispatcher.IO) { translationStore.syncTranslations() }
            observeLocale()
                .flatMapLatest { iso ->
                    translationStore.observeTranslations(languageIso = iso)
                        .flatMapLatest { translations ->
                            if (translations.isEmpty() && iso != DEFAULT_LANGUAGE) {
                                translationStore.observeTranslations(languageIso = DEFAULT_LANGUAGE)
                            } else flowOf(value = translations)
                        }
                }
                .flowOn(context = Dispatcher.IO)
                .collectLatest { translations ->
                    if (translations.isEmpty()) Telemetry.info(tag = TAG, message = "Clearing language cache") else {
                        Telemetry.info(tag = TAG, message = "Setting translations for language: ${translations.firstOrNull()?.languageIso}")
                    }
                    TranslationCache.set(translations = translations.associate { it.key to it.value })
                }
        }
    }

    companion object {
        private const val TAG = "TranslationManager"
    }
}