package app.openscanner.android.feature.library

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.openscanner.android.data.db.DocumentSummary
import app.openscanner.android.ui.components.OSButton
import app.openscanner.android.ui.components.OSConfirmDialog
import app.openscanner.android.ui.components.OSRenameDialog
import app.openscanner.android.ui.components.OSTopBar
import app.openscanner.android.ui.theme.ControlShape
import app.openscanner.android.ui.theme.Doto
import app.openscanner.android.ui.theme.TextSecondaryAlpha
import app.openscanner.android.ui.theme.TextTertiaryAlpha
import java.text.DateFormat
import java.util.Date

@Composable
fun LibraryScreen(
    onScanClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    var renameTarget by remember { mutableStateOf<DocumentSummary?>(null) }
    var deleteTarget by remember { mutableStateOf<DocumentSummary?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            OSTopBar(
                title = "OpenScanner",
                titleStyle = MaterialTheme.typography.headlineMedium.copy(fontFamily = Doto)
            )
        }
    ) { innerPadding ->
        val docs = documents
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                docs == null -> Unit // first load; keep the surface calm
                docs.isEmpty() && query.isBlank() -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyLibrary(onScanClick = onScanClick)
                }
                else -> Column(Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::setQuery,
                        placeholder = { Text("Search scans") },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = TextTertiaryAlpha)
                            )
                        },
                        singleLine = true,
                        shape = ControlShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(docs, key = { it.document.id }) { summary ->
                            DocumentRow(
                                summary = summary,
                                viewModel = viewModel,
                                onClick = { onDocumentClick(summary.document.id) },
                                onLongClick = { renameTarget = summary }
                            )
                        }
                    }
                }
            }

            if (docs?.isNotEmpty() == true || query.isNotBlank()) {
                OSButton(
                    label = "Scan",
                    icon = Icons.Rounded.PhotoCamera,
                    onClick = onScanClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }

    renameTarget?.let { target ->
        DocumentActionsDialog(
            summary = target,
            onRename = { name ->
                viewModel.rename(target.document.id, name)
                renameTarget = null
            },
            onDelete = {
                renameTarget = null
                deleteTarget = target
            },
            onDismiss = { renameTarget = null }
        )
    }

    deleteTarget?.let { target ->
        OSConfirmDialog(
            title = "Delete scan?",
            body = "\"${target.document.name}\" and its ${target.pageCount} " +
                (if (target.pageCount == 1) "page" else "pages") +
                " will be removed. This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.delete(target.document.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun DocumentActionsDialog(
    summary: DocumentSummary,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var showRename by remember { mutableStateOf(false) }
    if (showRename) {
        OSRenameDialog(
            title = "Rename scan",
            initialValue = summary.document.name,
            onConfirm = onRename,
            onDismiss = onDismiss
        )
    } else {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    summary.document.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showRename = true }) {
                    Text("Rename", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentRow(
    summary: DocumentSummary,
    viewModel: LibraryViewModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val thumbnail by produceState<Bitmap?>(null, summary.firstPageFileName) {
        value = viewModel.thumbnail(summary)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(ControlShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 56.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            thumbnail?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } ?: Icon(
                Icons.Rounded.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = TextTertiaryAlpha)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = summary.document.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${summary.pageCount} " +
                    (if (summary.pageCount == 1) "page" else "pages") +
                    " · " +
                    DateFormat.getDateInstance(DateFormat.MEDIUM)
                        .format(Date(summary.document.modifiedAt)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = TextSecondaryAlpha)
            )
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
