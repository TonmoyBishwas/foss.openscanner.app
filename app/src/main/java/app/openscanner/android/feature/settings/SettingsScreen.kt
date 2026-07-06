package app.openscanner.android.feature.settings

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FilterBAndW
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.openscanner.android.BuildConfig
import app.openscanner.android.OpenScannerApplication
import app.openscanner.android.core.vision.ScanFilter
import app.openscanner.android.data.ThemeMode
import app.openscanner.android.ui.components.OSCard
import app.openscanner.android.ui.components.OSIconButton
import app.openscanner.android.ui.components.OSTopBar
import app.openscanner.android.ui.theme.TextSecondaryAlpha

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = (application as OpenScannerApplication).container.settingsRepository
    val theme = settings.theme
    val defaultFilter = settings.defaultFilter
    fun setTheme(mode: ThemeMode) = settings.setTheme(mode)
    fun setDefaultFilter(filter: ScanFilter) = settings.setDefaultFilter(filter)
}

private const val SOURCE_URL = "https://github.com/TonmoyBishwas/foss.openscanner.app"
private const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val defaultFilter by viewModel.defaultFilter.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            OSTopBar(
                title = "Settings",
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SectionTitle("Appearance")
            OSCard(contentPadding = 8.dp, modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEach { mode ->
                    SelectableTile(
                        icon = Icons.Rounded.DarkMode,
                        title = mode.label,
                        selected = theme == mode,
                        onClick = { viewModel.setTheme(mode) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("Scanning")
            OSCard(contentPadding = 8.dp, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Default filter",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = TextSecondaryAlpha),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                ScanFilter.entries.forEach { filter ->
                    SelectableTile(
                        icon = Icons.Rounded.FilterBAndW,
                        title = filter.label,
                        selected = defaultFilter == filter,
                        onClick = { viewModel.setDefaultFilter(filter) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("About")
            OSCard(contentPadding = 8.dp, modifier = Modifier.fillMaxWidth()) {
                InfoTile(
                    icon = Icons.Rounded.Info,
                    title = "Version",
                    subtitle = BuildConfig.VERSION_NAME
                )
                InfoTile(
                    icon = Icons.Rounded.Code,
                    title = "Source code",
                    subtitle = "github.com/TonmoyBishwas/foss.openscanner.app",
                    trailing = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, SOURCE_URL.toUri()))
                    }
                )
                InfoTile(
                    icon = Icons.Rounded.Gavel,
                    title = "License",
                    subtitle = "GPL-3.0 — free as in freedom",
                    trailing = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, LICENSE_URL.toUri()))
                    }
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "OpenScanner never connects to the internet. " +
                    "Your documents stay on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = TextSecondaryAlpha),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SelectableTile(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (selected) Icons.Rounded.RadioButtonChecked
            else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun InfoTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = TextSecondaryAlpha)
            )
        }
        if (trailing != null) {
            Icon(
                imageVector = trailing,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = TextSecondaryAlpha),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
