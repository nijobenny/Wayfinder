package com.nijobenny.wayfinder

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nijobenny.wayfinder.domain.model.ThemeMode
import com.nijobenny.wayfinder.platform.PlatformBackHandler
import com.nijobenny.wayfinder.platform.SystemBarsEffect
import com.nijobenny.wayfinder.ui.components.GlassPanel
import com.nijobenny.wayfinder.ui.components.collectAsStateLifecycle
import com.nijobenny.wayfinder.ui.map.MapEvent
import com.nijobenny.wayfinder.ui.map.MapScreen
import com.nijobenny.wayfinder.ui.map.MapViewModel
import com.nijobenny.wayfinder.ui.map.ArrivalCelebration
import com.nijobenny.wayfinder.ui.profile.ProfileScreen
import com.nijobenny.wayfinder.ui.profile.ProfileViewModel
import com.nijobenny.wayfinder.ui.search.SearchPurpose
import com.nijobenny.wayfinder.ui.search.SearchScreen
import com.nijobenny.wayfinder.ui.search.SearchViewModel
import com.nijobenny.wayfinder.ui.settings.SettingsScreen
import com.nijobenny.wayfinder.ui.settings.SettingsViewModel
import com.nijobenny.wayfinder.ui.splash.SplashIntro
import com.nijobenny.wayfinder.ui.theme.LocalWayfinderColors
import com.nijobenny.wayfinder.ui.theme.WayfinderTheme
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

private enum class Screen { Map, Profile, Settings }

@Composable
fun App() {
    val mapViewModel: MapViewModel = koinViewModel()
    val searchViewModel: SearchViewModel = koinViewModel()
    val profileViewModel: ProfileViewModel = koinViewModel()
    val settingsViewModel: SettingsViewModel = koinViewModel()

    val settings by mapViewModel.settings.collectAsStateLifecycle()
    val mapState by mapViewModel.state.collectAsStateLifecycle()
    val saved by searchViewModel.saved.collectAsStateLifecycle()

    var showIntro by rememberSaveable { mutableStateOf(true) }
    var screen by rememberSaveable { mutableStateOf(Screen.Map) }
    var searchPurpose by remember { mutableStateOf<SearchPurpose?>(null) }
    var arrivedAt by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mapViewModel) {
        mapViewModel.events.collect { event ->
            when (event) {
                is MapEvent.Arrived -> arrivedAt = event.title
                is MapEvent.Message -> toast = event.text
            }
        }
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2_200)
            toast = null
        }
    }

    val darkTheme = when (settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    SystemBarsEffect(useDarkIcons = !darkTheme && !showIntro && screen != Screen.Profile)

    // Back closes the top-most layer: search → settings/profile → map sheets.
    PlatformBackHandler(enabled = searchPurpose != null || screen != Screen.Map || mapState.sheet != com.nijobenny.wayfinder.ui.map.Sheet.Explore) {
        when {
            searchPurpose != null -> searchPurpose = null
            screen == Screen.Settings -> screen = Screen.Profile
            screen == Screen.Profile -> screen = Screen.Map
            else -> mapViewModel.handleBack()
        }
    }

    WayfinderTheme(darkTheme = darkTheme) {
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(220)))
                },
                label = "screen",
            ) { current ->
                when (current) {
                    Screen.Map -> MapScreen(
                        state = mapState,
                        settings = settings,
                        saved = saved,
                        viewModel = mapViewModel,
                        onOpenSearch = { addStop -> searchPurpose = if (addStop) SearchPurpose.AddStop else SearchPurpose.Destination },
                        onCategory = { category ->
                            searchPurpose = SearchPurpose.Destination
                            searchViewModel.setNear(mapState.myLocation)
                            searchViewModel.onCategory(category)
                        },
                        onOpenProfile = { screen = Screen.Profile },
                    )
                    Screen.Profile -> ProfileScreen(
                        viewModel = profileViewModel,
                        onBack = { screen = Screen.Map },
                        onOpenSettings = { screen = Screen.Settings },
                        onOpenPlace = { place ->
                            screen = Screen.Map
                            mapViewModel.selectPlace(place)
                        },
                    )
                    Screen.Settings -> SettingsScreen(viewModel = settingsViewModel, onBack = { screen = Screen.Profile })
                }
            }

            // Search slides up over the map.
            AnimatedVisibility(
                visible = searchPurpose != null,
                enter = slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 5 } + fadeIn(tween(260)),
                exit = slideOutVertically(tween(300)) { it / 5 } + fadeOut(tween(200)),
            ) {
                val purpose = searchPurpose ?: SearchPurpose.Destination
                SearchScreen(
                    viewModel = searchViewModel,
                    purpose = purpose,
                    myLocation = mapState.myLocation,
                    units = settings.units,
                    onPlaceChosen = { place ->
                        searchPurpose = null
                        if (purpose == SearchPurpose.AddStop) mapViewModel.addStop(place) else mapViewModel.selectPlace(place)
                    },
                    onClose = { searchPurpose = null },
                )
            }

            // Small toast for messages from the map.
            AnimatedVisibility(
                visible = toast != null,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f),
                modifier = Modifier.align(Alignment.Center),
            ) {
                GlassPanel(shape = RoundedCornerShape(50), elevation = 10.dp) {
                    Text(toast ?: "", style = MaterialTheme.typography.labelLarge, color = LocalWayfinderColors.current.textPrimary, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                }
            }

            // Arrival celebration.
            AnimatedVisibility(visible = arrivedAt != null, enter = fadeIn(), exit = fadeOut(tween(250))) {
                ArrivalCelebration(title = arrivedAt ?: "", onDismiss = { arrivedAt = null })
            }

            // Animated intro on top of everything; fades and zooms away to reveal the map.
            AnimatedVisibility(
                visible = showIntro,
                enter = fadeIn(tween(0)),
                exit = fadeOut(tween(450)) + scaleOut(targetScale = 1.08f, animationSpec = tween(450)),
            ) {
                SplashIntro(onFinished = { showIntro = false })
            }
        }
    }
}

