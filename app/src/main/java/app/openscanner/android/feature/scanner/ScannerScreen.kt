package app.openscanner.android.feature.scanner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.openscanner.android.ui.components.OSIconButton
import app.openscanner.android.ui.components.OSTopBar
import app.openscanner.android.ui.theme.TextSecondaryAlpha

@Composable
fun ScannerScreen(
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            OSTopBar(
                title = "Scan",
                navigationIcon = {
                    OSIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Camera coming soon.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = TextSecondaryAlpha)
            )
        }
    }
}
