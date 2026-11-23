package com.nijobenny.wayfinder.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nijobenny.wayfinder.domain.model.Place
import com.nijobenny.wayfinder.domain.model.SavedPlace
import com.nijobenny.wayfinder.domain.model.TripRecord
import com.nijobenny.wayfinder.domain.model.UserSettings
import com.nijobenny.wayfinder.domain.repository.PlacesRepository
import com.nijobenny.wayfinder.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Achievement(val emoji: String, val title: String, val description: String, val unlocked: Boolean)

data class ProfileUiState(
    val settings: UserSettings = UserSettings(),
    val saved: List<SavedPlace> = emptyList(),
    val trips: List<TripRecord> = emptyList(),
) {
    val totalMeters: Double get() = trips.sumOf { it.distanceMeters }
    val totalSeconds: Double get() = trips.sumOf { it.durationSeconds }

    val achievements: List<Achievement>
        get() = listOf(
            Achievement("🚀", "First trip", "Complete your first trip", trips.isNotEmpty()),
            Achievement("📍", "Collector", "Save 3 places", saved.size >= 3),
            Achievement("🧭", "Explorer", "Travel 25 km in total", totalMeters >= 25_000),
            Achievement("🔁", "Regular", "Complete 5 trips", trips.size >= 5),
            Achievement("🛣️", "Road warrior", "Travel 100 km in total", totalMeters >= 100_000),
            Achievement("🚲", "Eco rider", "Finish a trip by bike or on foot", trips.any { it.mode.name != "Car" }),
        )

    val initials: String
        get() = settings.displayName.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "W" }
}

class ProfileViewModel(
    private val settingsRepository: SettingsRepository,
    private val places: PlacesRepository,
) : ViewModel() {

    val state: StateFlow<ProfileUiState> = combine(settingsRepository.settings, places.savedPlaces, places.trips) { s, saved, trips ->
        ProfileUiState(s, saved, trips)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun rename(name: String) {
        viewModelScope.launch { settingsRepository.setDisplayName(name) }
    }

    fun unsave(place: Place) {
        viewModelScope.launch { places.toggleSaved(place) }
    }
}
