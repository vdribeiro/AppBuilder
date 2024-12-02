package com.app.builder.domain.gateway.translation

import com.app.builder.data.database.TranslationSchema
import com.app.builder.domain.Translation

/**
 * Maps a [TranslationSchema] entity to a [Translation] domain model.
 *
 * @return The corresponding [Translation] instance.
 */
fun TranslationSchema.toTranslation(): Translation = Translation(
    languageIso = languageIso,
    key = key,
    value = value_
)

/**
 * Maps a [Translation] domain model to a [TranslationSchema] entity.
 *
 * @return The corresponding [TranslationSchema] instance.
 */
fun Translation.toTranslationSchema(): TranslationSchema = TranslationSchema(
    languageIso = languageIso,
    key = key,
    value_ = value
)