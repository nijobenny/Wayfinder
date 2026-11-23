package com.nijobenny.wayfinder.ui

import com.nijobenny.wayfinder.domain.model.Category
import com.nijobenny.wayfinder.domain.model.LatLng
import com.nijobenny.wayfinder.domain.model.Maneuver
import com.nijobenny.wayfinder.domain.model.Place
import com.nijobenny.wayfinder.domain.model.Route
import com.nijobenny.wayfinder.domain.model.RouteOptions
import com.nijobenny.wayfinder.domain.model.SavedPlace
import com.nijobenny.wayfinder.domain.model.ThemeMode
import com.nijobenny.wayfinder.domain.model.TravelMode
import com.nijobenny.wayfinder.domain.model.TripRecord
import com.nijobenny.wayfinder.domain.model.Units
import com.nijobenny.wayfinder.domain.model.UserSettings
import com.nijobenny.wayfinder.domain.repository.GeocodingRepository
import com.nijobenny.wayfinder.domain.repository.PlacesRepository
import com.nijobenny.wayfinder.domain.repository.RoutingRepository
import com.nijobenny.wayfinder.domain.repository.SettingsRepository
import com.nijobenny.wayfinder.ui.map.MapViewModel
import com.nijobenny.wayfinder.ui.map.PlaceUiState
import com.nijobenny.wayfinder.ui.map.RouteUiState
import com.nijobenny.wayfinder.ui.map.Sheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private class FakeSettings : SettingsRepository {
        val state = MutableStateFlow(UserSettings())
        override val settings = state
        override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
        override suspend fun setUnits(units: Units) = state.update { it.copy(units = units) }
        override suspend fun setDefaultMode(mode: TravelMode) = state.update { it.copy(defaultMode = mode) }
        override suspend fun setAvoidTolls(avoid: Boolean) = state.update { it.copy(avoidTolls = avoid) }
        override suspend fun setAvoidHighways(avoid: Boolean) = state.update { it.copy(avoidHighways = avoid) }
        override suspend fun setDisplayName(name: String) = state.update { it.copy(displayName = name) }
        override suspend fun setDemoSpeed(multiplier: Int) = state.update { it.copy(demoSpeed = multiplier) }
    }

    private class FakeGeocoding(var fail: Boolean = false) : GeocodingRepository {
        var calls = 0
        override suspend fun reverse(latitude: Double, longitude: Double): Result<Place> {
            calls++
            return if (fail) Result.failure(Exception("offline")) else Result.success(Place("Place $calls", "Kochi", latitude, longitude))
        }
        override suspend fun search(query: String, near: LatLng?) = Result.success(emptyList<Place>())
        override suspend fun nearby(category: Category, near: LatLng?) = Result.success(emptyList<Place>())
    }

    /** Returns a straight ~1.1 km route north from the first point. */
    private class FakeRouting : RoutingRepository {
        var lastPoints: List<LatLng> = emptyList()
        var lastOptions: RouteOptions? = null
        override suspend fun route(points: List<LatLng>, mode: TravelMode, options: RouteOptions): Result<Route> {
            lastPoints = points
            lastOptions = options
            val start = points.first()
            val geometry = (0..10).map { LatLng(start.latitude + it * 0.001, start.longitude) }
            return Result.success(
                Route(
                    geometry = geometry,
                    maneuvers = listOf(
                        Maneuver(1, "Head north", 500.0, 60.0, 0),
                        Maneuver(10, "Turn right", 600.0, 70.0, 5),
                        Maneuver(4, "You have arrived", 0.0, 0.0, 10),
                    ),
                    distanceMeters = 1_112.0,
                    durationSeconds = 130.0,
                    mode = mode,
                )
            )
        }
    }

    private class FakePlaces : PlacesRepository {
        override val savedPlaces = MutableStateFlow<List<SavedPlace>>(emptyList())
        override val recentSearches = MutableStateFlow<List<Place>>(emptyList())
        override val trips = MutableStateFlow<List<TripRecord>>(emptyList())
        override suspend fun toggleSaved(place: Place, emoji: String) = savedPlaces.update { list ->
            if (list.any { it.place.key == place.key }) list.filterNot { it.place.key == place.key } else list + SavedPlace(place, emoji)
        }
        override suspend fun addRecent(place: Place) = recentSearches.update { listOf(place) + it }
        override suspend fun clearRecents() { recentSearches.value = emptyList() }
        override suspend fun addTrip(trip: TripRecord) = trips.update { listOf(trip) + it }
        override suspend fun clearTrips() { trips.value = emptyList() }
    }

    private val cafe = Place("Cafe", "Kochi", 10.02, 76.30)
    private val park = Place("Park", "Kochi", 10.03, 76.31)

    private lateinit var settings: FakeSettings
    private lateinit var geocoding: FakeGeocoding
    private lateinit var routing: FakeRouting
    private lateinit var places: FakePlaces

    private fun createViewModel() = MapViewModel(settings, geocoding, routing, places) { 0L }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        settings = FakeSettings(); geocoding = FakeGeocoding(); routing = FakeRouting(); places = FakePlaces()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun cycleTheme_persistsNextMode() = runTest(dispatcher) {
        val vm = createViewModel()
        vm.cycleThemeMode()
        assertEquals(ThemeMode.Light, settings.state.value.themeMode)
    }

    @Test
    fun smallMovements_doNotTriggerNewReverseLookups() = runTest(dispatcher) {
        val vm = createViewModel()
        vm.onLocation(10.0000, 76.0000)
        vm.onLocation(10.0005, 76.0000) // ~55 m
        assertEquals(1, geocoding.calls)
        vm.onLocation(10.0050, 76.0000) // ~550 m
        assertEquals(2, geocoding.calls)
        assertEquals("Place 2", (vm.state.value.myPlace as PlaceUiState.Ready).place.title)
    }

    @Test
    fun directions_withoutLocation_showsHelpfulError() = runTest(dispatcher) {
        val vm = createViewModel()
        vm.directionsTo(cafe)
        assertIs<RouteUiState.Error>(vm.state.value.route)
    }

    @Test
    fun multiStopRoute_goesFromMeThroughEveryStopInOrder() = runTest(dispatcher) {
        val vm = createViewModel()
        vm.onLocation(10.0, 76.0)
        vm.directionsTo(cafe)
        vm.addStop(park)
        assertEquals(Sheet.Planner, vm.state.value.sheet)
        assertEquals(listOf(LatLng(10.0, 76.0), cafe.latLng, park.latLng), routing.lastPoints)

        vm.moveStop(1, 0)
        assertEquals(listOf(park, cafe), vm.state.value.stops)
        assertEquals(park.latLng, routing.lastPoints[1])
        assertIs<RouteUiState.Ready>(vm.state.value.route)
    }

    @Test
    fun routePreferences_arePassedToRouting() = runTest(dispatcher) {
        settings.state.update { it.copy(avoidTolls = true) }
        val vm = createViewModel()
        vm.onLocation(10.0, 76.0)
        vm.directionsTo(cafe)
        assertEquals(true, routing.lastOptions?.avoidTolls)
    }

    @Test
    fun demoDrive_advancesAlongRoute_andRecordsTripOnArrival() = runTest(dispatcher) {
        val vm = createViewModel()
        vm.onLocation(10.0, 76.0)
        vm.directionsTo(cafe)
        vm.startNavigation(simulate = true)
        assertNotNull(vm.state.value.navigation)
        assertEquals("Turn right", vm.state.value.navigation?.progress?.nextManeuver?.instruction)

        advanceTimeBy(5_000) // 22 m/s × 3 = 66 m/s → ~330 m after 5 s
        val mid = vm.state.value.navigation
        assertNotNull(mid)
        assertTrue(mid.progress.traveledMeters > 300)

        advanceTimeBy(20_000)
        assertNull(vm.state.value.navigation)
        assertEquals(1, places.trips.value.size)
        assertEquals("Cafe", places.trips.value.first().title)
    }

    @Test
    fun back_closesLayersInOrder() = runTest(dispatcher) {
        val vm = createViewModel()
        vm.onLocation(10.0, 76.0)
        vm.selectPlace(cafe)
        assertEquals(Sheet.Place, vm.state.value.sheet)
        assertTrue(vm.handleBack())
        assertEquals(Sheet.Explore, vm.state.value.sheet)
        assertEquals(false, vm.handleBack())
    }
}
