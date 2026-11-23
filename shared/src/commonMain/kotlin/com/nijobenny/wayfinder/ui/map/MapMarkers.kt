package com.nijobenny.wayfinder.ui.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.nijobenny.wayfinder.ui.theme.Brand

/**
 * A pin that drops in with a bounce, with a pulsing halo at its base. Drawn in Compose and
 * placed on the map at a coordinate, so it can be animated like any other UI.
 */
@Composable
fun DropPin(label: String?, modifier: Modifier = Modifier, color: Color = Brand.Sky) {
    val drop = remember { Animatable(-80f) }
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { drop.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
            launch { scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f)) }
        }
    }
    val halo = rememberInfiniteTransition(label = "halo")
    val pulse by halo.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "pulse")

    Box(modifier = modifier.size(64.dp, 76.dp), contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.size(64.dp, 24.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            drawOval(color.copy(alpha = (1f - pulse) * 0.45f), topLeft = Offset(c.x - 30f * pulse * density / 2, c.y - 10f * pulse * density / 2), size = androidx.compose.ui.geometry.Size(30f * pulse * density, 10f * pulse * density))
            drawOval(Color.Black.copy(alpha = 0.25f), topLeft = Offset(c.x - 6.dp.toPx(), c.y - 2.dp.toPx()), size = androidx.compose.ui.geometry.Size(12.dp.toPx(), 4.dp.toPx()))
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                translationY = drop.value.dp.toPx() - 10.dp.toPx()
                scaleX = scale.value
                scaleY = scale.value
                transformOrigin = TransformOrigin(0.5f, 1f)
            },
        ) {
            Box(
                Modifier.size(40.dp).shadow(10.dp, CircleShape, spotColor = color).clip(CircleShape).background(color).border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (label != null) Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                else Box(Modifier.size(12.dp).clip(CircleShape).background(Color.White))
            }
            Canvas(Modifier.size(14.dp, 10.dp)) {
                val tail = Path().apply {
                    moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height); close()
                }
                drawPath(tail, color)
            }
        }
    }
}

/** Small numbered stop marker used along a multi-stop route. */
@Composable
fun StopMarker(number: Int, isFinal: Boolean, modifier: Modifier = Modifier) {
    DropPin(
        label = if (isFinal) "🏁" else number.toString(),
        modifier = modifier,
        color = if (isFinal) Brand.Rose else Brand.Indigo,
    )
}

@Composable
fun MarkerLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White)
}
