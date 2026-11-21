package ai.localcompanion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ai.localcompanion.app.profile.ProfileStore
import ai.localcompanion.app.profile.ThemeMode
import ai.localcompanion.app.ui.OnboardingScreen
import ai.localcompanion.app.ui.StageScreen
import ai.localcompanion.app.ui.theme.LocalCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = ProfileStore(this)
        setContent {
            var profile by remember { mutableStateOf(store.load()) }
            var themeMode by remember { mutableStateOf(store.themeMode()) }
            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LocalCompanionTheme(darkTheme = dark) {
                val current = profile
                if (current == null) {
                    OnboardingScreen(onComplete = { created ->
                        store.save(created)
                        profile = created
                    })
                } else {
                    StageScreen(
                        profile = current,
                        themeMode = themeMode,
                        onThemeModeChange = { mode ->
                            themeMode = mode
                            store.setThemeMode(mode)
                        },
                        onResetOnboarding = {
                            store.clearProfile()
                            profile = null
                        }
                    )
                }
            }
        }
    }
}
