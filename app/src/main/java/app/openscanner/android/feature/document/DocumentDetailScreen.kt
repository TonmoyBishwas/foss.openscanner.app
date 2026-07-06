package app.openscanner.android.feature.document

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.openscanner.android.data.db.PageEntity
import app.openscanner.android.ui.components.OSButton
import app.openscanner.android.ui.components.OSConfirmDialog
import app.openscanner.android.ui.components.OSIconButton
import app.openscanner.android.ui.components.OSRenameDialog
import app.openscanner.android.ui.components.OSTopBar
import app.openscanner.android.ui.theme.ControlShape
import app.openscanner.android.ui.theme.TextSecondaryAlpha

@Composable
fun DocumentDetailScreen(
    onBack: () -> Unit,
    onAddPage: (String) -> Unit,
    viewModel: DocumentDetailViewModel = viewModel()
) {
    val document by viewModel.document.collectAsStateWithLifecycle()
    val pages by viewModel.pages.collectAsStateWithLifecycle()

    var showRename by remember { mutableStateOf(false) }
    var showDeleteDoc by remember { mutableStateOf(false) }
    var pageToDelete by remember { mutableStateOf<PageEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            OSTopBar(
                title = document?.name ?: "",
                navigationIcon = {
                    OSIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack
                    )
                },
                actions = {
                    OSIconButton(
                        icon = Icons.Rounded.DriveFileRenameOutline,
                        contentDescription = "Rename scan",
                        onClick = { showRename = true }
                    )
                    OSIconButton(
                        icon = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete scan",
                        onClick = { showDeleteDoc = true }
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(pages, key = { it.id }) { page ->
                    PageCell(
                        page = page,
                        viewModel = viewModel,
                        onLongClick = { pageToDelete = page }
                    )
                }
            }

            OSButton(
                label = "Add page",
                icon = Icons.Rounded.Add,
                onClick = { onAddPage(viewModel.documentId) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }

    if (showRename) {
        OSRenameDialog(
            title = "Rename scan",
            initialValue = document?.name.orEmpty(),
            onConfirm = {
                viewModel.rename(it)
                showRename = false
            },
            onDismiss = { showRename = false }
        )
    }

    if (showDeleteDoc) {
        OSConfirmDialog(
            title = "Delete scan?",
            body = "All pages will be removed. This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                showDeleteDoc = false
                viewModel.deleteDocument(onDeleted = onBack)
            },
            onDismiss = { showDeleteDoc = false }
        )
    }

    pageToDelete?.let { page ->
        OSConfirmDialog(
            title = "Delete page?",
            body = "Page ${page.position + 1} will be removed. This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.deletePage(page)
                pageToDelete = null
            },
            onDismiss = { pageToDelete = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageCell(
    page: PageEntity,
    viewModel: DocumentDetailViewModel,
    onLongClick: () -> Unit
) {
    val thumbnail by produceState<Bitmap?>(null, page.id) {
        value = viewModel.pageThumbnail(page)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(ControlShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
    ) {
        thumbnail?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Page ${page.position + 1}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            text = "${page.position + 1}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = TextSecondaryAlpha),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    MaterialTheme.shapes.small
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
