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

@Composable
fun BackGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.1f
        drawLine(color, Offset(s * 0.82f, s * 0.5f), Offset(s * 0.2f, s * 0.5f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.45f, s * 0.25f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.45f, s * 0.75f), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
fun CloseGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.12f
        drawLine(color, Offset(s * 0.2f, s * 0.2f), Offset(s * 0.8f, s * 0.8f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.8f, s * 0.2f), Offset(s * 0.2f, s * 0.8f), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
fun PlusGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.12f
        drawLine(color, Offset(s * 0.5f, s * 0.15f), Offset(s * 0.5f, s * 0.85f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.15f, s * 0.5f), Offset(s * 0.85f, s * 0.5f), strokeWidth = w, cap = StrokeCap.Round)
    }
}

/** Chevron pointing up (or down when [down]). */
@Composable
fun ChevronGlyph(color: Color, down: Boolean = false, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.13f
        val tipY = if (down) s * 0.68f else s * 0.32f
        val baseY = if (down) s * 0.36f else s * 0.64f
        drawLine(color, Offset(s * 0.2f, baseY), Offset(s * 0.5f, tipY), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.5f, tipY), Offset(s * 0.8f, baseY), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
fun GearGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val c = Offset(s / 2, s / 2)
        repeat(8) { i ->
            val a = i * kotlin.math.PI / 4
            drawLine(color, Offset(c.x + cos(a).toFloat() * s * 0.28f, c.y + sin(a).toFloat() * s * 0.28f), Offset(c.x + cos(a).toFloat() * s * 0.44f, c.y + sin(a).toFloat() * s * 0.44f), strokeWidth = s * 0.14f, cap = StrokeCap.Round)
        }
        drawCircle(color, radius = s * 0.24f, center = c, style = Stroke(width = s * 0.13f))
    }
}

@Composable
fun StarGlyph(color: Color, filled: Boolean, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val c = Offset(s / 2, s * 0.53f)
        val star = Path().apply {
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) s * 0.46f else s * 0.2f
                val a = -kotlin.math.PI / 2 + i * kotlin.math.PI / 5
                val p = Offset(c.x + cos(a).toFloat() * r, c.y + sin(a).toFloat() * r)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        if (filled) drawPath(star, color)
        else drawPath(star, color, style = Stroke(width = s * 0.08f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

/** Turn arrow for a Valhalla maneuver type: straight, left/right (slight/sharp), U-turn or arrive. */
@Composable
fun ManeuverGlyph(type: Int, color: Color, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.11f
        val stroke = Stroke(width = w, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        fun head(tip: Offset, angleDeg: Double) {
            val back = angleDeg * kotlin.math.PI / 180 + kotlin.math.PI
            val l = s * 0.2f
            listOf(back + 0.6, back - 0.6).forEach { a ->
                drawLine(color, tip, Offset(tip.x + cos(a).toFloat() * l, tip.y + sin(a).toFloat() * l), strokeWidth = w, cap = StrokeCap.Round)
            }
        }
        when (type) {
            4, 5, 6 -> { // arrive: flag-like pin
                drawCircle(color, radius = s * 0.18f, center = Offset(s * 0.5f, s * 0.38f), style = stroke)
                drawLine(color, Offset(s * 0.5f, s * 0.56f), Offset(s * 0.5f, s * 0.88f), strokeWidth = w, cap = StrokeCap.Round)
            }
            9, 10, 11, 18, 20 -> { // right turns
                val p = Path().apply { moveTo(s * 0.3f, s * 0.9f); lineTo(s * 0.3f, s * 0.5f); quadraticTo(s * 0.3f, s * 0.3f, s * 0.5f, s * 0.3f); lineTo(s * 0.8f, s * 0.3f) }
                drawPath(p, color, style = stroke)
                head(Offset(s * 0.8f, s * 0.3f), 0.0)
            }
            14, 15, 16, 19, 21 -> { // left turns
                val p = Path().apply { moveTo(s * 0.7f, s * 0.9f); lineTo(s * 0.7f, s * 0.5f); quadraticTo(s * 0.7f, s * 0.3f, s * 0.5f, s * 0.3f); lineTo(s * 0.2f, s * 0.3f) }
                drawPath(p, color, style = stroke)
                head(Offset(s * 0.2f, s * 0.3f), 180.0)
            }
            12, 13 -> { // U-turn
                val p = Path().apply { moveTo(s * 0.7f, s * 0.9f); lineTo(s * 0.7f, s * 0.4f); quadraticTo(s * 0.7f, s * 0.15f, s * 0.48f, s * 0.15f); quadraticTo(s * 0.26f, s * 0.15f, s * 0.26f, s * 0.4f); lineTo(s * 0.26f, s * 0.7f) }
                drawPath(p, color, style = stroke)
                head(Offset(s * 0.26f, s * 0.72f), 90.0)
            }
            else -> { // straight / continue / start
                drawLine(color, Offset(s * 0.5f, s * 0.9f), Offset(s * 0.5f, s * 0.15f), strokeWidth = w, cap = StrokeCap.Round)
                head(Offset(s * 0.5f, s * 0.15f), -90.0)
            }
        }
    }
}
