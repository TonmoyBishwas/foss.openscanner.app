package app.openscanner.android.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

// Quiet motion: fades and gentle position eases only. No bounces or springs.
object Motion {
    const val DurationQuick = 150
    const val DurationStandard = 250

    val EaseStandard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EaseDecelerate = CubicBezierEasing(0f, 0f, 0f, 1f)
}
