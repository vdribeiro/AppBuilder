package com.app.builder.ui.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel

/** CompositionLocal providing the owner of the stores shared by every screen of the current navigation host. */
val LocalHostViewModelStoreOwner: ProvidableCompositionLocal<ViewModelStoreOwner> = staticCompositionLocalOf { error("No host ViewModelStoreOwner provided") }

/**
 * Provides [LocalHostViewModelStoreOwner] to [content], clearing its stores when it leaves the composition or the [key] changes.
 *
 * @param key The value the shared stores depend on, such as the host's use cases.
 * @param content The navigation host sharing the stores.
 */
@Composable
fun HostViewModelStoreOwner(key: Any?, content: @Composable () -> Unit) {
    val owner = remember(key1 = key) {
        object: ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(key1 = owner) {
        onDispose { owner.viewModelStore.clear() }
    }
    CompositionLocalProvider(LocalHostViewModelStoreOwner provides owner, content = content)
}

/**
 * Gets the [ViewModel] shared by every screen of the navigation host under [key], creating it on first use.
 *
 * @param key The key identifying the store among the host's shared stores.
 * @param initializer Creates the store when the host doesn't hold it yet.
 * @return The shared store.
 */
@Composable
inline fun <reified VM: ViewModel> hostViewModel(key: String, noinline initializer: CreationExtras.() -> VM): VM =
    viewModel(viewModelStoreOwner = LocalHostViewModelStoreOwner.current, key = key, initializer = initializer)
