package app.openscanner.android.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.openscanner.android.core.ScanSession
import app.openscanner.android.feature.crop.CropScreen
import app.openscanner.android.feature.document.DocumentDetailScreen
import app.openscanner.android.feature.library.LibraryScreen
import app.openscanner.android.feature.review.ReviewScreen
import app.openscanner.android.feature.scanner.ScannerScreen
import app.openscanner.android.feature.settings.SettingsScreen

object Routes {
    const val Library = "library"
    const val Scanner = "scanner"
    const val Crop = "crop"
    const val Review = "review"
    const val Document = "document/{documentId}"
    const val Settings = "settings"

    fun document(documentId: String) = "document/$documentId"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.Library) {
        composable(Routes.Library) {
            LibraryScreen(
                onScanClick = {
                    ScanSession.clear()
                    navController.navigate(Routes.Scanner)
                },
                onDocumentClick = { id -> navController.navigate(Routes.document(id)) },
                onSettingsClick = { navController.navigate(Routes.Settings) }
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.Scanner) {
            ScannerScreen(
                onBack = { navController.popBackStack() },
                onCaptured = { navController.navigate(Routes.Crop) }
            )
        }
        composable(Routes.Crop) {
            CropScreen(
                onBack = { navController.popBackStack() },
                onCropped = { navController.navigate(Routes.Review) }
            )
        }
        composable(Routes.Review) {
            ReviewScreen(
                onBack = { navController.popBackStack() },
                onRetake = {
                    navController.popBackStack(Routes.Scanner, inclusive = false)
                },
                onSaved = { documentId, wasAppend ->
                    if (wasAppend) {
                        // Return to the document the page was added to.
                        navController.popBackStack(Routes.Document, inclusive = false)
                    } else {
                        navController.popBackStack(Routes.Library, inclusive = false)
                        navController.navigate(Routes.document(documentId))
                    }
                }
            )
        }
        composable(
            route = Routes.Document,
            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
        ) {
            DocumentDetailScreen(
                onBack = { navController.popBackStack() },
                onAddPage = { documentId ->
                    ScanSession.clear()
                    ScanSession.targetDocumentId = documentId
                    navController.navigate(Routes.Scanner)
                }
            )
        }
    }
}
