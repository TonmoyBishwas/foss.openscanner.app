package app.openscanner.android.feature.review

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.openscanner.android.core.ScanSession
import app.openscanner.android.ui.components.OSButton
import app.openscanner.android.ui.components.OSButtonVariant
import app.openscanner.android.ui.components.OSIconButton
import app.openscanner.android.ui.components.OSTopBar
import app.openscanner.android.ui.theme.CardShape

@Composable
fun ReviewScreen(
    onBack: () -> Unit,
    onRetake: () -> Unit
) {
    val bitmap = ScanSession.cropped
    if (bitmap == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            OSTopBar(
                title = "Preview",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Cropped page",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(CardShape)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OSButton(
                    label = "Retake",
                    variant = OSButtonVariant.Tonal,
                    onClick = onRetake
                )
                OSButton(
                    label = "Save",
                    enabled = false, // filters + saving arrive next milestone
                    onClick = {}
                )
            }
        }
    }
}
