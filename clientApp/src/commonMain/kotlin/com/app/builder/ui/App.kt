package com.app.builder.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.app.builder.Application
import com.app.builder.core.media.AudioPlayer
import com.app.builder.core.media.Camera
import com.app.builder.core.nfc.NfcController
import com.app.builder.ui.devicelocation.LocalDeviceLocationProvider
import com.app.builder.ui.media.rememberAudioPlayer
import com.app.builder.ui.media.rememberCamera
import com.app.builder.ui.navigation.AuthenticatedNavigation
import com.app.builder.ui.navigation.DeepLinks
import com.app.builder.ui.navigation.LoadingNavigation
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Navigation
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.UnauthenticatedNavigation
import com.app.builder.ui.navigation.authenticatedScreen
import com.app.builder.ui.navigation.loadingScreen
import com.app.builder.ui.navigation.rememberRouter
import com.app.builder.ui.navigation.unauthenticatedScreen
import com.app.builder.ui.nfc.rememberNfcController
import com.app.builder.ui.permission.LocalPermissionManager
import com.app.builder.ui.permission.Permission
import com.app.builder.ui.permission.PermissionManager
import com.app.builder.ui.permission.rememberPermissionManager

/** CompositionLocal providing the current [AppState]. */
val LocalAppState: ProvidableCompositionLocal<AppState> = staticCompositionLocalOf { AppState.LOADING }

/** CompositionLocal providing the current [AudioPlayer]. */
val LocalAudioPlayer: ProvidableCompositionLocal<AudioPlayer> = staticCompositionLocalOf { AudioPlayer() }

/** CompositionLocal providing the current [Camera]. */
val LocalCamera: ProvidableCompositionLocal<Camera> = staticCompositionLocalOf { Camera() }

/** CompositionLocal providing the current [NfcController]. */
val LocalNfcController: ProvidableCompositionLocal<NfcController> = staticCompositionLocalOf { NfcController() }

/** Application state. */
enum class AppState {
    LOADING,
    UNAUTHENTICATED,
    AUTHENTICATED
}

/**
 * Root Composable for the application UI.
 * Applies the [AppTheme], injects CompositionLocals, and routes to [Navigation].
 *
 * @param modifier Modifier to be applied to the layout.
 * @param compositionValues Platform-specific [ProvidedValue]s injected into the Compose tree.
 */
@Composable
fun App(
    modifier: Modifier = Modifier,
    compositionValues: List<ProvidedValue<*>> = emptyList(),
) {
    val dependency by Application.dependency.collectAsState()
    val userDependency by Application.userDependency.collectAsState()

    val appState = when {
        dependency == null -> AppState.LOADING
        userDependency == null -> AppState.UNAUTHENTICATED
        else -> AppState.AUTHENTICATED
    }

    val router: Router = when (appState) {
        AppState.LOADING -> rememberRouter(initialStack = listOf(element = loadingScreen))
        AppState.UNAUTHENTICATED -> rememberRouter(initialStack = listOf(element = unauthenticatedScreen))
        AppState.AUTHENTICATED -> rememberRouter(initialStack = listOf(element = authenticatedScreen))
    }
    val permissionManager: PermissionManager = rememberPermissionManager()
    val audioPlayer: AudioPlayer = rememberAudioPlayer()
    val camera: Camera = rememberCamera()
    val nfcController: NfcController = rememberNfcController()
    val providedValues = remember(
        compositionValues,
        router,
        permissionManager,
        audioPlayer,
        camera,
        nfcController
    ) {
        compositionValues + listOf(
            LocalAppState provides appState,
            LocalRouter provides router,
            LocalPermissionManager provides permissionManager,
            LocalAudioPlayer provides audioPlayer,
            LocalCamera provides camera,
            LocalNfcController provides nfcController
        )
    }

    AppTheme(compositionValues = providedValues) {
        AppCore {
            Application.RegisterLifecycles()

            LaunchedEffect(key1 = Unit) {
                permissionManager.grantPermission(permission = Permission.NOTIFICATIONS)
            }

            when (appState) {
                AppState.LOADING -> LoadingNavigation(modifier = modifier)
                AppState.UNAUTHENTICATED -> UnauthenticatedNavigation(modifier = modifier, useCases = dependency?.userGraph?.useCases)
                AppState.AUTHENTICATED -> AuthenticatedNavigation(modifier = modifier, useCases = userDependency?.useCases).also {
                    val deviceLocationProvider = LocalDeviceLocationProvider.current
                    LaunchedEffect(key1 = Unit) {
                        if (permissionManager.grantPermission(permission = Permission.LOCATION)) {
                            deviceLocationProvider.startUpdate()
                        }
                    }
                }
            }

            DeepLinks()
        }
    }
}

@Preview
@Composable
private fun AppPreview() = Preview {
    App()
}
