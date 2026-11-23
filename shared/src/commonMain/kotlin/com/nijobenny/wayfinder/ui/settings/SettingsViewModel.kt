package com.nijobenny.wayfinder.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nijobenny.wayfinder.domain.model.ThemeMode
import com.nijobenny.wayfinder.domain.model.TravelMode
import com.nijobenny.wayfinder.domain.model.Units
import com.nijobenny.wayfinder.domain.model.UserSettings
import com.nijobenny.wayfinder.domain.repository.PlacesRepository
import com.nijobenny.wayfinder.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val places: PlacesRepository,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    fun setTheme(mode: ThemeMode) = launch { repository.setThemeMode(mode) }
    fun setUnits(units: Units) = launch { repository.setUnits(units) }
    fun setDefaultMode(mode: TravelMode) = launch { repository.setDefaultMode(mode) }
    fun setAvoidTolls(value: Boolean) = launch { repository.setAvoidTolls(value) }
    fun setAvoidHighways(value: Boolean) = launch { repository.setAvoidHighways(value) }
    fun setDemoSpeed(value: Int) = launch { repository.setDemoSpeed(value) }
    fun clearSearchHistory() = launch { places.clearRecents() }
    fun clearTrips() = launch { places.clearTrips() }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
