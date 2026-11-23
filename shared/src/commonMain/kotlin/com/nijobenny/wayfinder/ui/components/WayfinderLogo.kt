package com.nijobenny.wayfinder.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import com.nijobenny.wayfinder.ui.theme.Brand
import kotlin.math.cos
import kotlin.math.sin

/**
 * The app icon drawn in code (same geometry as the launcher icon) so it can be animated:
 * [ringRotation] spins the dashed route ring, [arrowScale] pulses the arrow and
 * [shimmerProgress] sweeps a band of light across the icon.
 */
@Composable
fun WayfinderLogo(
    modifier: Modifier = Modifier,
    ringRotation: Float = 0f,
    arrowScale: Float = 1f,
    shimmerProgress: Float = -1f,
) {
    Canvas(modifier) {
        val k = size.width / 72f
        fun p(x: Float, y: Float) = Offset((x - 18f) * k, (y - 18f) * k)

        val shape = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(20f * k, 20f * k)))
        }
        clipPath(shape) {
            drawRect(Brush.linearGradient(listOf(Brand.Teal, Brand.Sky, Brand.Indigo), Offset.Zero, Offset(size.width, size.height)))
            drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.30f), Color.Transparent), center = p(30f, 22f), radius = 60f * k))

            // Dashed route ring
            val ringTopLeft = p(54f - 27f, 54f - 27f)
            val ringSize = Size(54f * k, 54f * k)
            var angle = -28f
            while (angle < 262f) {
                drawArc(
                    color = Color.White.copy(alpha = 0.5f),
                    startAngle = angle + ringRotation,
                    sweepAngle = 14f,
                    useCenter = false,
                    topLeft = ringTopLeft,
                    size = ringSize,
                    style = Stroke(width = 3f * k, cap = StrokeCap.Round),
                )
                angle += 24f
            }

            // Waypoint dot the arrow is heading to
            val dotAngle = (-50.0) * kotlin.math.PI / 180.0
            drawCircle(Color.White, radius = 5f * k, center = p(54f + 27f * cos(dotAngle).toFloat(), 54f + 27f * sin(dotAngle).toFloat()))

            // Navigation arrow, rotated 40° towards the waypoint
            val rotation = 40.0 * kotlin.math.PI / 180.0
            val points = listOf(0f to -17f, 12f to 15f, 0f to 7.5f, -12f to 15f).map { (x, y) ->
                val sx = x * arrowScale
                val sy = y * arrowScale
                p(
                    54f + (sx * cos(rotation) - sy * sin(rotation)).toFloat(),
                    54f + (sx * sin(rotation) + sy * cos(rotation)).toFloat(),
                )
            }
            val arrow = Path().apply {
                moveTo(points[0].x, points[0].y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            drawPath(arrow, Color.White)
            drawPath(arrow, Color.White, style = Stroke(width = 3f * k, join = androidx.compose.ui.graphics.StrokeJoin.Round))

            // Light sweep
            if (shimmerProgress > -1f) {
                val x = shimmerProgress * size.width
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                        start = Offset(x - size.width * 0.35f, 0f),
                        end = Offset(x, size.height),
                    )
                )
            }
        }
    }
}
