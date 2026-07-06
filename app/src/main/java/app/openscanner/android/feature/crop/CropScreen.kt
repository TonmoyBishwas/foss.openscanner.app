package app.openscanner.android.feature.crop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.openscanner.android.core.ScanSession
import app.openscanner.android.core.vision.CoordinateMapper
import app.openscanner.android.core.vision.PerspectiveCorrector
import app.openscanner.android.core.vision.Point2
import app.openscanner.android.core.vision.Quad
import app.openscanner.android.ui.components.OSButton
import app.openscanner.android.ui.components.OSButtonVariant
import app.openscanner.android.ui.components.OSIconButton
import app.openscanner.android.ui.components.OSTopBar

@Composable
fun CropScreen(
    onBack: () -> Unit,
    onCropped: () -> Unit
) {
    val bitmap = ScanSession.captured
    if (bitmap == null) {
        // Session was cleared (e.g. process death); nothing to edit.
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    val fullFrameQuad = remember(bitmap) {
        val inset = 0.04f
        Quad(
            Point2(bitmap.width * inset, bitmap.height * inset),
            Point2(bitmap.width * (1 - inset), bitmap.height * inset),
            Point2(bitmap.width * (1 - inset), bitmap.height * (1 - inset)),
            Point2(bitmap.width * inset, bitmap.height * (1 - inset))
        )
    }
    var quad by remember(bitmap) {
        mutableStateOf(ScanSession.detectedQuad ?: fullFrameQuad)
    }
    var draggedCorner by remember { mutableStateOf(-1) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            OSTopBar(
                title = "Adjust corners",
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onSizeChanged { viewSize = it }
                    .pointerInput(bitmap) {
                        detectDragGestures(
                            onDragStart = { position ->
                                val touchRadius = 32.dp.toPx()
                                val mapped = quad.corners.map { corner ->
                                    CoordinateMapper.mapPointFit(
                                        corner,
                                        bitmap.width.toFloat(), bitmap.height.toFloat(),
                                        viewSize.width.toFloat(), viewSize.height.toFloat()
                                    )
                                }
                                draggedCorner = mapped.indices.minByOrNull { i ->
                                    val dx = mapped[i].x - position.x
                                    val dy = mapped[i].y - position.y
                                    dx * dx + dy * dy
                                }?.takeIf { i ->
                                    val dx = mapped[i].x - position.x
                                    val dy = mapped[i].y - position.y
                                    dx * dx + dy * dy <= touchRadius * touchRadius
                                } ?: -1
                            },
                            onDrag = { change, _ ->
                                if (draggedCorner < 0) return@detectDragGestures
                                change.consume()
                                val inImage = CoordinateMapper.unmapPointFit(
                                    Point2(change.position.x, change.position.y),
                                    bitmap.width.toFloat(), bitmap.height.toFloat(),
                                    viewSize.width.toFloat(), viewSize.height.toFloat()
                                )
                                val clamped = Point2(
                                    inImage.x.coerceIn(0f, bitmap.width.toFloat()),
                                    inImage.y.coerceIn(0f, bitmap.height.toFloat())
                                )
                                quad = quad.withCorner(draggedCorner, clamped)
                            },
                            onDragEnd = { draggedCorner = -1 },
                            onDragCancel = { draggedCorner = -1 }
                        )
                    }
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Captured page",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                CropOverlay(
                    image = imageBitmap,
                    quad = quad,
                    imageWidth = bitmap.width.toFloat(),
                    imageHeight = bitmap.height.toFloat(),
                    draggedCorner = draggedCorner
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OSButton(
                    label = "Auto",
                    variant = OSButtonVariant.Tonal,
                    onClick = { quad = ScanSession.detectedQuad ?: fullFrameQuad }
                )
                OSButton(
                    label = "Continue",
                    onClick = {
                        ScanSession.cropped = PerspectiveCorrector.crop(bitmap, quad)
                        onCropped()
                    }
                )
            }
        }
    }
}

private fun Quad.withCorner(index: Int, p: Point2): Quad = when (index) {
    0 -> copy(topLeft = p)
    1 -> copy(topRight = p)
    2 -> copy(bottomRight = p)
    3 -> copy(bottomLeft = p)
    else -> this
}

@Composable
private fun CropOverlay(
    image: ImageBitmap,
    quad: Quad,
    imageWidth: Float,
    imageHeight: Float,
    draggedCorner: Int
) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxSize()) {
        val mapped = CoordinateMapper.mapQuadFit(quad, imageWidth, imageHeight, size.width, size.height)

        val path = Path().apply {
            moveTo(mapped.topLeft.x, mapped.topLeft.y)
            lineTo(mapped.topRight.x, mapped.topRight.y)
            lineTo(mapped.bottomRight.x, mapped.bottomRight.y)
            lineTo(mapped.bottomLeft.x, mapped.bottomLeft.y)
            close()
        }
        drawPath(path, color = accent.copy(alpha = 0.10f))
        drawPath(path, color = accent, style = Stroke(width = 2.dp.toPx()))

        mapped.corners.forEachIndexed { i, corner ->
            val center = Offset(corner.x, corner.y)
            drawCircle(Color.White, radius = 10.dp.toPx(), center = center)
            drawCircle(
                accent,
                radius = 10.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            if (i == draggedCorner) {
                drawCircle(accent, radius = 4.dp.toPx(), center = center)
            }
        }

        // Magnifier loupe while dragging: zoomed view of the image around the
        // active corner, placed in whichever top corner is out of the way.
        if (draggedCorner in 0..3) {
            val corner = quad.corners[draggedCorner]
            val loupeRadius = 56.dp.toPx()
            val zoom = 3f
            val onLeftHalf = mapped.corners[draggedCorner].x < size.width / 2f
            val loupeCenter = Offset(
                if (onLeftHalf) size.width - loupeRadius - 12.dp.toPx() else loupeRadius + 12.dp.toPx(),
                loupeRadius + 12.dp.toPx()
            )
            val loupePath = Path().apply {
                addOval(Rect(center = loupeCenter, radius = loupeRadius))
            }
            clipPath(loupePath) {
                drawRect(Color.Black, topLeft = loupeCenter - Offset(loupeRadius, loupeRadius))
                val srcHalf = loupeRadius / zoom
                drawImage(
                    image = image,
                    srcOffset = IntOffset(
                        (corner.x - srcHalf).toInt().coerceIn(0, (imageWidth - 2 * srcHalf).toInt().coerceAtLeast(0)),
                        (corner.y - srcHalf).toInt().coerceIn(0, (imageHeight - 2 * srcHalf).toInt().coerceAtLeast(0))
                    ),
                    srcSize = IntSize((2 * srcHalf).toInt(), (2 * srcHalf).toInt()),
                    dstOffset = IntOffset(
                        (loupeCenter.x - loupeRadius).toInt(),
                        (loupeCenter.y - loupeRadius).toInt()
                    ),
                    dstSize = IntSize((2 * loupeRadius).toInt(), (2 * loupeRadius).toInt())
                )
                // Crosshair marking the exact corner position
                drawCircle(accent, radius = 4.dp.toPx(), center = loupeCenter, style = Stroke(2.dp.toPx()))
            }
            drawCircle(accent, radius = loupeRadius, center = loupeCenter, style = Stroke(width = 2.dp.toPx()))
        }
    }
}
