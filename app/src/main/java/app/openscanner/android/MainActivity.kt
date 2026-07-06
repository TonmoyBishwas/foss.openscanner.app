package app.openscanner.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.openscanner.android.ui.AppNavHost
import app.openscanner.android.ui.theme.OpenScannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenScannerTheme {
                AppNavHost()
            }
        }
    }
}
