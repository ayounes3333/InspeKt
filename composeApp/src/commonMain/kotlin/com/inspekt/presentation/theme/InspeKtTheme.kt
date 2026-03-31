package com.inspekt.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── Catppuccin Mocha-inspired palette ────────────────────────────────────────
object InspeKtColors {
    // Base
    val Base = Color(0xFF1E1E2E)
    val Mantle = Color(0xFF181825)
    val Crust = Color(0xFF11111B)

    // Surface layers
    val Surface0 = Color(0xFF313244)
    val Surface1 = Color(0xFF45475A)
    val Surface2 = Color(0xFF585B70)

    // Overlays (muted)
    val Overlay0 = Color(0xFF6C7086)
    val Overlay1 = Color(0xFF7F849C)

    // Text
    val Text = Color(0xFFCDD6F4)
    val Subtext1 = Color(0xFFBAC2DE)
    val Subtext0 = Color(0xFFA6ADC8)

    // Accent palette
    val Blue = Color(0xFF89B4FA)
    val Green = Color(0xFFA6E3A1)
    val Yellow = Color(0xFFF9E2AF)
    val Peach = Color(0xFFFAB387)
    val Red = Color(0xFFF38BA8)
    val Mauve = Color(0xFFCBA6F7)
    val Teal = Color(0xFF94E2D5)
    val Sky = Color(0xFF89DCFE)
    val Lavender = Color(0xFFB4BEFE)
}

// ── Material 3 dark scheme ───────────────────────────────────────────────────

private val InspeKtDarkScheme = darkColorScheme(
    primary = InspeKtColors.Blue,
    onPrimary = InspeKtColors.Crust,
    primaryContainer = InspeKtColors.Blue.copy(alpha = 0.12f),
    onPrimaryContainer = InspeKtColors.Blue,

    secondary = InspeKtColors.Green,
    onSecondary = InspeKtColors.Crust,
    secondaryContainer = InspeKtColors.Green.copy(alpha = 0.12f),
    onSecondaryContainer = InspeKtColors.Green,

    tertiary = InspeKtColors.Mauve,
    onTertiary = InspeKtColors.Crust,
    tertiaryContainer = InspeKtColors.Mauve.copy(alpha = 0.12f),
    onTertiaryContainer = InspeKtColors.Mauve,

    error = InspeKtColors.Red,
    onError = InspeKtColors.Crust,
    errorContainer = InspeKtColors.Red.copy(alpha = 0.12f),
    onErrorContainer = InspeKtColors.Red,

    background = InspeKtColors.Base,
    onBackground = InspeKtColors.Text,
    surface = InspeKtColors.Base,
    onSurface = InspeKtColors.Text,
    surfaceVariant = InspeKtColors.Surface0,
    onSurfaceVariant = InspeKtColors.Subtext1,

    outline = InspeKtColors.Surface2,
    outlineVariant = InspeKtColors.Surface1,

    inverseSurface = InspeKtColors.Text,
    inverseOnSurface = InspeKtColors.Base,
    inversePrimary = InspeKtColors.Blue,
    surfaceTint = InspeKtColors.Blue,
    scrim = Color.Black,
)

private val InspeKtShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun InspeKtTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = InspeKtDarkScheme,
        shapes = InspeKtShapes,
        content = content,
    )
}

// ── Semantic color helpers ───────────────────────────────────────────────────

fun methodColor(method: String): Color = when (method.uppercase()) {
    "GET"     -> InspeKtColors.Green
    "POST"    -> InspeKtColors.Yellow
    "PUT"     -> InspeKtColors.Blue
    "PATCH"   -> InspeKtColors.Mauve
    "DELETE"  -> InspeKtColors.Red
    "HEAD"    -> InspeKtColors.Teal
    "OPTIONS" -> InspeKtColors.Overlay1
    else      -> InspeKtColors.Overlay1
}

fun statusColor(code: Int): Color = when (code) {
    in 200..299 -> InspeKtColors.Green
    in 300..399 -> InspeKtColors.Blue
    in 400..499 -> InspeKtColors.Yellow
    in 500..599 -> InspeKtColors.Red
    else        -> InspeKtColors.Overlay1
}

