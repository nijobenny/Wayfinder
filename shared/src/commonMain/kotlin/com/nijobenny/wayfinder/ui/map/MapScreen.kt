package com.nijobenny.wayfinder.ui.map

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nijobenny.wayfinder.domain.model.ThemeMode
import com.nijobenny.wayfinder.domain.util.GeoMath
import com.nijobenny.wayfinder.ui.components.AutoThemeGlyph
import com.nijobenny.wayfinder.ui.components.GlassPanel
import com.nijobenny.wayfinder.ui.components.LocateGlyph
import com.nijobenny.wayfinder.ui.components.MapButton
import com.nijobenny.wayfinder.ui.components.MoonGlyph
import com.nijobenny.wayfinder.ui.components.PinGlyph
import com.nijobenny.wayfinder.ui.components.SearchGlyph
import com.nijobenny.wayfinder.ui.components.SunGlyph
import com.nijobenny.wayfinder.ui.components.bounceClick
import com.nijobenny.wayfinder.ui.theme.Brand
import com.nijobenny.wayfinder.ui.theme.LocalWayfinderColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.LocationIndicatorLayer
import org.maplibre.compose.location.LocationPermission
import org.maplibre.compose.location.LocationState
import org.maplibre.compose.location.LocationTrackingEffect
import org.maplibre.compose.location.rememberDefaultHeadingProvider
import org.maplibre.compose.location.rememberDefaultLocationProvider
import org.maplibre.compose.location.rememberLocationState
import org.maplibre.compose.location.rememberSystemSettingsLauncher
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.overlay.MapOverlay
import org.maplibre.compose.overlay.include
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Kochi, Kerala: where the camera starts before the first location fix. */
private val StartPosition = Position(latitude = 9.9816, longitude = 76.2999)

@Composable
fun MapScreen(
    placeState: PlaceUiState,
    themeMode: ThemeMode,
    onCycleTheme: () -> Unit,
    onLocation: (latitude: Double, longitude: Double) -> Unit,
) {
    val colors = LocalWayfinderColors.current
    val density = LocalDensity.current

    val locationState = rememberLocationState(
        provider = rememberDefaultLocationProvider(),
        headingProvider = rememberDefaultHeadingProvider(),
    )
    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(colors.mapStyleUri),
        initialCameraPosition = CameraPosition(center = StartPosition, zoom = 11.5),
    ) {
        LocationIndicatorLayer(
            id = "user",
            locationState = locationState,
            bearingAccuracyColor = const(Brand.Sky.copy(alpha = 0.30f)),
            accuracyRadiusColor = Brand.Sky.copy(alpha = 0.14f),
            accuracyRadiusBorderColor = Brand.Sky.copy(alpha = 0.6f),
        )
    }

    // Follow the user until they pan the map themselves.
    var following by remember { mutableStateOf(true) }
    LaunchedEffect(mapState) {
        snapshotFlow { mapState.cameraMoveReason }.collect { reason ->
            if (reason == CameraMoveReason.Gesture) following = false
        }
    }
    LocationTrackingEffect(locationState = locationState, enabled = following, trackBearing = false) {
        mapState.animateCamera(
            update = CameraUpdate(center = currentLocation.position, zoom = maxOf(mapState.cameraPosition.zoom, 15.0)),
            animation = CameraAnimation.Ease { duration = 900.milliseconds },
        )
    }

    // Look up the place name as the user moves.
    val lastLocation = locationState.lastLocation
    LaunchedEffect(lastLocation) {
        lastLocation?.let { onLocation(it.position.latitude, it.position.longitude) }
    }

    // Briefly show which theme was picked.
    var themeToastKey by remember { mutableIntStateOf(0) }
    var showThemeToast by remember { mutableStateOf(false) }
    LaunchedEffect(themeToastKey) {
        if (themeToastKey > 0) {
            showThemeToast = true
            delay(1_400)
            showThemeToast = false
        }
    }

    val scope = rememberCoroutineScope()
    var panelHeightPx by remember { mutableIntStateOf(0) }
    val panelHeight = with(density) { panelHeightPx.toDp() }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            state = mapState,
            viewportInsets = PaddingValues(bottom = panelHeight + 16.dp),
            overlay = { include(MapOverlay.AttributionOnly) },
        )

        // Soft scrim so the status bar stays readable over any map content.
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Brush.verticalGradient(listOf(colors.background.copy(alpha = 0.75f), Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SearchBar()
            Spacer(Modifier.height(10.dp))
            AnimatedVisibility(
                visible = showThemeToast,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 },
            ) {
                GlassPanel(shape = RoundedCornerShape(50), elevation = 8.dp) {
                    Text(
                        when (themeMode) {
                            ThemeMode.System -> "Following system theme"
                            ThemeMode.Light -> "Light mode"
                            ThemeMode.Dark -> "Dark mode"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }

        // Floating controls, kept just above the bottom panel.
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = panelHeight + 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MapButton(onClick = {
                onCycleTheme()
                themeToastKey++
            }) {
                AnimatedContent(
                    targetState = themeMode,
                    transitionSpec = {
                        (fadeIn(tween(250)) + scaleIn(initialScale = 0.4f, animationSpec = tween(350))) togetherWith
                            (fadeOut(tween(200)) + scaleOut(targetScale = 0.4f, animationSpec = tween(200)))
                    },
                    label = "themeIcon",
                ) { mode ->
                    when (mode) {
                        ThemeMode.System -> AutoThemeGlyph(colors.textPrimary)
                        ThemeMode.Light -> SunGlyph(Brand.Amber)
                        ThemeMode.Dark -> MoonGlyph(Brand.Sky)
                    }
                }
            }
            MapButton(onClick = {
                following = true
                val current = locationState.lastLocation?.position
                if (current != null) {
                    scope.launch {
                        mapState.animateCamera(
                            update = CameraUpdate(center = current, zoom = 16.0, bearing = 0.0, pitch = 0.0),
                            animation = CameraAnimation.Fly { duration = 1.2.seconds },
                        )
                    }
                } else if (locationState.permission !is LocationPermission.Granted) {
                    locationState.requestPermission()
                }
            }) {
                LocateGlyph(if (following) Brand.Sky else colors.textPrimary, active = following)
            }
        }

        BottomPanel(
            locationState = locationState,
            placeState = placeState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .onSizeChanged { panelHeightPx = it.height },
        )
    }
}

/** Search pill with rotating hints (search itself arrives in the next step). */
@Composable
private fun SearchBar() {
    val colors = LocalWayfinderColors.current
    val hints = remember { listOf("Where to?", "Search cafés, places, addresses", "Plan a route home") }
    var hintIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3_200)
            hintIndex = (hintIndex + 1) % hints.size
        }
    }
    GlassPanel(modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp)) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 18.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchGlyph(colors.textMuted)
            Spacer(Modifier.width(12.dp))
            AnimatedContent(
                targetState = hints[hintIndex],
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }) togetherWith
                        (fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 2 })
                },
                modifier = Modifier.weight(1f),
                label = "hint",
            ) { hint ->
                Text(hint, style = MaterialTheme.typography.bodyLarge, color = colors.textMuted, maxLines = 1)
            }
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Brand.Gradient),
                contentAlignment = Alignment.Center,
            ) {
                Text("NB", style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private enum class PanelMode { NeedsPermission, Locating, Located }

@Composable
private fun BottomPanel(locationState: LocationState, placeState: PlaceUiState, modifier: Modifier = Modifier) {
    val mode = when {
        locationState.permission !is LocationPermission.Granted -> PanelMode.NeedsPermission
        locationState.lastLocation == null -> PanelMode.Locating
        else -> PanelMode.Located
    }
    GlassPanel(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                (fadeIn(tween(350)) + slideInVertically(tween(350, easing = FastOutSlowInEasing)) { it / 3 }) togetherWith
                    fadeOut(tween(200))
            },
            label = "panel",
        ) { panelMode ->
            Box(Modifier.padding(20.dp)) {
                when (panelMode) {
                    PanelMode.NeedsPermission -> PermissionContent(locationState)
                    PanelMode.Locating -> LocatingContent()
                    PanelMode.Located -> LocatedContent(locationState, placeState)
                }
            }
        }
    }
}

@Composable
private fun PermissionContent(locationState: LocationState) {
    val colors = LocalWayfinderColors.current
    val settings = rememberSystemSettingsLauncher()
    val permission = locationState.permission
    val mustUseSettings = permission is LocationPermission.NotGranted && permission.canRequest == false

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulsingPin()
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Find your way", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Turn on location to see where you are and get directions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        GradientButton(
            text = if (mustUseSettings) "Open settings" else "Enable location",
            onClick = {
                if (mustUseSettings && settings.canOpenApplicationSettings) settings.openApplicationSettings()
                else locationState.requestPermission()
            },
        )
    }
}

@Composable
private fun LocatingContent() {
    val colors = LocalWayfinderColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadarPulse()
        Spacer(Modifier.width(16.dp))
        Column {
            Text("Finding you…", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Text("Getting a GPS fix", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
        }
    }
}

@Composable
private fun LocatedContent(locationState: LocationState, placeState: PlaceUiState) {
    val colors = LocalWayfinderColors.current
    val position = locationState.lastLocation?.position
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Brand.Gradient),
                contentAlignment = Alignment.Center,
            ) {
                PinGlyph(Color.White, holeColor = Brand.Sky)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("YOU ARE HERE", style = MaterialTheme.typography.labelMedium, color = Brand.Sky)
                AnimatedContent(
                    targetState = placeState,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) },
                    contentKey = { (it as? PlaceUiState.Ready)?.place?.title ?: it::class.simpleName },
                    label = "place",
                ) { state ->
                    when (state) {
                        is PlaceUiState.Ready -> Column {
                            Text(state.place.title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (state.place.subtitle.isNotBlank()) {
                                Text(state.place.subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        PlaceUiState.Error -> Text("Unknown place", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                        else -> ShimmerLines()
                    }
                }
            }
        }
        if (position != null) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot()
                Spacer(Modifier.width(8.dp))
                Text("Live", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                Spacer(Modifier.width(12.dp))
                Text(
                    GeoMath.formatCoordinates(position.latitude, position.longitude),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun GradientButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brand.HorizontalGradient)
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

@Composable
private fun PulsingPin() {
    val transition = rememberInfiniteTransition(label = "pin")
    val bob by transition.animateFloat(-3f, 3f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    Box(
        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Brand.Gradient),
        contentAlignment = Alignment.Center,
    ) {
        PinGlyph(Color.White, holeColor = Brand.Sky, size = 26.dp, modifier = Modifier.graphicsLayer { translationY = bob.dp.toPx() })
    }
}

/** Expanding rings, like a radar sweep, while waiting for the first GPS fix. */
@Composable
private fun RadarPulse() {
    val transition = rememberInfiniteTransition(label = "radar")
    val progress by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "radarProgress")
    Canvas(Modifier.size(48.dp)) {
        val maxR = size.minDimension / 2
        listOf(0f, 0.5f).forEach { offset ->
            val t = (progress + offset) % 1f
            drawCircle(Brand.Sky.copy(alpha = (1f - t) * 0.5f), radius = maxR * t)
        }
        drawCircle(Brand.Sky, radius = maxR * 0.22f)
        drawCircle(Color.White, radius = maxR * 0.1f)
    }
}

@Composable
private fun LiveDot() {
    val transition = rememberInfiniteTransition(label = "live")
    val pulse by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "livePulse")
    Canvas(Modifier.size(12.dp)) {
        val r = size.minDimension / 2
        drawCircle(Color(0xFF22C55E).copy(alpha = (1f - pulse) * 0.5f), radius = r * (0.5f + pulse * 0.5f))
        drawCircle(Color(0xFF22C55E), radius = r * 0.45f)
    }
}

@Composable
private fun ShimmerLines() {
    val colors = LocalWayfinderColors.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "shimmerAlpha")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        Box(Modifier.fillMaxWidth(0.7f).height(18.dp).graphicsLayer { this.alpha = alpha }.clip(RoundedCornerShape(6.dp)).background(colors.surfaceHigh))
        Box(Modifier.fillMaxWidth(0.5f).height(12.dp).graphicsLayer { this.alpha = alpha }.clip(RoundedCornerShape(6.dp)).background(colors.surfaceHigh))
    }
}
