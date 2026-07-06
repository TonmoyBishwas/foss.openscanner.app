package app.openscanner.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.openscanner.android.ui.theme.CardShape

/**
 * Calm Material card: 16dp radius, tonal surface (no shadow), 16dp padding.
 */
@Composable
fun OSCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    var m = modifier
        .clip(CardShape)
        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    if (onClick != null) {
        m = m.clickable(onClick = onClick)
    }
    Column(modifier = m.padding(contentPadding), content = content)
}
