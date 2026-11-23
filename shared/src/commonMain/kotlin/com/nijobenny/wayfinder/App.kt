package com.nijobenny.wayfinder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nijobenny.wayfinder.domain.model.ThemeMode
import com.nijobenny.wayfinder.platform.SystemBarsEffect
import com.nijobenny.wayfinder.ui.map.MapScreen
import com.nijobenny.wayfinder.ui.map.MapViewModel
import com.nijobenny.wayfinder.ui.splash.SplashIntro
import com.nijobenny.wayfinder.ui.theme.WayfinderTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val viewModel: MapViewModel = koinViewModel()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val placeState by viewModel.place.collectAsStateWithLifecycle()
    var showIntro by rememberSaveable { mutableStateOf(true) }

    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    // Light status-bar icons during the (always dark) intro, then follow the theme.
    SystemBarsEffect(useDarkIcons = !darkTheme && !showIntro)

    WayfinderTheme(darkTheme = darkTheme) {
        Box(Modifier.fillMaxSize()) {
            MapScreen(
                placeState = placeState,
                themeMode = themeMode,
                onCycleTheme = viewModel::cycleThemeMode,
                onLocation = viewModel::onLocation,
            )

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
