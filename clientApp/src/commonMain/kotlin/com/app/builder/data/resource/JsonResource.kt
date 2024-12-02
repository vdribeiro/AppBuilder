package com.app.builder.data.resource

/**
 * A type-safe registry for bundled JSON assets, serving as a centralized index for static data files.
 * Eliminates the need for hardcoded string paths to reduce the risk of asset resolution errors.
 *
 * @property path The relative file path locating the JSON asset within the `commonMain/composeResources/files` directory.
 */
sealed class JsonResource(val path: String) {
    /** The bundled localization strings used to populate the translation dictionary. */
    data object Translations: JsonResource(path = "files/translations.json")
}
