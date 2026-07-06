package app.openscanner.android.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.openscanner.android.ui.components.OSButton
import app.openscanner.android.ui.components.OSTopBar
import app.openscanner.android.ui.theme.Doto
import app.openscanner.android.ui.theme.TextSecondaryAlpha
import app.openscanner.android.ui.theme.TextTertiaryAlpha

@Composable
fun LibraryScreen(
    onScanClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            OSTopBar(
                title = "OpenScanner",
                titleStyle = MaterialTheme.typography.headlineMedium.copy(fontFamily = Doto)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyLibrary(onScanClick = onScanClick)
        }
    }
}

@Composable
private fun EmptyLibrary(onScanClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Rounded.DocumentScanner,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = TextTertiaryAlpha),
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No scans yet",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Scan a page and it will show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = TextSecondaryAlpha),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        OSButton(
            label = "Scan a page",
            icon = Icons.Rounded.PhotoCamera,
            onClick = onScanClick
        )
    }
}
