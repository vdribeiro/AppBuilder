package com.app.builder.ui.navigation.translations

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
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.list.TranslationItem
import com.app.builder.ui.component.list.TranslationList
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.core.navigation.Tab
import com.app.builder.ui.core.navigation.Tabs
import com.app.builder.ui.getTranslation
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The Translations Screen, showing one tab per available language above the translations of the selected one.
 *
 * @param sessionStore The session store shared by every screen.
 * @param filtersStore The translation filters.
 * @param store Provides the translations state and receives its actions.
 */
@Composable
fun TranslationsScreen(
    sessionStore: Store<SessionState, Unit>,
    filtersStore: Store<FiltersState, FiltersAction>,
    store: Store<TranslationsScreenState, TranslationsScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = { ActionBar(sessionStore = sessionStore, filtersStore = filtersStore) },
        bottomBar = { Navigation(sessionStore = sessionStore, selected = NavigationRoute.TRANSLATION) },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (state.languages.isNotEmpty()) {
                Tabs(
                    modifier = Modifier.testTag(tag = "translation_languages"),
                    selectedTabIndex = state.languages.indexOf(element = state.selectedLanguage).coerceAtLeast(minimumValue = 0),
                    items = state.languages.map { languageIso ->
                        Tab(
                            text = getTranslation(key = "translation_language_$languageIso", default = languageIso),
                            onClick = { store.send(action = TranslationsScreenAction.SelectLanguage(languageIso = languageIso)) }
                        )
                    }.toPersistentList()
                )
            }
            TranslationList(
                items = state.translations,
                enabled = state.editable,
                onValueChange = { item, value -> store.send(action = TranslationsScreenAction.UpdateValue(key = item.id, value = value)) }
            )
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
            "translation_language_en" to "English",
            "translation_language_pt" to "Português",
        )
    )
    TranslationsScreen(
        sessionStore = previewStore(initialState = SessionState()),
        filtersStore = previewStore(initialState = FiltersState()),
        store = previewStore(
            initialState = TranslationsScreenState(
                languages = persistentListOf("en", "pt"),
                selectedLanguage = "en",
                translations = persistentListOf(
                    TranslationItem(id = "login_app_name", key = "login_app_name", value = "App Builder"),
                    TranslationItem(id = "login_submit", key = "login_submit", value = "Login"),
                )
            )
        )
    )
}
