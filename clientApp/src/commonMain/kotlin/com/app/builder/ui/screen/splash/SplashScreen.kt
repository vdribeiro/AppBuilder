package com.app.builder.ui.screen.splash

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import com.app.builder.ui.Preview
import com.app.builder.ui.core.progress.ProgressIndicator
import com.app.builder.ui.screen.Screen

/** The Splash Screen. */
@Composable
fun SplashScreen() {
    Screen(contentAlignment = Alignment.Center) {
        ProgressIndicator()
    }
}

@Preview
@Composable
private fun SplashScreenPreview() = Preview {
    SplashScreen()
}
