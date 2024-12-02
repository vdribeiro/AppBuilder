package com.app.builder.ui

import androidx.compose.runtime.Composable

/**
 * Applies the [AppTheme] for accurate rendering in previews.
 *
 * @param content The composable layout to render within the preview.
 */
@Composable
fun Preview(content: @Composable () -> Unit) {
    AppTheme {
        content()
    }
}
