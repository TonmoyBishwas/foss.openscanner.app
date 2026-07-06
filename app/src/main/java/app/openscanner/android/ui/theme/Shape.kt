package app.openscanner.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// 12dp controls, 16dp cards/sheets/dialogs, pill for toggles/badges. Never 0.
val ControlShape = RoundedCornerShape(12.dp)
val CardShape = RoundedCornerShape(16.dp)
val PillShape = RoundedCornerShape(999.dp)

val CalmShapes = Shapes(
    extraSmall = ControlShape,
    small = ControlShape,
    medium = ControlShape,
    large = CardShape,
    extraLarge = CardShape
)
