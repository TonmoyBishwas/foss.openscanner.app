package app.openscanner.android.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.openscanner.android.feature.library.LibraryScreen
import app.openscanner.android.feature.scanner.ScannerScreen

object Routes {
    const val Library = "library"
    const val Scanner = "scanner"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.Library) {
        composable(Routes.Library) {
            LibraryScreen(
                onScanClick = { navController.navigate(Routes.Scanner) }
            )
        }
        composable(Routes.Scanner) {
            ScannerScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
