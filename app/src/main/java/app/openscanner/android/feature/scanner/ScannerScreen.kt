package app.openscanner.android.feature.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.camera.compose.CameraXViewfinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.openscanner.android.core.vision.CoordinateMapper
import app.openscanner.android.ui.components.OSButton
import app.openscanner.android.ui.components.OSIconButton
import app.openscanner.android.ui.components.OSIconButtonVariant
import app.openscanner.android.ui.components.OSTopBar
import app.openscanner.android.ui.theme.CardShape
import app.openscanner.android.ui.theme.TextSecondaryAlpha

@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    onCaptured: () -> Unit,
    viewModel: ScannerViewModel = viewModel()
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (hasPermission) {
                CameraContent(
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                PermissionRequest(
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
            Controls(viewModel = viewModel, enabled = hasPermission, onCaptured = onCaptured)
        }
    }
}

@Composable
private fun CameraContent(
    viewModel: ScannerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        viewModel.bindToCamera(context.applicationContext, lifecycleOwner)
    }

    val surfaceRequest by viewModel.surfaceRequest.collectAsStateWithLifecycle()
    val detection by viewModel.detection.collectAsStateWithLifecycle()
    val quadColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.clip(CardShape)) {
        surfaceRequest?.let { request ->
            CameraXViewfinder(
                surfaceRequest = request,
                modifier = Modifier.fillMaxSize()
            )
        } ?: Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        )

        // Live page outline: fill-center mapping from analysis frame to this box.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val d = detection ?: return@Canvas
            val quad = d.quad ?: return@Canvas
            val mapped = CoordinateMapper.mapQuad(
                quad,
                d.frameWidth.toFloat(),
                d.frameHeight.toFloat(),
                size.width,
                size.height
            )
            val path = Path().apply {
                moveTo(mapped.topLeft.x, mapped.topLeft.y)
                lineTo(mapped.topRight.x, mapped.topRight.y)
                lineTo(mapped.bottomRight.x, mapped.bottomRight.y)
                lineTo(mapped.bottomLeft.x, mapped.bottomLeft.y)
                close()
            }
            drawPath(path, color = quadColor.copy(alpha = 0.15f))
            drawPath(path, color = quadColor, style = Stroke(width = 3.dp.toPx()))
            mapped.corners.forEach { corner ->
                drawCircle(
                    color = Color.White,
                    radius = 6.dp.toPx(),
                    center = Offset(corner.x, corner.y)
                )
                drawCircle(
                    color = quadColor,
                    radius = 4.dp.toPx(),
                    center = Offset(corner.x, corner.y)
                )
            }
        }

        StatusHint(
            found = detection?.quad != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        )
    }
}

@Composable
private fun StatusHint(found: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = if (found) "Page found — hold steady" else "Point the camera at a page",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
    }
}

@Composable
private fun Controls(
    viewModel: ScannerViewModel,
    enabled: Boolean,
    onCaptured: () -> Unit
) {
    val torchOn by viewModel.torchOn.collectAsStateWithLifecycle()
    val isCapturing by viewModel.isCapturing.collectAsStateWithLifecycle()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OSIconButton(
            icon = Icons.Rounded.Image,
            contentDescription = "Import from gallery",
            variant = OSIconButtonVariant.Tonal,
            enabled = false, // arrives with gallery import
            onClick = {}
        )
        OSButton(
            label = if (isCapturing) "Capturing…" else "Capture",
            icon = Icons.Rounded.PhotoCamera,
            enabled = enabled && !isCapturing,
            onClick = {
                viewModel.capture(
                    onCaptured = onCaptured,
                    onError = { /* stay on screen; user can retry */ }
                )
            }
        )
        OSIconButton(
            icon = if (torchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
            contentDescription = if (torchOn) "Turn flash off" else "Turn flash on",
            variant = OSIconButtonVariant.Tonal,
            enabled = enabled,
            onClick = { viewModel.toggleTorch() }
        )
    }
}

@Composable
private fun PermissionRequest(
    onRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = TextSecondaryAlpha),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "OpenScanner needs the camera to scan pages.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = TextSecondaryAlpha),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            OSButton(label = "Allow camera access", onClick = onRequest)
        }
    }
}
