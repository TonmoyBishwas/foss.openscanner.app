package app.openscanner.android.feature.review

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.openscanner.android.core.ScanSession
import app.openscanner.android.core.vision.ScanFilter
import app.openscanner.android.ui.components.OSButton
import app.openscanner.android.ui.components.OSButtonVariant
import app.openscanner.android.ui.components.OSIconButton
import app.openscanner.android.ui.components.OSIconButtonVariant
import app.openscanner.android.ui.components.OSTopBar
import app.openscanner.android.ui.theme.CardShape
import app.openscanner.android.ui.theme.ControlShape
import app.openscanner.android.ui.theme.TextSecondaryAlpha

@Composable
fun ReviewScreen(
    onBack: () -> Unit,
    onRetake: () -> Unit,
    viewModel: ReviewViewModel = viewModel()
) {
    if (ScanSession.cropped == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    LaunchedEffect(Unit) { viewModel.start() }

    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val thumbnails by viewModel.thumbnails.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()

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
                },
                actions = {
                    OSIconButton(
                        icon = Icons.Rounded.RotateRight,
                        contentDescription = "Rotate",
                        onClick = { viewModel.rotate() }
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(CardShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                val current = preview
                if (current != null) {
                    Image(
                        bitmap = current.asImageBitmap(),
                        contentDescription = "Filtered page",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(12.dp))
            FilterRow(
                thumbnails = thumbnails,
                selected = selected,
                onSelect = { viewModel.select(it) }
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
                    enabled = false, // library storage arrives next milestone
                    onClick = {}
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    thumbnails: Map<ScanFilter, android.graphics.Bitmap>,
    selected: ScanFilter,
    onSelect: (ScanFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScanFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(filter) }
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 80.dp)
                        .clip(ControlShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .then(
                            if (isSelected) Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                ControlShape
                            ) else Modifier
                        )
                ) {
                    thumbnails[filter]?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = filter.label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = filter.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = TextSecondaryAlpha)
                )
            }
        }
    }
}
