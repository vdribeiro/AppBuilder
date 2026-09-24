package com.app.builder.ui.screen.configs

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.list.ConfigItem
import com.app.builder.ui.component.list.ConfigList
import com.app.builder.ui.component.list.ConfigValue
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.core.list.LazyColumn
import com.app.builder.ui.core.navigation.NavigationItem
import com.app.builder.ui.core.navigation.NavigationRail
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.showcase.CategorySection
import com.app.builder.ui.store.Store

/**
 * The Config Screen, stacking the client and server flags and configs the current user may browse as one scrollable page, with a side navigation rail jumping to each section.
 *
 * @param actionBarStore The store for action bar.
 * @param navigationStore The store driving the bottom navigation bar.
 * @param store The store driving the config sections displayed in the screen.
 */
@Composable
fun ConfigsScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
    store: Store<ConfigScreenState, ConfigScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val selectedIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex.coerceIn(minimumValue = 0, maximumValue = maxOf(a = state.sections.lastIndex, b = 0)) }
    }

    Screen(
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore) }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                modifier = Modifier.testTag(tag = "config_navigation_rail"),
                items = state.sections.mapIndexed { index, section ->
                    NavigationItem(
                        selected = index == selectedIndex,
                        text = section.configType.text,
                        icon = section.configType.icon,
                        onClick = { coroutineScope.launch { listState.animateScrollToItem(index = index) } }
                    )
                }.toPersistentList()
            )
            LazyColumn(
                modifier = Modifier.testTag(tag = "config_sections"),
                state = listState
            ) {
                state.sections.forEach { section ->
                    item(key = section.configType) {
                        CategorySection(title = section.configType.text) {
                            ConfigList(
                                items = section.items,
                                onValueChange = { item, value ->
                                    store.send(
                                        action = ConfigScreenAction.UpdateValue(
                                            configType = section.configType,
                                            item = item,
                                            value = value
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ConfigsScreenPreview() = Preview {
    ConfigsScreen(
        actionBarStore = Store(initialState = ActionBarState(avatarName = "Sonic")),
        navigationStore = Store(initialState = NavigationState()),
        store = Store(
            initialState = ConfigScreenState(
                sections = ConfigType.entries.map { configType ->
                    ConfigSection(
                        configType = configType,
                        items = persistentListOf(
                            ConfigItem(
                                name = "http",
                                description = "Toggles outbound networking.",
                                value = ConfigValue.Toggle(value = true)
                            )
                        )
                    )
                }.toPersistentList()
            )
        )
    )
}
