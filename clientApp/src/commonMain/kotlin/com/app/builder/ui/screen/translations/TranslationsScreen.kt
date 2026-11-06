package com.app.builder.ui.screen.translations

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.list.TranslationItem
import com.app.builder.ui.component.list.TranslationList
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.core.navigation.Tab
import com.app.builder.ui.core.navigation.Tabs
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The Translations Screen, showing one tab per available language above the translations of the selected one.
 *
 * @param actionBarStore The store for action bar.
 * @param navigationStore Drives the bottom navigation bar.
 * @param store Provides the translations state and receives its actions.
 */
@Composable
fun TranslationsScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
    store: Store<TranslationsScreenState, TranslationsScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore) },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (state.languages.isNotEmpty()) {
                Tabs(
                    modifier = Modifier.testTag(tag = "translation_languages"),
                    selectedTabIndex = state.languages.indexOf(element = state.selectedLanguage).coerceAtLeast(minimumValue = 0),
                    items = state.languages.map { languageIso ->
                        Tab(
                            text = languageIso,
                            onClick = { store.send(action = TranslationsScreenAction.SelectLanguage(languageIso = languageIso)) }
                        )
                    }.toPersistentList()
                )
            }
            TranslationList(items = state.translations)
        }
    }
}

@PreviewLightDark
@Composable
private fun TranslationsScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "translation_key" to "Key",
            "translation_value" to "Value",
            "en" to "English",
            "pt" to "Português",
        )
    )
    TranslationsScreen(
        actionBarStore = previewStore(initialState = ActionBarState(avatarName = "Translations")),
        navigationStore = previewStore(initialState = NavigationState()),
        store = previewStore(
            initialState = TranslationsScreenState(
                languages = persistentListOf("en", "pt"),
                selectedLanguage = "en",
                translations = persistentListOf(
                    TranslationItem(id = "app_name", key = "app_name", value = "App Builder"),
                    TranslationItem(id = "yes", key = "yes", value = "Yes"),
                )
            )
        )
    )
}
