package com.nijobenny.wayfinder.ui.map

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.nijobenny.wayfinder.domain.model.Units
import com.nijobenny.wayfinder.domain.util.ClockFormat
import com.nijobenny.wayfinder.domain.util.Format
import com.nijobenny.wayfinder.platform.currentTimeMillis
import com.nijobenny.wayfinder.ui.components.GlassPanel
import com.nijobenny.wayfinder.ui.components.GradientButton
import com.nijobenny.wayfinder.ui.components.ManeuverGlyph
import com.nijobenny.wayfinder.ui.components.SoftButton
import com.nijobenny.wayfinder.ui.theme.Brand
import com.nijobenny.wayfinder.ui.theme.LocalWayfinderColors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Big turn instruction at the top of the screen; slides when the next maneuver changes. */
@Composable
fun NavigationBanner(nav: NavigationState, units: Units, modifier: Modifier = Modifier) {
    val maneuver = nav.progress.nextManeuver
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brand.Gradient)
            .padding(18.dp),
    ) {
        AnimatedContent(
            targetState = maneuver,
            transitionSpec = {
                (slideInHorizontally(tween(350)) { it / 3 } + fadeIn(tween(350))) togetherWith
                    (slideOutHorizontally(tween(250)) { -it / 3 } + fadeOut(tween(200)))
            },
            label = "maneuver",
        ) { m ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) { ManeuverGlyph(m?.type ?: 4, Color.White) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        Format.distance(nav.progress.distanceToNextManeuver, units),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        m?.instruction ?: "Arrive at ${nav.destinationTitle}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Bottom navigation card: time left, distance, arrival clock and a progress bar. */
@Composable
fun NavigationPanel(nav: NavigationState, units: Units, onEnd: () -> Unit) {
    val colors = LocalWayfinderColors.current
    val progress by animateFloatAsState(nav.progress.fraction, tween(300), label = "progress")
    val arrival = ClockFormat.time(currentTimeMillis() + (nav.progress.remainingSeconds * 1000).toLong())
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Format.duration(nav.progress.remainingSeconds), style = MaterialTheme.typography.headlineMedium, color = Brand.Teal)
                Text(
                    "${Format.distance(nav.progress.remainingMeters, units)} · arrive $arrival",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
            }
            if (nav.simulated) {
                Text(
                    "DEMO",
                    style = MaterialTheme.typography.labelMedium,
                    color = Brand.Amber,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Brand.Amber.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Spacer(Modifier.width(10.dp))
            }
            SoftButton("End", onClick = onEnd, modifier = Modifier.width(90.dp))
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(colors.surfaceHigh)) {
            Box(Modifier.fillMaxWidth(progress.coerceIn(0.02f, 1f)).height(6.dp).clip(RoundedCornerShape(50)).background(Brand.HorizontalGradient))
        }
    }
}

/** Celebration on arrival: a burst of confetti behind a card. */
@Composable
fun ArrivalCelebration(title: String, onDismiss: () -> Unit) {
    val colors = LocalWayfinderColors.current
    val burst = remember { Animatable(0f) }
    val card = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { burst.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }
            launch { card.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 250f)) }
        }
    }
    val particles = remember {
        val palette = listOf(Brand.Teal, Brand.Sky, Brand.Indigo, Brand.Amber, Brand.Rose, Color(0xFF22C55E))
        List(48) { Triple(Random.nextDouble(0.0, 2 * kotlin.math.PI), Random.nextFloat() * 0.6f + 0.4f, palette[it % palette.size]) }
    }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f * card.value)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxR = size.minDimension * 0.6f
            particles.forEach { (angle, speed, color) ->
                val r = maxR * speed * burst.value
                val p = Offset(center.x + cos(angle).toFloat() * r, center.y + sin(angle).toFloat() * r + 120f * burst.value * burst.value)
                drawCircle(color.copy(alpha = 1f - burst.value * 0.8f), radius = 7f * (1f - burst.value * 0.5f), center = p)
            }
        }
        GlassPanel(
            modifier = Modifier
                .padding(32.dp)
                .graphicsLayer {
                    scaleX = 0.6f + 0.4f * card.value
                    scaleY = 0.6f + 0.4f * card.value
                    alpha = card.value.coerceIn(0f, 1f)
                },
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🎉", fontSize = 52.sp)
                Text("You've arrived!", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.textMuted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                GradientButton("Done", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
