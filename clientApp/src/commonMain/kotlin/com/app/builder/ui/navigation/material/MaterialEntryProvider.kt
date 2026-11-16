package com.app.builder.ui.navigation.material

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.lifecycle.hostViewModel
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.building.buildingFiltersStore
import com.app.builder.ui.navigation.scene.SplitSceneStrategy.Companion.split

/**
 * The material routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.materialProvider(useCases: UseCases) {
    entry<Screen.MaterialList>(metadata = split()) {
        val router = LocalRouter.current
        MaterialListScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = materialFiltersStore(),
            store = viewModel { MaterialListScreenStore(router = router, state = MaterialListScreenState(), materialUseCases = useCases.materialUseCases) }
        )
    }
    entry<Screen.MaterialDetail>(metadata = split()) {
        val router = LocalRouter.current
        MaterialDetailScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = materialFiltersStore(),
            buildingFiltersStore = buildingFiltersStore(),
            store = viewModel {
                MaterialDetailScreenStore(
                    state = MaterialDetailScreenState(),
                    router = router,
                    materialUseCases = useCases.materialUseCases,
                    buildingMaterialUseCases = useCases.buildingMaterialUseCases,
                    buildingUseCases = useCases.buildingUseCases,
                    materialUuid = it.uuid,
                    new = it.new
                )
            }
        )
    }
}
