package com.app.builder.ui.core.image

import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkFetcher
import coil3.network.ktor3.asNetworkClient
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.createNetworkEngine

/** The [PlatformContext] captured by [RegisterImageLoader], used to reach the [SingletonImageLoader] outside composition. */
private var platformContext: PlatformContext? = null

/** Builds the [ImageLoader]. */
private object NetworkImageLoaderFactory: SingletonImageLoader.Factory {
    /** The [HttpClient] used to fetch remote images, created lazily on first use. */
    private val httpClient by lazy { HttpClient(engine = createNetworkEngine()) }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context = context)
            .components { add(factory = NetworkFetcher.Factory(networkClient = { httpClient.asNetworkClient() })) }
            .build()
}

/** Registers [NetworkImageLoaderFactory] as the [SingletonImageLoader], if one hasn't already been set. */
@Composable
internal fun RegisterImageLoader() {
    platformContext = LocalPlatformContext.current
    remember { SingletonImageLoader.setSafe(factory = NetworkImageLoaderFactory) }
}

/**
 * Clears the [SingletonImageLoader]'s disk and memory caches.
 *
 * @return `true` if the operation completes successfully, `false` if [RegisterImageLoader] hasn't run yet or an error occurs.
 */
suspend fun clearImageCache(): Boolean = withContext(context = Dispatcher.IO) {
    runCatching {
        val context = platformContext ?: throw IllegalStateException("Image loader not registered")
        val imageLoader = SingletonImageLoader.get(context = context)
        imageLoader.diskCache?.clear()
        imageLoader.memoryCache?.clear()
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to clear image cache", throwable = it)
    }.getOrDefault(defaultValue = false)
}

private const val TAG = "ImageLoader"
