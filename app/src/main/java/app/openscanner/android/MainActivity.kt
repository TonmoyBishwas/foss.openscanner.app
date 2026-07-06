package app.openscanner.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.openscanner.android.data.ThemeMode
import app.openscanner.android.ui.AppNavHost
import app.openscanner.android.ui.theme.OpenScannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as OpenScannerApplication).container.settingsRepository
        setContent {
            val themeMode by settings.theme.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            OpenScannerTheme(darkTheme = darkTheme) {
                AppNavHost()
            }
        }
    }
}
