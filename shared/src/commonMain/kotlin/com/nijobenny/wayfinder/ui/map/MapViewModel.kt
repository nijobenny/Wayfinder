package com.nijobenny.wayfinder.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nijobenny.wayfinder.domain.model.Place
import com.nijobenny.wayfinder.domain.model.ThemeMode
import com.nijobenny.wayfinder.domain.repository.GeocodingRepository
import com.nijobenny.wayfinder.domain.repository.SettingsRepository
import com.nijobenny.wayfinder.domain.util.GeoMath
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PlaceUiState {
    data object Idle : PlaceUiState
    data object Loading : PlaceUiState
    data class Ready(val place: Place) : PlaceUiState
    data object Error : PlaceUiState
}

class MapViewModel(
    private val settings: SettingsRepository,
    private val geocoding: GeocodingRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settings.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ThemeMode.System,
    )

    private val _place = MutableStateFlow<PlaceUiState>(PlaceUiState.Idle)
    val place: StateFlow<PlaceUiState> = _place.asStateFlow()

    private var lastGeocoded: Pair<Double, Double>? = null
    private var geocodeJob: Job? = null

    fun cycleThemeMode() {
        viewModelScope.launch { settings.setThemeMode(themeMode.value.next()) }
    }

    /**
     * Called on every location fix. Only looks up the place name again after moving
     * [MIN_MOVE_METERS], which keeps the free geocoding API happy.
     */
    fun onLocation(latitude: Double, longitude: Double) {
        val last = lastGeocoded
        if (last != null && _place.value !is PlaceUiState.Error &&
            GeoMath.distanceMeters(last.first, last.second, latitude, longitude) < MIN_MOVE_METERS
        ) return

        lastGeocoded = latitude to longitude
        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch {
            if (_place.value !is PlaceUiState.Ready) _place.value = PlaceUiState.Loading
            _place.value = geocoding.reverse(latitude, longitude).fold(
                onSuccess = { PlaceUiState.Ready(it) },
                onFailure = { PlaceUiState.Error },
            )
        }
    }

    companion object {
        const val MIN_MOVE_METERS = 150.0
    }
}
