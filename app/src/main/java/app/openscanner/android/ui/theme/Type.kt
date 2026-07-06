package app.openscanner.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.openscanner.android.R

// Two fonts, strict roles: Doto (dot-matrix) for headlines and big numbers only;
// Hanken Grotesk for everything else. Tracking 0 everywhere — dots need air.
val Doto = FontFamily(
    Font(R.font.doto, weight = FontWeight.Bold)
)

val HankenGrotesk = FontFamily(
    Font(R.font.hanken_grotesk, weight = FontWeight.Normal),
    Font(R.font.hanken_grotesk, weight = FontWeight.Medium),
    Font(R.font.hanken_grotesk, weight = FontWeight.SemiBold)
)

val CalmTypography = Typography(
    // Doto — headlines / large numeric readouts only
    headlineLarge = TextStyle(
        fontFamily = Doto,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Doto,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    // Hanken Grotesk — everything else
    titleLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp
    )
)
