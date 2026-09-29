package com.app.builder.ui.navigation.provider

import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.Material
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.LocalSplitScreen
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.bar.ActionBarLayout
import com.app.builder.ui.component.bar.ActionBarMode
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.scene.SplitSceneStrategy.Companion.split
import com.app.builder.ui.screen.materialdetail.MaterialDetailScreen
import com.app.builder.ui.screen.materialdetail.MaterialDetailScreenAction
import com.app.builder.ui.screen.materialdetail.MaterialDetailScreenState
import com.app.builder.ui.screen.materialdetail.MaterialDetailScreenStore
import com.app.builder.ui.screen.materiallist.MaterialListScreen
import com.app.builder.ui.screen.materiallist.MaterialListScreenAction
import com.app.builder.ui.screen.materiallist.MaterialListScreenState
import com.app.builder.ui.screen.materiallist.MaterialListScreenStore

/**
 * The material routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.materialProvider(useCases: UseCases) {
    val properties = Material.Property.entries.map { it.name }.toPersistentList()
    val propertyMap = Material.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap()
    val defaultFilterCriteria = AppFile.ActionBarData(
        mode = ActionBarMode.DEFAULT.name,
        search = "",
        sortProperty = Material.Property.MODIFIED_AT.name,
        sortAscending = false,
        visibleProperties = properties,
        searchableProperties = properties,
    )
    val actionBarState = ActionBarState(
        sortProperty = defaultFilterCriteria.sortProperty,
        sortAscending = defaultFilterCriteria.sortAscending,
        properties = propertyMap,
        visibleProperties = properties,
        searchableProperties = properties
    )

    entry<Screen.MaterialList>(metadata = split()) {
        val router = LocalRouter.current
        val store = viewModel { MaterialListScreenStore(router = router, state = MaterialListScreenState(), materialUseCases = useCases.materialUseCases) }
        MaterialListScreen(
            actionBarStore = viewModel {
                ActionBarStore(
                    state = actionBarState.copy(title = "materials", layout = ActionBarLayout.LIST),
                    router = router,
                    authenticationUseCases = useCases.authenticationUseCases,
                    storageFile = AppFile.MaterialPreferences,
                    defaults = defaultFilterCriteria,
                    entityType = EntityType.MATERIAL,
                    onOkClick = { mode -> store.send(action = MaterialListScreenAction.Ok(mode = mode)) }
                )
            },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.MATERIAL), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = store
        )
    }
    entry<Screen.MaterialDetail>(metadata = split()) {
        val router = LocalRouter.current
        val splitScreen = LocalSplitScreen.current
        val store = viewModel { MaterialDetailScreenStore(state = MaterialDetailScreenState(), router = router, materialUseCases = useCases.materialUseCases, materialUuid = it.uuid) }
        MaterialDetailScreen(
            actionBarStore = viewModel {
                ActionBarStore(
                    state = actionBarState.copy(title = "material", layout = if (splitScreen) ActionBarLayout.ALL else ActionBarLayout.DETAIL),
                    router = router,
                    authenticationUseCases = useCases.authenticationUseCases,
                    storageFile = AppFile.MaterialPreferences,
                    defaults = defaultFilterCriteria,
                    entityType = EntityType.MATERIAL,
                    onOkClick = { mode -> store.send(action = MaterialDetailScreenAction.Ok(mode = mode)) },
                    onCancelClick = { mode -> store.send(action = MaterialDetailScreenAction.Cancel(mode = mode)) }
                )
            },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.MATERIAL), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = store
        )
    }
}
