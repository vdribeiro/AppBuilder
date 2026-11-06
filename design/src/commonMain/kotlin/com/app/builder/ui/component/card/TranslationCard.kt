package com.app.builder.ui.component.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.text.InputField
import com.app.builder.ui.core.card.Card

/**
 * A read-only translation card, showing only the properties that are not `null`.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param key The key of the translation.
 * @param value The localized value of the translation.
 */
@Composable
fun TranslationCard(
    modifier: Modifier = Modifier,
    key: String? = null,
    value: String? = null,
) {
    val fields = listOf(
        "translation_key" to key,
        "translation_value" to value,
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            fields.forEach { (title, value) ->
                value?.let {
                    InputField(
                        enabled = false,
                        title = title,
                        value = it,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun TranslationCardPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "translation_key" to "Key",
            "translation_value" to "Value",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        TranslationCard(
            key = "app_name",
            value = "App Builder"
        )
        TranslationCard(
            value = "Yes"
        )
    }
}
