package com.nijobenny.wayfinder.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nijobenny.wayfinder.domain.model.LatLng
import com.nijobenny.wayfinder.domain.model.Place
import com.nijobenny.wayfinder.domain.model.Route
import com.nijobenny.wayfinder.domain.model.RouteOptions
import com.nijobenny.wayfinder.domain.model.TravelMode
import com.nijobenny.wayfinder.domain.model.TripRecord
import com.nijobenny.wayfinder.domain.model.UserSettings
import com.nijobenny.wayfinder.domain.repository.GeocodingRepository
import com.nijobenny.wayfinder.domain.repository.PlacesRepository
import com.nijobenny.wayfinder.domain.repository.RoutingRepository
import com.nijobenny.wayfinder.domain.repository.SettingsRepository
import com.nijobenny.wayfinder.domain.util.GeoMath
import com.nijobenny.wayfinder.domain.util.RouteProgress
import com.nijobenny.wayfinder.domain.util.RouteTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PlaceUiState {
    data object Idle : PlaceUiState
    data object Loading : PlaceUiState
    data class Ready(val place: Place) : PlaceUiState
    data object Error : PlaceUiState
}

sealed interface RouteUiState {
    data object Idle : RouteUiState
    data object Loading : RouteUiState
    data class Ready(val route: Route) : RouteUiState
    data class Error(val message: String) : RouteUiState
}

/** Live navigation along a route (real GPS or the demo drive). */
data class NavigationState(
    val route: Route,
    val progress: RouteProgress,
    val simulated: Boolean,
    val destinationTitle: String,
)

data class MapUiState(
    val myLocation: LatLng? = null,
    val myPlace: PlaceUiState = PlaceUiState.Idle,
    /** Place shown in the details card (from search, long-press or saved places). */
    val selected: Place? = null,
    /** Destinations in visiting order. Non-empty means the route planner is open. */
    val stops: List<Place> = emptyList(),
    val mode: TravelMode = TravelMode.Car,
    val route: RouteUiState = RouteUiState.Idle,
    val navigation: NavigationState? = null,
    val savedKeys: Set<String> = emptySet(),
) {
    val sheet: Sheet
        get() = when {
            navigation != null -> Sheet.Navigation
            stops.isNotEmpty() -> Sheet.Planner
            selected != null -> Sheet.Place
            else -> Sheet.Explore
        }
}

enum class Sheet { Explore, Place, Planner, Navigation }

sealed interface MapEvent {
    data class Arrived(val title: String) : MapEvent
    data class Message(val text: String) : MapEvent
}

class MapViewModel(
    private val settingsRepository: SettingsRepository,
    private val geocoding: GeocodingRepository,
    private val routing: RoutingRepository,
    private val places: PlacesRepository,
    private val currentTimeMillis: () -> Long,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    private val _events = Channel<MapEvent>(Channel.BUFFERED)
    val events: Flow<MapEvent> = _events.receiveAsFlow()

    private var lastGeocoded: LatLng? = null
    private var geocodeJob: Job? = null
    private var routeJob: Job? = null
    private var simulationJob: Job? = null
    private var tracker: RouteTracker? = null
    private var modeChosenByUser = false

    init {
        viewModelScope.launch {
            places.savedPlaces.map { list -> list.map { it.place.key }.toSet() }.collect { keys ->
                _state.update { it.copy(savedKeys = keys) }
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                if (!modeChosenByUser) _state.update { it.copy(mode = s.defaultMode) }
            }
        }
    }

    // ---- Theme ----------------------------------------------------------------------------

    fun cycleThemeMode() {
        viewModelScope.launch { settingsRepository.setThemeMode(settings.value.themeMode.next()) }
    }

    // ---- Location -------------------------------------------------------------------------

    fun onLocation(latitude: Double, longitude: Double) {
        val here = LatLng(latitude, longitude)
        _state.update { it.copy(myLocation = here) }

        // Real GPS navigation: snap the fix onto the route.
        val nav = _state.value.navigation
        val t = tracker
        if (nav != null && !nav.simulated && t != null) updateNavigation(t.progressNear(here))

        val last = lastGeocoded
        if (last != null && _state.value.myPlace !is PlaceUiState.Error &&
            GeoMath.distanceMeters(last.latitude, last.longitude, latitude, longitude) < MIN_MOVE_METERS
        ) return
        lastGeocoded = here
        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch {
            if (_state.value.myPlace !is PlaceUiState.Ready) _state.update { it.copy(myPlace = PlaceUiState.Loading) }
            val result = geocoding.reverse(latitude, longitude)
            _state.update {
                it.copy(myPlace = result.fold(onSuccess = { p -> PlaceUiState.Ready(p) }, onFailure = { PlaceUiState.Error }))
            }
        }
    }

    // ---- Places ---------------------------------------------------------------------------

    fun selectPlace(place: Place) {
        _state.update { it.copy(selected = place) }
        viewModelScope.launch { places.addRecent(place) }
    }

    /** Long-press on the map: show a "Dropped pin" immediately, then resolve its address. */
    fun dropPin(latitude: Double, longitude: Double) {
        val pin = Place("Dropped pin", "Looking up address…", latitude, longitude)
        _state.update { it.copy(selected = pin) }
        viewModelScope.launch {
            val resolved = geocoding.reverse(latitude, longitude).getOrNull()
            _state.update { s ->
                if (s.selected?.latitude == latitude && s.selected.longitude == longitude) {
                    s.copy(selected = pin.copy(subtitle = resolved?.let { r -> listOf(r.title, r.subtitle).filter { x -> x.isNotBlank() }.joinToString(", ") } ?: "Unknown address"))
                } else s
            }
        }
    }

    fun clearSelection() = _state.update { it.copy(selected = null) }

    fun toggleSaved(place: Place) {
        viewModelScope.launch {
            val emoji = when (_state.value.savedKeys.size) {
                0 -> "🏠"
                1 -> "💼"
                else -> "⭐"
            }
            places.toggleSaved(place, emoji)
        }
    }

    // ---- Route planning -------------------------------------------------------------------

    fun directionsTo(place: Place) {
        _state.update { it.copy(stops = listOf(place), selected = null) }
        requestRoute()
    }

    fun addStop(place: Place) {
        if (_state.value.stops.size >= MAX_STOPS) {
            _events.trySend(MapEvent.Message("Up to $MAX_STOPS stops"))
            return
        }
        _state.update { it.copy(stops = it.stops + place, selected = null) }
        requestRoute()
    }

    fun removeStop(index: Int) {
        _state.update { it.copy(stops = it.stops.filterIndexed { i, _ -> i != index }) }
        if (_state.value.stops.isEmpty()) clearRoute() else requestRoute()
    }

    fun moveStop(from: Int, to: Int) {
        val stops = _state.value.stops.toMutableList()
        if (from !in stops.indices || to !in stops.indices) return
        stops.add(to, stops.removeAt(from))
        _state.update { it.copy(stops = stops) }
        requestRoute()
    }

    fun setMode(mode: TravelMode) {
        modeChosenByUser = true
        if (mode == _state.value.mode) return
        _state.update { it.copy(mode = mode) }
        if (_state.value.stops.isNotEmpty()) requestRoute()
    }

    fun clearRoute() {
        routeJob?.cancel()
        tracker = null
        _state.update { it.copy(stops = emptyList(), route = RouteUiState.Idle) }
    }

    fun requestRoute() {
        val s = _state.value
        val origin = s.myLocation
        if (origin == null) {
            _state.update { it.copy(route = RouteUiState.Error("Turn on location to plan a route from where you are.")) }
            return
        }
        routeJob?.cancel()
        routeJob = viewModelScope.launch {
            _state.update { it.copy(route = RouteUiState.Loading) }
            val prefs = settings.value
            val result = routing.route(
                points = listOf(origin) + s.stops.map { it.latLng },
                mode = s.mode,
                options = RouteOptions(avoidTolls = prefs.avoidTolls, avoidHighways = prefs.avoidHighways),
            )
            result.onSuccess { tracker = RouteTracker(it) }
            _state.update {
                it.copy(
                    route = result.fold(
                        onSuccess = { r -> RouteUiState.Ready(r) },
                        onFailure = { RouteUiState.Error("Couldn't find a route. Check your connection and try again.") },
                    )
                )
            }
        }
    }

    // ---- Navigation -----------------------------------------------------------------------

    /** Starts turn-by-turn. [simulate] drives along the route automatically (great for demos). */
    fun startNavigation(simulate: Boolean) {
        val route = (_state.value.route as? RouteUiState.Ready)?.route ?: return
        val t = tracker ?: RouteTracker(route).also { tracker = it }
        val title = _state.value.stops.lastOrNull()?.title ?: "Destination"
        val start = _state.value.myLocation?.takeIf { !simulate }?.let { t.progressNear(it) } ?: t.progressAt(0.0)
        _state.update { it.copy(navigation = NavigationState(route, start, simulate, title)) }

        simulationJob?.cancel()
        if (simulate) {
            simulationJob = viewModelScope.launch {
                var traveled = 0.0
                val speed = route.mode.demoSpeedMps * settings.value.demoSpeed
                while (_state.value.navigation != null) {
                    delay(TICK_MS)
                    traveled += speed * TICK_MS / 1000.0
                    updateNavigation(t.progressAt(traveled))
                }
            }
        }
    }

    private fun updateNavigation(progress: RouteProgress) {
        val nav = _state.value.navigation ?: return
        _state.update { it.copy(navigation = nav.copy(progress = progress)) }
        if (progress.arrived) finishNavigation(arrived = true)
    }

    fun stopNavigation() = finishNavigation(arrived = false)

    private fun finishNavigation(arrived: Boolean) {
        val nav = _state.value.navigation ?: return
        simulationJob?.cancel()
        simulationJob = null
        _state.update { it.copy(navigation = null) }
        if (arrived || nav.progress.traveledMeters > 200) {
            viewModelScope.launch {
                places.addTrip(
                    TripRecord(
                        title = nav.destinationTitle,
                        distanceMeters = nav.progress.traveledMeters,
                        durationSeconds = nav.route.durationSeconds - nav.progress.remainingSeconds,
                        mode = nav.route.mode,
                        finishedAtMillis = currentTimeMillis(),
                        stops = _state.value.stops.size,
                    )
                )
            }
        }
        if (arrived) {
            _events.trySend(MapEvent.Arrived(nav.destinationTitle))
            clearRoute()
        }
    }

    /** System back: closes the top-most layer. Returns false when there is nothing to close. */
    fun handleBack(): Boolean {
        val s = _state.value
        when {
            s.navigation != null -> stopNavigation()
            s.stops.isNotEmpty() -> clearRoute()
            s.selected != null -> clearSelection()
            else -> return false
        }
        return true
    }

    suspend fun awaitSettings(): UserSettings = settingsRepository.settings.first()

    companion object {
        const val MIN_MOVE_METERS = 150.0
        const val MAX_STOPS = 5
        const val TICK_MS = 100L
    }
}
