package com.nijobenny.wayfinder.ui.map

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import org.maplibre.compose.layers.LocationIndicatorDefaults
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.key
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nijobenny.wayfinder.domain.model.Category
import com.nijobenny.wayfinder.domain.model.LatLng
import com.nijobenny.wayfinder.domain.model.SavedPlace
import com.nijobenny.wayfinder.domain.model.ThemeMode
import com.nijobenny.wayfinder.domain.model.UserSettings
import com.nijobenny.wayfinder.domain.util.RouteTracker
import com.nijobenny.wayfinder.ui.components.AutoThemeGlyph
import com.nijobenny.wayfinder.ui.components.GlassPanel
import com.nijobenny.wayfinder.ui.components.LocateGlyph
import com.nijobenny.wayfinder.ui.components.MapButton
import com.nijobenny.wayfinder.ui.components.MoonGlyph
import com.nijobenny.wayfinder.ui.components.SearchGlyph
import com.nijobenny.wayfinder.ui.components.SunGlyph
import com.nijobenny.wayfinder.ui.components.bounceClick
import com.nijobenny.wayfinder.ui.theme.Brand
import com.nijobenny.wayfinder.ui.theme.LocalWayfinderColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraFit
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.layers.LineLayer
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
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.TransitionOptions
import org.maplibre.compose.util.DpPadding
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.units.Bearing
import org.maplibre.spatialk.units.extensions.degrees
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Kochi, Kerala: where the camera starts before the first location fix. */
private val StartPosition = Position(latitude = 9.9816, longitude = 76.2999)

private fun LatLng.toPosition() = Position(latitude = latitude, longitude = longitude)

@Composable
fun MapScreen(
    state: MapUiState,
    settings: UserSettings,
    saved: List<SavedPlace>,
    viewModel: MapViewModel,
    onOpenSearch: (addStop: Boolean) -> Unit,
    onCategory: (Category) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val colors = LocalWayfinderColors.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val locationState = rememberLocationState(
        provider = rememberDefaultLocationProvider(),
        headingProvider = rememberDefaultHeadingProvider(),
    )

    val nav = state.navigation
    val readyRoute = (state.route as? RouteUiState.Ready)?.route
    val tracker = remember(readyRoute) { readyRoute?.let { RouteTracker(it) } }

    // Animate the route line being drawn whenever a new route arrives.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(readyRoute) {
        if (readyRoute != null) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
        }
    }

    val routeLine: List<Position> = when {
        tracker == null -> emptyList()
        nav != null -> tracker.remainingGeometry(nav.progress.traveledMeters).map { it.toPosition() }
        else -> tracker.partialGeometry(reveal.value).map { it.toPosition() }
    }

    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(colors.mapStyleUri),
        initialCameraPosition = CameraPosition(center = StartPosition, zoom = 11.5),
    ) {
        if (routeLine.size >= 2) {
            val routeSource = rememberGeoJsonSource(GeoJsonData.Features(Feature(geometry = LineString(routeLine), properties = null)))
            LineLayer(
                id = "route-casing",
                source = routeSource,
                color = const(if (colors.isDark) Color(0xFF0B1220) else Color.White),
                width = const(11.dp),
                cap = const(LineCap.Round),
                join = const(LineJoin.Round),
            )
            LineLayer(
                id = "route",
                source = routeSource,
                color = const(Brand.Sky),
                width = const(6.dp),
                cap = const(LineCap.Round),
                join = const(LineJoin.Round),
            )
        }
        if (nav != null && nav.simulated) {
            LocationIndicatorLayer(
                id = "demo-vehicle",
                location = nav.progress.position.toPosition(),
                bearing = Bearing.North + nav.progress.bearing.degrees,
                locationTransition = TransitionOptions(duration = Duration.ZERO),
                bearingAccuracyColor = const(Brand.Sky.copy(alpha = 0.3f)),
                accuracyRadiusColor = Brand.Sky.copy(alpha = 0.14f),
                accuracyRadiusBorderColor = Brand.Sky.copy(alpha = 0.6f),
                topImage = LocationIndicatorDefaults.topImage(color = Brand.Indigo, borderColor = Color.White),
                bearingImage = LocationIndicatorDefaults.bearingImage(color = Brand.Indigo),
            )
        } else {
            LocationIndicatorLayer(
                id = "user",
                locationState = locationState,
                bearingAccuracyColor = const(Brand.Sky.copy(alpha = 0.30f)),
                accuracyRadiusColor = Brand.Sky.copy(alpha = 0.14f),
                accuracyRadiusBorderColor = Brand.Sky.copy(alpha = 0.6f),
                topImage = LocationIndicatorDefaults.topImage(color = Brand.Sky, borderColor = Color.White),
                bearingImage = LocationIndicatorDefaults.bearingImage(color = Brand.Sky),
            )
        }
    }

    // ---- Camera behaviour -----------------------------------------------------------------

    var following by remember { mutableStateOf(true) }
    LaunchedEffect(mapState) {
        snapshotFlow { mapState.cameraMoveReason }.collect { if (it == CameraMoveReason.Gesture) following = false }
    }
    LocationTrackingEffect(locationState = locationState, enabled = following && nav == null && state.sheet == Sheet.Explore, trackBearing = false) {
        mapState.animateCamera(
            update = CameraUpdate(center = currentLocation.position, zoom = maxOf(mapState.cameraPosition.zoom, 15.0)),
            animation = CameraAnimation.Ease { duration = 900.milliseconds },
        )
    }

    val lastLocation = locationState.lastLocation
    LaunchedEffect(lastLocation) {
        lastLocation?.let { viewModel.onLocation(it.position.latitude, it.position.longitude) }
    }

    var panelHeightPx by remember { mutableIntStateOf(0) }
    val panelHeight = with(density) { panelHeightPx.toDp() }

    // Fly to a selected place.
    LaunchedEffect(state.selected) {
        val place = state.selected ?: return@LaunchedEffect
        following = false
        mapState.animateCamera(
            update = CameraUpdate(center = Position(latitude = place.latitude, longitude = place.longitude), zoom = 15.5, bearing = 0.0, pitch = 0.0),
            animation = CameraAnimation.Fly { duration = 1.2.seconds },
        )
    }

    // Frame the whole route when it's ready.
    LaunchedEffect(readyRoute, nav == null) {
        val route = readyRoute ?: return@LaunchedEffect
        if (nav != null) return@LaunchedEffect
        following = false
        val lats = route.geometry.map { it.latitude }
        val lons = route.geometry.map { it.longitude }
        mapState.animateCameraToBounds(
            boundingBox = BoundingBox(west = lons.min(), south = lats.min(), east = lons.max(), north = lats.max()),
            fit = CameraFit(fitPadding = DpPadding(left = 48.dp, top = 120.dp, right = 48.dp, bottom = 40.dp)),
        )
    }

    // Navigation camera: tilted, heading-up, puck in the lower third.
    LaunchedEffect(nav?.progress) {
        val n = nav ?: return@LaunchedEffect
        mapState.animateCamera(
            update = CameraPosition(
                center = n.progress.position.toPosition(),
                zoom = 17.0,
                bearing = n.progress.bearing,
                pitch = 55.0,
                padding = DpPadding(top = 320.dp),
            ).toCameraUpdate(),
            animation = CameraAnimation.Ease { duration = if (n.simulated) MapViewModel.TICK_MS.milliseconds else 900.milliseconds },
        )
    }
    var wasNavigating by remember { mutableStateOf(false) }
    LaunchedEffect(nav == null) {
        if (nav == null && wasNavigating) {
            mapState.animateCamera(
                update = CameraUpdate(pitch = 0.0, bearing = 0.0, zoom = 15.0, padding = DpPadding.Zero),
                animation = CameraAnimation.Ease { duration = 800.milliseconds },
            )
        }
        wasNavigating = nav != null
    }

    // Theme toast
    var themeToastKey by remember { mutableIntStateOf(0) }
    var showThemeToast by remember { mutableStateOf(false) }
    LaunchedEffect(themeToastKey) {
        if (themeToastKey > 0) {
            showThemeToast = true
            delay(1_400)
            showThemeToast = false
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.background)) {
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val sheetIsTall = panelHeight > maxHeight * 0.45f
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            state = mapState,
            viewportInsets = PaddingValues(bottom = panelHeight + navBarBottom + 32.dp),
            interactions = MapInteractions {
                callbacks {
                    longClick {
                        onEvent { event ->
                            val p = event.position
                            if (p != null && nav == null) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.dropPin(p.latitude, p.longitude)
                            }
                            ClickResult.Consume
                        }
                    }
                }
            },
            overlay = {
                include(MapOverlay.AttributionOnly)
                val selected = state.selected
                if (selected != null && nav == null) {
                    key(selected.key) {
                        DropPin(label = null, modifier = Modifier.placedAt(Position(latitude = selected.latitude, longitude = selected.longitude), Alignment.BottomCenter))
                    }
                }
                state.stops.forEachIndexed { i, stop ->
                    key("stop-" + stop.key + "-" + i) {
                        StopMarker(
                            number = i + 1,
                            isFinal = i == state.stops.lastIndex,
                            modifier = Modifier.placedAt(Position(latitude = stop.latitude, longitude = stop.longitude), Alignment.BottomCenter),
                        )
                    }
                }
            },
        )

        // Readability scrim behind the status bar.
        Box(
            Modifier.fillMaxWidth().height(130.dp)
                .background(Brush.verticalGradient(listOf(colors.background.copy(alpha = 0.75f), Color.Transparent)))
        )

        // Top: search bar, or turn-by-turn banner while navigating.
        Column(
            modifier = Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = nav != null,
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInVertically(tween(350)) { -it }) togetherWith (fadeOut(tween(200)) + slideOutVertically(tween(250)) { -it })
                },
                label = "top",
            ) { navigating ->
                if (navigating && nav != null) NavigationBanner(nav, settings.units)
                else SearchBar(initials = initialsOf(settings.displayName), onClick = { onOpenSearch(false) }, onAvatar = onOpenProfile)
            }
            Spacer(Modifier.height(10.dp))
            AnimatedVisibility(
                visible = showThemeToast,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 },
            ) {
                GlassPanel(shape = RoundedCornerShape(50), elevation = 8.dp) {
                    Text(
                        when (settings.themeMode) {
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

        // Floating controls just above the sheet.
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 16.dp, bottom = panelHeight + 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedVisibility(visible = nav == null && !sheetIsTall, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                MapButton(onClick = {
                    viewModel.cycleThemeMode()
                    themeToastKey++
                }) {
                    AnimatedContent(
                        targetState = settings.themeMode,
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
            }
            AnimatedVisibility(visible = nav == null && !sheetIsTall, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
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
        }

        // Bottom sheet that morphs between explore, place, planner and navigation.
        GlassPanel(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                .heightIn(max = maxHeight * 0.62f)
                .onSizeChanged { panelHeightPx = it.height },
            shape = RoundedCornerShape(28.dp),
        ) {
            val needsPermission = locationState.permission !is LocationPermission.Granted
            AnimatedContent(
                targetState = if (needsPermission && state.sheet == Sheet.Explore) null else state.sheet,
                transitionSpec = {
                    (fadeIn(tween(320)) + slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 4 }) togetherWith fadeOut(tween(160))
                },
                label = "sheet",
            ) { sheet ->
                Box(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                    when (sheet) {
                        null -> PermissionContent(locationState)
                        Sheet.Explore -> ExploreSheet(state, saved, settings.units, onCategory = onCategory, onSavedPlace = viewModel::selectPlace)
                        Sheet.Place -> state.selected?.let { place ->
                            PlaceSheet(
                                place = place,
                                state = state,
                                units = settings.units,
                                isSaved = place.key in state.savedKeys,
                                canAddStop = state.stops.isNotEmpty(),
                                onDirections = { viewModel.directionsTo(place) },
                                onAddStop = { viewModel.addStop(place) },
                                onToggleSave = { viewModel.toggleSaved(place) },
                                onClose = viewModel::clearSelection,
                            )
                        }
                        Sheet.Planner -> PlannerSheet(
                            state = state,
                            units = settings.units,
                            onMode = viewModel::setMode,
                            onAddStop = { onOpenSearch(true) },
                            onRemoveStop = viewModel::removeStop,
                            onMoveStop = viewModel::moveStop,
                            onStart = { viewModel.startNavigation(simulate = false) },
                            onDemo = { viewModel.startNavigation(simulate = true) },
                            onRetry = viewModel::requestRoute,
                            onClose = viewModel::clearRoute,
                        )
                        Sheet.Navigation -> nav?.let { NavigationPanel(it, settings.units, onEnd = viewModel::stopNavigation) }
                    }
                }
            }
        }
    }
}

private fun initialsOf(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "W" }

@Composable
private fun SearchBar(initials: String, onClick: () -> Unit, onAvatar: () -> Unit) {
    val colors = LocalWayfinderColors.current
    val hints = remember { listOf("Where to?", "Search cafés, places, addresses", "Plan a route with stops") }
    var hintIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3_200)
            hintIndex = (hintIndex + 1) % hints.size
        }
    }
    GlassPanel(modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick(onClick = onClick), shape = RoundedCornerShape(28.dp)) {
        Row(Modifier.fillMaxSize().padding(start = 18.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchGlyph(colors.textMuted)
            Spacer(Modifier.width(12.dp))
            AnimatedContent(
                targetState = hints[hintIndex],
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }) togetherWith (fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 2 })
                },
                modifier = Modifier.weight(1f),
                label = "hint",
            ) { hint -> Text(hint, style = MaterialTheme.typography.bodyLarge, color = colors.textMuted, maxLines = 1) }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Brand.Gradient).bounceClick(onClick = onAvatar),
                contentAlignment = Alignment.Center,
            ) { Text(initials, style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold) }
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
        Text("Find your way", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text("Turn on location to see where you are, plan routes and navigate.", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
        Spacer(Modifier.height(16.dp))
        com.nijobenny.wayfinder.ui.components.GradientButton(
            text = if (mustUseSettings) "Open settings" else "Enable location",
            onClick = {
                if (mustUseSettings && settings.canOpenApplicationSettings) settings.openApplicationSettings()
                else locationState.requestPermission()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Suppress("unused")
private val Linear = LinearEasing
