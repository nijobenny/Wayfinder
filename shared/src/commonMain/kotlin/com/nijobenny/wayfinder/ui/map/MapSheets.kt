package com.nijobenny.wayfinder.ui.map

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nijobenny.wayfinder.domain.model.Category
import com.nijobenny.wayfinder.domain.model.Place
import com.nijobenny.wayfinder.domain.model.Route
import com.nijobenny.wayfinder.domain.model.SavedPlace
import com.nijobenny.wayfinder.domain.model.TravelMode
import com.nijobenny.wayfinder.domain.model.Units
import com.nijobenny.wayfinder.domain.util.ClockFormat
import com.nijobenny.wayfinder.domain.util.Format
import com.nijobenny.wayfinder.domain.util.GeoMath
import com.nijobenny.wayfinder.platform.currentTimeMillis
import com.nijobenny.wayfinder.ui.components.ChevronGlyph
import com.nijobenny.wayfinder.ui.components.Chip
import com.nijobenny.wayfinder.ui.components.CloseGlyph
import com.nijobenny.wayfinder.ui.components.GradientButton
import com.nijobenny.wayfinder.ui.components.IconBubble
import com.nijobenny.wayfinder.ui.components.ManeuverGlyph
import com.nijobenny.wayfinder.ui.components.PinGlyph
import com.nijobenny.wayfinder.ui.components.PlusGlyph
import com.nijobenny.wayfinder.ui.components.SegmentedSelector
import com.nijobenny.wayfinder.ui.components.SoftButton
import com.nijobenny.wayfinder.ui.components.StarGlyph
import com.nijobenny.wayfinder.ui.components.bounceClick
import com.nijobenny.wayfinder.ui.search.categoryEmoji
import com.nijobenny.wayfinder.ui.theme.Brand
import com.nijobenny.wayfinder.ui.theme.LocalWayfinderColors

// ---------------------------------------------------------------------------------------------
// Explore: where you are + quick categories + saved places
// ---------------------------------------------------------------------------------------------

@Composable
fun ExploreSheet(
    state: MapUiState,
    saved: List<SavedPlace>,
    units: Units,
    onCategory: (Category) -> Unit,
    onSavedPlace: (Place) -> Unit,
) {
    val colors = LocalWayfinderColors.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Brand.Gradient), contentAlignment = Alignment.Center) {
                PinGlyph(Color.White, holeColor = Brand.Sky)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("YOU ARE HERE", style = MaterialTheme.typography.labelMedium, color = Brand.Sky)
                AnimatedContent(
                    targetState = state.myPlace,
                    contentKey = { (it as? PlaceUiState.Ready)?.place?.title ?: it::class.simpleName },
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) },
                    label = "myPlace",
                ) { s ->
                    when (s) {
                        is PlaceUiState.Ready -> Column {
                            Text(s.place.title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (s.place.subtitle.isNotBlank()) Text(s.place.subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        PlaceUiState.Error -> Text("Unknown place", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                        else -> ShimmerLine()
                    }
                }
            }
            LiveBadge()
        }
        Spacer(Modifier.height(16.dp))
        Text("EXPLORE NEARBY", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Category.entries) { c -> Chip("${c.emoji} ${c.label}", selected = false, onClick = { onCategory(c) }) }
        }
        if (saved.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("SAVED", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(saved, key = { it.place.key }) { s ->
                    val distance = state.myLocation?.let { GeoMath.distanceMeters(it.latitude, it.longitude, s.place.latitude, s.place.longitude) }
                    Row(
                        Modifier.clip(RoundedCornerShape(16.dp)).background(colors.surfaceHigh).bounceClick { onSavedPlace(s.place) }.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.emoji, fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(s.label, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(110.dp))
                            if (distance != null) Text(Format.distance(distance, units), style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Place details
// ---------------------------------------------------------------------------------------------

@Composable
fun PlaceSheet(
    place: Place,
    state: MapUiState,
    units: Units,
    isSaved: Boolean,
    canAddStop: Boolean,
    onDirections: () -> Unit,
    onAddStop: () -> Unit,
    onToggleSave: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalWayfinderColors.current
    val distance = state.myLocation?.let { GeoMath.distanceMeters(it.latitude, it.longitude, place.latitude, place.longitude) }
    Column {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Brand.Sky.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Text(categoryEmoji(place.category), fontSize = 24.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(place.title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (place.subtitle.isNotBlank()) Text(place.subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (distance != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("${Format.distance(distance, units)} away", style = MaterialTheme.typography.labelLarge, color = Brand.Teal)
                }
            }
            IconBubble(onClick = onClose, size = 36.dp) { CloseGlyph(colors.textMuted, size = 14.dp) }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            GradientButton(
                text = if (canAddStop) "Add as stop" else "Directions",
                onClick = if (canAddStop) onAddStop else onDirections,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(colors.surfaceHigh).bounceClick(onClick = onToggleSave),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = isSaved,
                    transitionSpec = {
                        (fadeIn(tween(200)) + androidx.compose.animation.scaleIn(spring(dampingRatio = 0.4f, stiffness = 400f), initialScale = 0.3f)) togetherWith fadeOut(tween(120))
                    },
                    label = "save",
                ) { savedNow -> StarGlyph(if (savedNow) Brand.Amber else colors.textMuted, filled = savedNow) }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Route planner: travel mode, multiple stops, summary, steps
// ---------------------------------------------------------------------------------------------

@Composable
fun PlannerSheet(
    state: MapUiState,
    units: Units,
    onMode: (TravelMode) -> Unit,
    onAddStop: () -> Unit,
    onRemoveStop: (Int) -> Unit,
    onMoveStop: (from: Int, to: Int) -> Unit,
    onStart: () -> Unit,
    onDemo: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalWayfinderColors.current
    var showSteps by remember { mutableStateOf(false) }
    Column(Modifier.animateContentSize(spring(dampingRatio = 0.85f, stiffness = 300f))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Plan your trip", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, modifier = Modifier.weight(1f))
            IconBubble(onClick = onClose, size = 36.dp) { CloseGlyph(colors.textMuted, size = 14.dp) }
        }
        Spacer(Modifier.height(12.dp))
        SegmentedSelector(
            options = TravelMode.entries,
            selected = state.mode,
            label = { "${it.emoji} ${it.label}" },
            onSelect = onMode,
        )
        Spacer(Modifier.height(14.dp))

        // Stops timeline
        StopRow(index = -1, title = "Your location", subtitle = null, isLast = false, canMoveUp = false, canMoveDown = false, onRemove = null, onMove = onMoveStop)
        state.stops.forEachIndexed { i, stop ->
            StopRow(
                index = i,
                title = stop.title,
                subtitle = stop.subtitle.takeIf { it.isNotBlank() },
                isLast = i == state.stops.lastIndex,
                canMoveUp = i > 0,
                canMoveDown = i < state.stops.lastIndex,
                onRemove = { onRemoveStop(i) },
                onMove = onMoveStop,
            )
        }
        if (state.stops.size < MapViewModel.MAX_STOPS) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).bounceClick(onClick = onAddStop).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(Brand.Sky.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    PlusGlyph(Brand.Sky, size = 14.dp)
                }
                Spacer(Modifier.width(12.dp))
                Text("Add stop", style = MaterialTheme.typography.labelLarge, color = Brand.Sky)
            }
        }
        Spacer(Modifier.height(12.dp))

        AnimatedContent(
            targetState = state.route,
            contentKey = { it::class.simpleName },
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) },
            label = "route",
        ) { route ->
            when (route) {
                RouteUiState.Idle, RouteUiState.Loading -> RouteLoading()
                is RouteUiState.Error -> Column {
                    Text(route.message, style = MaterialTheme.typography.bodyMedium, color = Brand.Rose)
                    Spacer(Modifier.height(10.dp))
                    SoftButton("Try again", onClick = onRetry, modifier = Modifier.fillMaxWidth())
                }
                is RouteUiState.Ready -> Column {
                    RouteSummary(route.route, units)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GradientButton("Start", onClick = onStart, modifier = Modifier.weight(1f))
                        SoftButton("Demo drive", onClick = onDemo, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).bounceClick { showSteps = !showSteps }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (showSteps) "Hide steps" else "Show ${route.route.maneuvers.size} steps", style = MaterialTheme.typography.labelLarge, color = colors.textMuted)
                        Spacer(Modifier.width(6.dp))
                        ChevronGlyph(colors.textMuted, down = !showSteps, size = 12.dp)
                    }
                    AnimatedVisibility(visible = showSteps, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        LazyColumn(Modifier.heightIn(max = 260.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                            itemsIndexed(route.route.maneuvers) { _, m ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    ManeuverGlyph(m.type, Brand.Sky, size = 26.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(m.instruction, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                                        if (m.distanceMeters > 0) Text(Format.distance(m.distanceMeters, units), style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StopRow(
    index: Int,
    title: String,
    subtitle: String?,
    isLast: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRemove: (() -> Unit)?,
    onMove: (Int, Int) -> Unit,
) {
    val colors = LocalWayfinderColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(
                when {
                    index < 0 -> Brand.Sky
                    isLast -> Brand.Rose
                    else -> Brand.Indigo
                }
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when {
                    index < 0 -> "●"
                    isLast -> "🏁"
                    else -> "${index + 1}"
                },
                color = Color.White,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelMedium, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (canMoveUp) IconBubble(onClick = { onMove(index, index - 1) }, size = 30.dp) { ChevronGlyph(colors.textMuted, size = 12.dp) }
        if (canMoveDown) {
            Spacer(Modifier.width(4.dp))
            IconBubble(onClick = { onMove(index, index + 1) }, size = 30.dp) { ChevronGlyph(colors.textMuted, down = true, size = 12.dp) }
        }
        if (onRemove != null) {
            Spacer(Modifier.width(4.dp))
            IconBubble(onClick = onRemove, size = 30.dp) { CloseGlyph(colors.textMuted, size = 11.dp) }
        }
    }
}

@Composable
private fun RouteSummary(route: Route, units: Units) {
    val colors = LocalWayfinderColors.current
    val arrival = ClockFormat.time(currentTimeMillis() + (route.durationSeconds * 1000).toLong())
    Row(verticalAlignment = Alignment.Bottom) {
        Text(Format.duration(route.durationSeconds), style = MaterialTheme.typography.headlineMedium, color = Brand.Teal)
        Spacer(Modifier.width(10.dp))
        Text(
            "${Format.distance(route.distanceMeters, units)} · arrive $arrival",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textMuted,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
    val badges = buildList {
        if (route.hasToll) add("💰 Tolls")
        if (route.hasHighway) add("🛣️ Highway")
    }
    if (badges.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            badges.forEach {
                Text(it, style = MaterialTheme.typography.labelMedium, color = colors.textMuted, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(colors.surfaceHigh).padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun RouteLoading() {
    val colors = LocalWayfinderColors.current
    val transition = rememberInfiniteTransition(label = "routeLoading")
    val t by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "t")
    Column {
        Text("Finding the best route…", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
        Spacer(Modifier.height(10.dp))
        Canvas(Modifier.fillMaxWidth().height(6.dp)) {
            drawRoundRect(colors.surfaceHigh, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height))
            val w = size.width * 0.35f
            val x = (size.width + w) * t - w
            drawRoundRect(
                Brand.HorizontalGradient,
                topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                size = androidx.compose.ui.geometry.Size(w, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height),
            )
        }
    }
}

@Composable
private fun ShimmerLine() {
    val colors = LocalWayfinderColors.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Box(Modifier.padding(top = 4.dp).fillMaxWidth(0.6f).height(18.dp).graphicsLayer { this.alpha = alpha }.clip(RoundedCornerShape(6.dp)).background(colors.surfaceHigh))
}

@Composable
private fun LiveBadge() {
    val transition = rememberInfiniteTransition(label = "live")
    val pulse by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "p")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(12.dp)) {
            val r = size.minDimension / 2
            drawCircle(Color(0xFF22C55E).copy(alpha = (1f - pulse) * 0.5f), radius = r * (0.5f + pulse * 0.5f))
            drawCircle(Color(0xFF22C55E), radius = r * 0.45f)
        }
        Spacer(Modifier.width(6.dp))
        Text("Live", style = MaterialTheme.typography.labelLarge, color = LocalWayfinderColors.current.textPrimary)
    }
}

@Suppress("unused")
private val PanelEasing = FastOutSlowInEasing
