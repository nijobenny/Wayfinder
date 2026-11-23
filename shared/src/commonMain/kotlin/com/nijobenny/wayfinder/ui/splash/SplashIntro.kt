package com.nijobenny.wayfinder.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nijobenny.wayfinder.ui.components.WayfinderLogo
import com.nijobenny.wayfinder.ui.theme.Brand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SplashBackground = Color(0xFF070B14)

/**
 * Animated intro shown once per launch, right after the system splash. It starts with the logo
 * exactly where the system splash drew it (128 dp), then: the logo lifts, the dashed route ring
 * spins once, the arrow pulses, a light sweep passes, and the wordmark rises in.
 */
@Composable
fun SplashIntro(onFinished: () -> Unit) {
    val logoSize = remember { Animatable(128f) }
    val logoLift = remember { Animatable(0f) }
    val ring = remember { Animatable(0f) }
    val arrow = remember { Animatable(1f) }
    val glow = remember { Animatable(0f) }
    val shimmer = remember { Animatable(-0.6f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleRise = remember { Animatable(24f) }
    val taglineAlpha = remember { Animatable(0f) }
    val footerAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            delay(120)
            launch { logoSize.animateTo(96f, spring(dampingRatio = 0.6f, stiffness = 180f)) }
            launch { logoLift.animateTo(-60f, spring(dampingRatio = 0.7f, stiffness = 160f)) }
            glow.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
        launch {
            delay(150)
            ring.animateTo(360f, tween(1500, easing = FastOutSlowInEasing))
        }
        launch {
            delay(450)
            arrow.animateTo(1.18f, tween(220, easing = FastOutSlowInEasing))
            arrow.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 300f))
        }
        launch {
            delay(380)
            launch { titleAlpha.animateTo(1f, tween(450)) }
            titleRise.animateTo(0f, tween(550, easing = FastOutSlowInEasing))
        }
        launch {
            delay(560)
            taglineAlpha.animateTo(1f, tween(450))
        }
        launch {
            delay(700)
            shimmer.animateTo(1.6f, tween(900, easing = LinearEasing))
        }
        launch {
            delay(750)
            footerAlpha.animateTo(1f, tween(500))
        }
        delay(1_850)
        onFinished()
    }

    val breathing = rememberInfiniteTransition(label = "breathing")
    val glowScale by breathing.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowScale",
    )

    Box(
        modifier = Modifier.fillMaxSize().background(SplashBackground),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(340.dp)
                .offset(y = logoLift.value.dp)
                .graphicsLayer {
                    alpha = glow.value
                    scaleX = glowScale
                    scaleY = glowScale
                },
        ) {
            drawCircle(
                Brush.radialGradient(
                    listOf(Brand.Sky.copy(alpha = 0.42f), Brand.Teal.copy(alpha = 0.14f), Color.Transparent),
                )
            )
        }

        WayfinderLogo(
            ringRotation = ring.value,
            arrowScale = arrow.value,
            shimmerProgress = shimmer.value,
            modifier = Modifier
                .offset(y = logoLift.value.dp)
                .size(logoSize.value.dp),
        )

        Column(
            modifier = Modifier.offset(y = 62.dp + titleRise.value.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Wayfinder",
                color = Color(0xFFF1F5F9),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                modifier = Modifier.graphicsLayer { alpha = titleAlpha.value },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Find your way, beautifully.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF94A3B8),
                modifier = Modifier.graphicsLayer { alpha = taglineAlpha.value },
            )
        }

        Text(
            "Built with Kotlin Multiplatform",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF94A3B8).copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .graphicsLayer { alpha = footerAlpha.value },
        )
    }
}
