package com.app.builder.ui.navigation

import kotlin.uuid.Uuid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.builder.core.config.ClientFlags
import com.app.builder.domain.EntityType
import com.app.builder.domain.deeplink.DeepLink
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.navigation.audio.audioProvider
import com.app.builder.ui.navigation.camera.cameraProvider
import com.app.builder.ui.navigation.configs.configsProvider
import com.app.builder.ui.navigation.design.designProvider
import com.app.builder.ui.navigation.devicefiles.deviceFilesProvider
import com.app.builder.ui.navigation.devicelocation.deviceLocationProvider
import com.app.builder.ui.navigation.error.errorProvider
import com.app.builder.ui.navigation.home.homeProvider
import com.app.builder.ui.navigation.login.authenticationProvider
import com.app.builder.ui.navigation.nfc.nfcProvider
import com.app.builder.ui.navigation.push.pushProvider
import com.app.builder.ui.navigation.registry.registryProvider
import com.app.builder.ui.navigation.splash.SplashScreen
import com.app.builder.ui.navigation.task.taskProvider
import com.app.builder.ui.navigation.translations.translationsProvider
import com.app.builder.ui.navigation.user.userProvider
import com.app.builder.ui.navigation.userprofile.userProfileProvider
import com.app.builder.ui.store.Store

/** The screen to show while the use cases are loading. */
val loadingScreen: Screen = Screen.Splash

/** The screen to land on once loaded, for a session that isn't authenticated yet. */
val unauthenticatedScreen: Screen
    get() = when {
        ClientFlags.value.home -> Screen.Home
        ClientFlags.value.login -> Screen.Login
        else -> Screen.Error(error = true)
    }

/** The screen to land on once loaded, for an authenticated session. */
val authenticatedScreen: Screen
    get() = when {
        ClientFlags.value.home -> Screen.Home
        ClientFlags.value.userProfile -> Screen.UserProfile
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
    val navigationStore = remember { Store<NavigationState, Unit>(initialState = NavigationState()) }
    Navigation(
        modifier = modifier,
        fallbackScreen = Screen.Error(error = true),
        entryProviderScope = {
            errorProvider()
            if (useCases == null) return@Navigation
            if (ClientFlags.value.home) homeProvider(useCases = useCases, navigationStore = navigationStore)
            if (ClientFlags.value.userProfile) userProfileProvider(useCases = useCases)
            if (ClientFlags.value.login) authenticationProvider(useCases = useCases)
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
    val navigationStore = useCases?.let { rememberNavigationStore(useCases = it) }
    Navigation(
        modifier = modifier,
        fallbackScreen = Screen.Error(error = true),
        entryProviderScope = {
            errorProvider()
            if (useCases == null || navigationStore == null) return@Navigation
            configsProvider(useCases = useCases, navigationStore = navigationStore)
            if (ClientFlags.value.home) homeProvider(useCases = useCases, navigationStore = navigationStore)
            if (ClientFlags.value.design) designProvider(useCases = useCases, navigationStore = navigationStore)
            if (ClientFlags.value.userProfile) userProfileProvider(useCases = useCases)
            if (ClientFlags.value.users) userProvider(useCases = useCases, navigationStore = navigationStore)
            if (ClientFlags.value.locationCapture) deviceLocationProvider(useCases = useCases, navigationStore = navigationStore)
            if (ClientFlags.value.nfc) nfcProvider(useCases = useCases, navigationStore = navigationStore)
            if (ClientFlags.value.camera) cameraProvider(useCases = useCases, navigationStore = navigationStore)
            if (ClientFlags.value.deviceFiles) deviceFilesProvider(useCases = useCases, navigationStore = navigationStore)
            if (ClientFlags.value.music) audioProvider(useCases = useCases, navigationStore = navigationStore)
            if (ClientFlags.value.notifications) pushProvider(useCases = useCases, navigationStore = navigationStore)
            if (ClientFlags.value.registries) registryProvider(useCases = useCases, navigationStore = navigationStore)
            if (ClientFlags.value.tasks) taskProvider(useCases = useCases, navigationStore = navigationStore)
            translationsProvider(useCases = useCases, navigationStore = navigationStore)
        }
    )
}

/**
 * Remembers the navigation bar store shared by every screen of a navigation host, cleared when the host leaves the composition or the use cases change.
 *
 * @param useCases The use cases the store observes.
 * @return The shared navigation bar store.
 */
@Composable
private fun rememberNavigationStore(useCases: UseCases): Store<NavigationState, Unit> {
    val owner = remember(key1 = useCases) {
        object: ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(key1 = owner) {
        onDispose { owner.viewModelStore.clear() }
    }
    return viewModel(viewModelStoreOwner = owner) { NavigationStore(state = NavigationState(), authenticationUseCases = useCases.authenticationUseCases) }
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
        EntityType.CLIENT_FLAG,
        EntityType.CLIENT_CONFIG,
        EntityType.SERVER_FLAG,
        EntityType.SERVER_CONFIG -> listOf(Screen.Configs)

        EntityType.REGISTRY -> buildList<Screen> { add(Screen.RegistryList); entityUuid?.let { add(Screen.RegistryDetail(uuid = it.toString())) } }
        EntityType.NOTIFICATION -> listOf(Screen.Push)
        EntityType.TRANSLATION -> listOf(Screen.Translations)
        EntityType.ACCOUNT -> listOf(Screen.Login)
        EntityType.USER -> buildList<Screen> { add(Screen.UserList); entityUuid?.let { add(Screen.UserDetail(uuid = it.toString())) } }
        EntityType.DEVICE_LOCATION -> listOf(Screen.DeviceLocation)
        EntityType.NFC -> listOf(Screen.NFC)
        EntityType.TASK -> buildList<Screen> { add(Screen.TaskList); entityUuid?.let { add(Screen.TaskDetail(uuid = it.toString())) } }
    }
    if (backStack.isNotEmpty()) router.backStack.apply {
        clear()
        addAll(elements = backStack)
    }
}
