package app.openscanner.android.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.openscanner.android.ui.theme.ControlShape

enum class OSButtonVariant { Filled, Tonal, Text }

/**
 * Calm Material button: flat, 12dp radius, 24x16 padding, no elevation,
 * no shrink on press.
 */
@Composable
fun OSButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: OSButtonVariant = OSButtonVariant.Filled,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val colors = when (variant) {
        OSButtonVariant.Filled -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
        OSButtonVariant.Tonal -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        OSButtonVariant.Text -> ButtonDefaults.buttonColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ControlShape,
        colors = colors,
        elevation = null,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
