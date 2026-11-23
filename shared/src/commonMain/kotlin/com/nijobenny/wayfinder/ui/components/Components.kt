package com.nijobenny.wayfinder.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nijobenny.wayfinder.ui.theme.LocalWayfinderColors

/** Springy press-in effect used on every tappable element. */
fun Modifier.bounceClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "bounce",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
}

/** Frosted "glass" surface that floats over the map, adapting to light and dark themes. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 18.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalWayfinderColors.current
    Box(
        modifier = modifier
            .shadow(elevation, shape, ambientColor = colors.shadow.copy(alpha = 0.25f), spotColor = colors.shadow.copy(alpha = 0.35f))
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, colors.glassBorder, shape),
        content = content,
    )
}

/** Round floating map button. */
@Composable
fun MapButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    GlassPanel(
        modifier = modifier.size(52.dp).bounceClick(onClick = onClick),
        shape = CircleShape,
        elevation = 14.dp,
    ) {
        Box(Modifier.align(Alignment.Center), contentAlignment = Alignment.Center, content = content)
    }
}
