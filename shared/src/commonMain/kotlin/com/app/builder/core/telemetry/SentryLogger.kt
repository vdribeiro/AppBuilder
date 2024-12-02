package com.app.builder.core.telemetry

import kotlin.concurrent.Volatile
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb
import io.sentry.kotlin.multiplatform.protocol.UserFeedback

/** Sentry integration for remote crash reporting and telemetry. */
object SentryLogger: TelemetryEngine {

    /** Whether the underlying Sentry SDK has already been initialized. */
    @Volatile
    private var initialized = false

    /**
     * Initializes the Sentry SDK.
     * If already initialized, this is a no-op: the native SDK does not cleanly support re-initialization on every platform (notably iOS, where it leaks the previous integrations).
     *
     * @param options The configuration used to set up the Sentry client.
     * @return This [SentryLogger] instance.
     */
    fun init(options: Options): SentryLogger = apply {
        if (initialized) return@apply
        initialized = true

        Sentry.init { sentryOptions ->
            sentryOptions.dsn = options.dsn
            sentryOptions.sampleRate = options.sampleRate
            sentryOptions.tracesSampleRate = options.tracesSampleRate
            sentryOptions.release = options.release
        }
    }

    /**
     * Options to configure the Sentry.
     *
     * @property dsn The Data Source Name identifying the Sentry project to report events to.
     * @property release The release version identifier attached to reported events.
     * @property sampleRate The fraction of error events that are sent, from 0.0 to 1.0.
     * @property tracesSampleRate The fraction of transactions that are sent for performance monitoring, from 0.0 to 1.0.
     */
    data class Options(
        val dsn: String? = null,
        val release: String? = null,
        val sampleRate: Double? = 1.0,
        val tracesSampleRate: Double? = 0.5
    )

    /**
     * Records a diagnostic breadcrumb to provide context for future error reports.
     *
     * @param tag The identifier mapped to the breadcrumb category.
     * @param message The detailed description of the informational event.
     */
    override fun info(tag: String, message: String) {
        if (!initialized) return
        Sentry.addBreadcrumb(breadcrumb = Breadcrumb().apply {
            this.level = SentryLevel.INFO
            this.category = tag
            this.message = message
        })
    }

    /**
     * Immediately captures and transmits an error message or exception payload to Sentry.
     *
     * @param tag The identifier injected as a searchable event tag.
     * @param message The context description of the error.
     * @param throwable The optional exception containing stack trace details.
     */
    override fun error(tag: String, message: String, throwable: Throwable?) {
        if (!initialized) return
        when {
            throwable == null -> Sentry.captureMessage(message = message) { scope ->
                scope.setTag(key = "tag", value = tag)
            }

            else -> Sentry.captureException(throwable = throwable) { scope ->
                scope.setTag(key = "tag", value = tag)
                scope.setExtra(key = "message", value = message)
            }
        }
    }

    /**
     * Transmits a user-submitted feedback report associated with a new message event.
     *
     * @param message The textual commentary provided by the user.
     */
    override fun feedback(message: String) {
        if (!initialized) return
        Sentry.captureUserFeedback(userFeedback = UserFeedback(sentryId = Sentry.captureMessage(message = message)).apply {
            this.comments = message
        })
    }
}