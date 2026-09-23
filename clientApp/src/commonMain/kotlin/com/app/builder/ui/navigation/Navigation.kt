package com.app.builder.ui.navigation

import kotlin.uuid.Uuid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.app.builder.core.config.ClientFlags
import com.app.builder.domain.EntityType
import com.app.builder.domain.deeplink.DeepLink
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.navigation.provider.audioProvider
import com.app.builder.ui.navigation.provider.authenticationProvider
import com.app.builder.ui.navigation.provider.cameraProvider
import com.app.builder.ui.navigation.provider.configsProvider
import com.app.builder.ui.navigation.provider.deviceLocationProvider
import com.app.builder.ui.navigation.provider.errorProvider
import com.app.builder.ui.navigation.provider.homeProvider
import com.app.builder.ui.navigation.provider.nfcProvider
import com.app.builder.ui.navigation.provider.pushProvider
import com.app.builder.ui.navigation.provider.taskProvider
import com.app.builder.ui.navigation.provider.userProfileProvider
import com.app.builder.ui.navigation.provider.userProvider
import com.app.builder.ui.screen.splash.SplashScreen

/** The screen to show while the use cases are loading. */
val loadingScreen: Screen = Screen.Splash

/** The screen to land on once loaded, for a session that isn't authenticated yet. */
val unauthenticatedScreen: Screen
    get() = when {
        ClientFlags.flags.home -> Screen.Home
        ClientFlags.flags.login -> Screen.Login
        else -> Screen.Error(error = true)
    }

/** The screen to land on once loaded, for an authenticated session. */
val authenticatedScreen: Screen
    get() = when {
        ClientFlags.flags.userProfile -> Screen.UserProfile
        else -> Screen.Error(error = true)
    }

/**
 * The navigation host shown while the use cases are loading.
 *
 * @param modifier The [Modifier] applied to the layout surface of the navigation container.
 */
@Composable
fun LoadingNavigation(
    modifier: Modifier = Modifier,
) {
    Navigation(
        modifier = modifier,
        fallbackScreen = Screen.Error(error = true),
        entryProviderScope = {
            errorProvider()
            entry<Screen.Splash> { SplashScreen() }
        }
    )
}

/**
 * The navigation host for the unauthenticated pathways.
 *
 * @param modifier The [Modifier] applied to the layout surface of the navigation container.
 * @param useCases The unauthenticated use cases.
 */
@Composable
fun UnauthenticatedNavigation(
    modifier: Modifier = Modifier,
    useCases: UseCases?
) {
    Navigation(
        modifier = modifier,
        fallbackScreen = Screen.Error(error = true),
        entryProviderScope = {
            errorProvider()
            if (useCases == null) return@Navigation
            if (ClientFlags.flags.home) homeProvider()
            if (ClientFlags.flags.userProfile) userProfileProvider(useCases = useCases)
            if (ClientFlags.flags.login) authenticationProvider(useCases = useCases)
        }
    )
}

/**
 * The navigation host for the session validated pathways.
 *
 * @param modifier The [Modifier] applied to the layout surface of the navigation container.
 * @param useCases The session validated use cases.
 */
@Composable
fun AuthenticatedNavigation(
    modifier: Modifier = Modifier,
    useCases: UseCases?,
) {
    Navigation(
        modifier = modifier,
        fallbackScreen = Screen.Error(error = true),
        entryProviderScope = {
            errorProvider()
            if (useCases == null) return@Navigation
            configsProvider(useCases = useCases)
            if (ClientFlags.flags.userProfile) userProfileProvider(useCases = useCases)
            if (ClientFlags.flags.users) userProvider(useCases = useCases)
            if (ClientFlags.flags.locationCapture) deviceLocationProvider(useCases = useCases)
            if (ClientFlags.flags.nfc) nfcProvider(useCases = useCases)
            if (ClientFlags.flags.camera) cameraProvider(useCases = useCases)
            if (ClientFlags.flags.music) audioProvider(useCases = useCases)
            if (ClientFlags.flags.notifications) pushProvider(useCases = useCases)
            if (ClientFlags.flags.tasks) taskProvider(useCases = useCases)
        }
    )
}

/** Register deep links. */
@Composable
fun DeepLinks() {
    val router = LocalRouter.current
    LaunchedEffect(key1 = Unit) {
        DeepLink.deepLinks.collect { deepLink ->
            when (deepLink) {
                is DeepLink.Entity -> setBackStack(
                    router = router,
                    entityType = deepLink.entityType,
                    entityUuid = deepLink.entityUuid
                )
            }
        }
    }
}

/**
 * Converts entity information to a synthetic backstack.
 *
 * @param router Navigation router to set the synthetic backstack.
 * @param entityType The domain type of the entity.
 * @param entityUuid The unique UUID of the entity.
 */
private fun setBackStack(
    router: Router,
    entityType: EntityType,
    entityUuid: Uuid?
) {
    // TODO - check app state
    val backStack = when (entityType) {
        EntityType.CLIENT_FLAG -> listOf(Screen.ClientFlags)
        EntityType.CLIENT_CONFIG -> listOf(Screen.ClientConfigs)
        EntityType.SERVER_FLAG -> listOf(Screen.ServerFlags)
        EntityType.SERVER_CONFIG -> listOf(Screen.ServerConfigs)
        EntityType.REGISTRY -> buildList<Screen> { add(Screen.RegistryList); entityUuid?.let { add(Screen.RegistryDetail(uuid = it.toString())) } }
        EntityType.NOTIFICATION -> listOf(Screen.Push)
        EntityType.TRANSLATION -> listOf(Screen.Translations)
        EntityType.SESSION -> listOf(Screen.Login)
        EntityType.USER -> buildList<Screen> { add(Screen.UserList); entityUuid?.let { add(Screen.UserDetail(uuid = it.toString())) } }
        EntityType.DEVICE_LOCATION -> listOf(Screen.DeviceLocation)
        EntityType.TASK -> buildList<Screen> { add(Screen.TaskList); entityUuid?.let { add(Screen.TaskDetail(uuid = it.toString())) } }
        EntityType.FILE -> TODO()
    }
    if (backStack.isNotEmpty()) router.backStack.apply {
        clear()
        addAll(elements = backStack)
    }
}
