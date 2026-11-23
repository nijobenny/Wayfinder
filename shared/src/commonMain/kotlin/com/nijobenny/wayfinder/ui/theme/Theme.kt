package com.nijobenny.wayfinder.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Brand accents shared by both themes: teal → sky → indigo. */
object Brand {
    val Teal = Color(0xFF14B8A6)
    val Sky = Color(0xFF0EA5E9)
    val Indigo = Color(0xFF4F46E5)
    val Rose = Color(0xFFF43F5E)
    val Amber = Color(0xFFF59E0B)
    val Gradient = Brush.linearGradient(listOf(Teal, Sky, Indigo))
    val HorizontalGradient = Brush.horizontalGradient(listOf(Teal, Sky, Indigo))
}

@Immutable
data class WayfinderColors(
    val isDark: Boolean,
    val background: Color,
    val glass: Color,
    val glassBorder: Color,
    val surfaceHigh: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val shadow: Color,
    val mapStyleUri: String,
)

val DarkColors = WayfinderColors(
    isDark = true,
    background = Color(0xFF070B14),
    glass = Color(0xE6101726),
    glassBorder = Color(0x1FFFFFFF),
    surfaceHigh = Color(0xFF1B2436),
    textPrimary = Color(0xFFF1F5F9),
    textMuted = Color(0xFF94A3B8),
    shadow = Color(0xFF000000),
    mapStyleUri = "https://tiles.openfreemap.org/styles/dark",
)

val LightColors = WayfinderColors(
    isDark = false,
    background = Color(0xFFF4F7FB),
    glass = Color(0xEBFFFFFF),
    glassBorder = Color(0x140F172A),
    surfaceHigh = Color(0xFFEDF2F8),
    textPrimary = Color(0xFF0F172A),
    textMuted = Color(0xFF64748B),
    shadow = Color(0xFF0F172A),
    mapStyleUri = "https://tiles.openfreemap.org/styles/positron",
)

val LocalWayfinderColors = staticCompositionLocalOf { DarkColors }

private val WayfinderTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.3.sp),
)

/** Every colour animates when the theme changes, so switching light/dark is a smooth cross-fade. */
@Composable
fun WayfinderTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val target = if (darkTheme) DarkColors else LightColors
    val spec = tween<Color>(durationMillis = 450)
    val background by animateColorAsState(target.background, spec, label = "bg")
    val glass by animateColorAsState(target.glass, spec, label = "glass")
    val glassBorder by animateColorAsState(target.glassBorder, spec, label = "border")
    val surfaceHigh by animateColorAsState(target.surfaceHigh, spec, label = "surfaceHigh")
    val textPrimary by animateColorAsState(target.textPrimary, spec, label = "text")
    val textMuted by animateColorAsState(target.textMuted, spec, label = "muted")

    val colors = target.copy(
        background = background,
        glass = glass,
        glassBorder = glassBorder,
        surfaceHigh = surfaceHigh,
        textPrimary = textPrimary,
        textMuted = textMuted,
    )
    val scheme = if (darkTheme) {
        darkColorScheme(primary = Brand.Sky, secondary = Brand.Teal, background = background, surface = glass, onSurface = textPrimary, onBackground = textPrimary)
    } else {
        lightColorScheme(primary = Brand.Sky, secondary = Brand.Teal, background = background, surface = glass, onSurface = textPrimary, onBackground = textPrimary)
    }

    CompositionLocalProvider(LocalWayfinderColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = WayfinderTypography, content = content)
    }
}
