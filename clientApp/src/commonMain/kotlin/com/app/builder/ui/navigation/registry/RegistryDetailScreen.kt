package com.app.builder.ui.navigation.registry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.locale.now
import com.app.builder.core.platform.OS
import com.app.builder.core.security.uuid
import com.app.builder.domain.EntityType
import com.app.builder.domain.Registry
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalSplitScreen
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.card.RegistryCard
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The user-facing Registry Detail Screen, showing a single registry.
 *
 * @param sessionStore The session store shared by every screen.
 * @param filtersStore The registry filters, shown when the registry list is beside this screen.
 * @param store Provides the registry detail state.
 */
@Composable
fun RegistryDetailScreen(
    sessionStore: Store<SessionState, Unit>,
    filtersStore: Store<FiltersState, FiltersAction>,
    store: Store<RegistryDetailScreenState, Unit>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val registry = state.registry

    val router = LocalRouter.current
    val splitScreen = LocalSplitScreen.current

    Screen(
        onBackClick = { router.back() },
        topBar = { ActionBar(sessionStore = sessionStore, filtersStore = filtersStore.takeIf { splitScreen }) },
        bottomBar = { Navigation(sessionStore = sessionStore, selected = NavigationRoute.REGISTRY) },
    ) {
        RegistryCard(
            requestUuid = registry?.requestUuid?.toString(),
            requestType = registry?.requestType,
            requestUtc = registry?.requestUtc?.toString(),
            requestSentUtc = registry?.requestSentUtc?.toString(),
            userUuid = registry?.userUuid?.toString(),
            appVersion = registry?.appVersion,
            os = registry?.os?.toString(),
            osVersion = registry?.osVersion,
            brand = registry?.brand,
            model = registry?.model,
            deviceUuid = registry?.deviceUuid?.toString(),
            entityUuid = registry?.entityUuid?.toString(),
            entityType = registry?.entityType?.name,
            payload = registry?.payload,
        )
    }
}

@PreviewLightDark
@Composable
private fun RegistryDetailScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "registry_request_type" to "Request Type",
            "registry_entity_type" to "Entity Type",
        )
    )
    val registry = Registry(
        requestUuid = uuid(),
        requestType = "POST",
        requestUtc = now(),
        requestSentUtc = now(),
        userUuid = uuid(),
        appVersion = "1.0.0",
        os = OS.Android,
        osVersion = "16",
        brand = "Google",
        model = "Pixel",
        deviceUuid = uuid(),
        entityUuid = uuid(),
        entityType = EntityType.TASK,
        payload = null
    )
    RegistryDetailScreen(
        sessionStore = previewStore(initialState = SessionState()),
        filtersStore = previewStore(initialState = FiltersState()),
        store = previewStore(initialState = RegistryDetailScreenState(registry = registry))
    )
}
