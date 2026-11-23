package com.nijobenny.wayfinder.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/*
 * Hand-drawn vector glyphs (no icon library needed), so every icon matches the brand
 * and can be animated.
 */

@Composable
fun SearchGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = Stroke(width = s * 0.11f, cap = StrokeCap.Round)
        drawCircle(color, radius = s * 0.30f, center = Offset(s * 0.42f, s * 0.42f), style = stroke)
        drawLine(color, Offset(s * 0.64f, s * 0.64f), Offset(s * 0.88f, s * 0.88f), strokeWidth = s * 0.11f, cap = StrokeCap.Round)
    }
}

/** Crosshair. When [active] the centre is filled to show "following your location". */
@Composable
fun LocateGlyph(color: Color, active: Boolean, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val c = Offset(s / 2, s / 2)
        val w = s * 0.09f
        drawCircle(color, radius = s * 0.30f, center = c, style = Stroke(width = w))
        listOf(Offset(0f, -1f), Offset(0f, 1f), Offset(-1f, 0f), Offset(1f, 0f)).forEach { d ->
            drawLine(color, Offset(c.x + d.x * s * 0.32f, c.y + d.y * s * 0.32f), Offset(c.x + d.x * s * 0.48f, c.y + d.y * s * 0.48f), strokeWidth = w, cap = StrokeCap.Round)
        }
        if (active) drawCircle(color, radius = s * 0.14f, center = c)
        else drawCircle(color, radius = s * 0.08f, center = c, style = Stroke(width = w * 0.9f))
    }
}

@Composable
fun SunGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val c = Offset(s / 2, s / 2)
        drawCircle(color, radius = s * 0.2f, center = c)
        repeat(8) { i ->
            val a = i * kotlin.math.PI / 4
            val dx = cos(a).toFloat()
            val dy = sin(a).toFloat()
            drawLine(color, Offset(c.x + dx * s * 0.32f, c.y + dy * s * 0.32f), Offset(c.x + dx * s * 0.45f, c.y + dy * s * 0.45f), strokeWidth = s * 0.08f, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun MoonGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val outer = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(s / 2, s / 2), s * 0.36f)) }
        val bite = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(s * 0.68f, s * 0.34f), s * 0.30f)) }
        val crescent = Path().apply { op(outer, bite, PathOperation.Difference) }
        drawPath(crescent, color)
    }
}

/** Half-filled circle: "follow system appearance". */
@Composable
fun AutoThemeGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val c = Offset(s / 2, s / 2)
        drawCircle(color, radius = s * 0.34f, center = c, style = Stroke(width = s * 0.08f))
        drawArc(color, startAngle = 90f, sweepAngle = 180f, useCenter = true, topLeft = Offset(c.x - s * 0.34f, c.y - s * 0.34f), size = androidx.compose.ui.geometry.Size(s * 0.68f, s * 0.68f))
    }
}

/** Map pin. */
@Composable
fun PinGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp, holeColor: Color = Color.Transparent) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val pin = Path().apply {
            moveTo(s * 0.5f, s * 0.95f)
            cubicTo(s * 0.18f, s * 0.6f, s * 0.12f, s * 0.42f, s * 0.12f, s * 0.38f)
            cubicTo(s * 0.12f, s * 0.17f, s * 0.29f, s * 0.02f, s * 0.5f, s * 0.02f)
            cubicTo(s * 0.71f, s * 0.02f, s * 0.88f, s * 0.17f, s * 0.88f, s * 0.38f)
            cubicTo(s * 0.88f, s * 0.42f, s * 0.82f, s * 0.6f, s * 0.5f, s * 0.95f)
            close()
        }
        drawPath(pin, color)
        drawCircle(holeColor.takeIf { it != Color.Transparent } ?: Color.White, radius = s * 0.13f, center = Offset(s * 0.5f, s * 0.38f))
    }
}
