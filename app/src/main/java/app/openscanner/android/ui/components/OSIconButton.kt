package app.openscanner.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.openscanner.android.ui.theme.PillShape

enum class OSIconButtonVariant { Standard, Filled, Tonal }

/**
 * Round icon button, 44dp tap target per Calm Material.
 */
@Composable
fun OSIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: OSIconButtonVariant = OSIconButtonVariant.Standard,
    tint: Color = Color.Unspecified,
    enabled: Boolean = true
) {
    val background = when (variant) {
        OSIconButtonVariant.Standard -> Color.Transparent
        OSIconButtonVariant.Filled -> MaterialTheme.colorScheme.primary
        OSIconButtonVariant.Tonal -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (tint != Color.Unspecified) tint else when (variant) {
        OSIconButtonVariant.Standard -> MaterialTheme.colorScheme.onSurface
        OSIconButtonVariant.Filled -> MaterialTheme.colorScheme.onPrimary
        OSIconButtonVariant.Tonal -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(PillShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) contentColor else contentColor.copy(alpha = 0.38f),
            modifier = Modifier.size(24.dp)
        )
    }
}
