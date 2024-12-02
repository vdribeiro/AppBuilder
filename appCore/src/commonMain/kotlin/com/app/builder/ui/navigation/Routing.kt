package com.app.builder.ui.navigation

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.UriHandler
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.savedstate.serialization.SavedStateConfiguration
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.ui.modifier.MouseClick
import com.app.builder.ui.modifier.onMouseClick
import com.app.builder.ui.navigation.scene.SplitSceneStrategy

/** CompositionLocal providing the current [Router]. */
val LocalRouter: ProvidableCompositionLocal<Router> = staticCompositionLocalOf { NoOpRouter() }

/**
 * Hosts the application navigation stack and renders the active screen.
 *
 * @param modifier Applied to the navigation container.
 * @param fallbackScreen The screen navigated to when the backstack yields an unknown destination. Its entry must be registered in [entryProviderScope]. When null, an unknown destination throws.
 * @param entryProviderScope The [EntryProviderScope] for the navigation graph.
 */
@Composable
fun Navigation(
    modifier: Modifier = Modifier,
    fallbackScreen: NavKey? = null,
    entryProviderScope: EntryProviderScope<NavKey>.() -> Unit
) {
    val router = LocalRouter.current
    val splitSceneStrategy = remember { SplitSceneStrategy<NavKey>() }
    NavDisplay(
        modifier = modifier.navigationHandler(onBack = { router.back() }),
        onBack = { router.back() },
        backStack = router.backStack,
        sceneStrategies = listOf(
            splitSceneStrategy,
        ),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider(fallback = {
            if (fallbackScreen == null) throw IllegalStateException("Unknown screen $it")
            NavEntry(key = it) { LaunchedEffect(key1 = Unit) { router.navigate(screen = fallbackScreen, option = Router.NavOption.CLEAR) } }
        }) {
            entryProviderScope()
        }
    )
}

/**
 * Intercepts hardware back gestures.
 *
 * @param onBack Invoked when a back event completes.
 */
@Composable
fun NavigationHandler(onBack: () -> Unit) {
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        onBackCompleted = { onBack() }
    )
}

/**
 * Intercepts hardware mouse back button clicks.
 *
 * @param onBack Invoked when the back button is clicked.
 * @return The modifier with the mouse back button listener attached.
 */
fun Modifier.navigationHandler(onBack: () -> Unit): Modifier =
    onMouseClick(mouseClicks = listOf(element = MouseClick.BACK)) { onBack() }

/**
 * Creates a savable [Router] initialized with the provided screens, automatically handling polymorphic serialization for the sealed screen type [T].
 *
 * @param initialStack The screens to seed the router's back stack with.
 * @return The remembered [Router].
 */
@Composable
inline fun <reified T: NavKey> rememberRouter(initialStack: List<T>): Router {
    val backStack: NavBackStack<NavKey> = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(baseClass = NavKey::class) { subclassesOfSealed<T>() }
            }
        },
        *initialStack.toTypedArray()
    )
    return remember { Navigator(backStack = backStack) }
}

/**
 * Safely attempts to open a [uri] using the provided [UriHandler].
 * This extension function provides a wrapper around [UriHandler.openUri] to handle common failures gracefully, such as:
 * - Null URI strings.
 * - Malformed or invalid URI formats.
 * - Lack of a corresponding application on the device to handle the URI.
 *
 * If the operation fails, the error is captured and logged instead of crashing the application.
 *
 * @param uri The string representation of the URI to open.
 */
fun UriHandler.open(uri: String?) {
    runCatching {
        uri ?: throw IllegalArgumentException("Uri is null")
        openUri(uri = uri)
    }.onFailure {
        Telemetry.error(tag = "Routing", message = "Unable to open uri: $uri", throwable = it)
    }
}
