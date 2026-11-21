package ai.localcompanion.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// AIRI-style: one configurable accent hue drives the palette (their default ~220).
private fun scheme(dark: Boolean, hueSeed: Color) = if (dark) {
    darkColorScheme(primary = hueSeed, surface = Color(0xFF14161A), background = Color(0xFF0E1013))
} else {
    lightColorScheme(primary = hueSeed, surface = Color(0xFFF4F5F7), background = Color(0xFFECEEF1))
}

@Composable
fun LocalCompanionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: Color = Color(0xFF5B8DEF), // hue ~220 soft blue, user-configurable later
    content: @Composable () -> Unit
) {
    MaterialTheme(colorScheme = scheme(darkTheme, accent), content = content)
}
