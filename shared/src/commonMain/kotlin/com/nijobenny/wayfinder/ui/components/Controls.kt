package com.nijobenny.wayfinder.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nijobenny.wayfinder.ui.theme.Brand
import com.nijobenny.wayfinder.ui.theme.LocalWayfinderColors

@Composable
fun GradientButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null) {
    Box(
        modifier = modifier
            .height(54.dp)
            .shadow(16.dp, RoundedCornerShape(18.dp), ambientColor = Brand.Sky, spotColor = Brand.Sky)
            .clip(RoundedCornerShape(18.dp))
            .background(Brand.HorizontalGradient)
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
    }
}

/** Secondary action on a soft tinted surface. */
@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null) {
    val colors = LocalWayfinderColors.current
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceHigh)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        }
    }
}

/** Pill chip that lights up when [selected]. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalWayfinderColors.current
    val background by animateColorAsState(if (selected) Brand.Sky.copy(alpha = 0.18f) else colors.surfaceHigh, tween(250), label = "chipBg")
    val border by animateColorAsState(if (selected) Brand.Sky else Color.Transparent, tween(250), label = "chipBorder")
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) colors.textPrimary else colors.textMuted,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .border(1.5.dp, border, RoundedCornerShape(50))
            .bounceClick(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

/** Segmented control with a gradient indicator that slides between options. */
@Composable
fun <T> SegmentedSelector(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWayfinderColors.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceHigh)
            .padding(4.dp),
    ) {
        val segmentWidth = maxWidth / options.size
        val index = options.indexOf(selected).coerceAtLeast(0)
        val offset by animateDpAsState(segmentWidth * index, spring(dampingRatio = 0.75f, stiffness = 400f), label = "segment")
        Box(
            Modifier
                .offset(x = offset)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(6.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Brand.HorizontalGradient)
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEach { option ->
                val isSelected = option == selected
                val textColor by animateColorAsState(if (isSelected) Color.White else colors.textMuted, tween(200), label = "segText")
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().bounceClick { onSelect(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label(option), style = MaterialTheme.typography.labelLarge, color = textColor, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
    }
}

/** iOS-style switch with a springy thumb and gradient track. */
@Composable
fun ToggleSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalWayfinderColors.current
    val thumbOffset by animateDpAsState(if (checked) 22.dp else 2.dp, spring(dampingRatio = 0.6f, stiffness = 500f), label = "thumb")
    val track by animateColorAsState(if (checked) Brand.Sky else colors.surfaceHigh, tween(200), label = "track")
    Box(
        modifier = Modifier
            .size(width = 50.dp, height = 30.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
            .bounceClick { onCheckedChange(!checked) },
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset, y = 2.dp)
                .size(26.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/** Round icon button on a soft surface. */
@Composable
fun IconBubble(onClick: () -> Unit, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 42.dp, content: @Composable () -> Unit) {
    val colors = LocalWayfinderColors.current
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(colors.surfaceHigh).bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun RowScope.StatTile(value: String, label: String) {
    val colors = LocalWayfinderColors.current
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceHigh)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
        }
    }
}
