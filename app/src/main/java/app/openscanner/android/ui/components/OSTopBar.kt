package app.openscanner.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * Top app bar per Calm Material: 64dp tall, blends into the page surface,
 * zero elevation, left-aligned title.
 */
@Composable
fun OSTopBar(
    title: String,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (navigationIcon != null) {
            navigationIcon()
        }
        Text(
            text = title,
            style = titleStyle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
        actions()
    }
}
