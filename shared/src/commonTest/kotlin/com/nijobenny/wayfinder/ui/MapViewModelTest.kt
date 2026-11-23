package com.nijobenny.wayfinder.ui

import com.nijobenny.wayfinder.domain.model.Place
import com.nijobenny.wayfinder.domain.model.ThemeMode
import com.nijobenny.wayfinder.domain.repository.GeocodingRepository
import com.nijobenny.wayfinder.domain.repository.SettingsRepository
import com.nijobenny.wayfinder.ui.map.MapViewModel
import com.nijobenny.wayfinder.ui.map.PlaceUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private class FakeSettings : SettingsRepository {
        override val themeMode = MutableStateFlow(ThemeMode.System)
        override suspend fun setThemeMode(mode: ThemeMode) { themeMode.value = mode }
    }

    private class FakeGeocoding(var fail: Boolean = false) : GeocodingRepository {
        var calls = 0
        override suspend fun reverse(latitude: Double, longitude: Double): Result<Place> {
            calls++
            return if (fail) Result.failure(Exception("offline"))
            else Result.success(Place("Place $calls", "Kochi", latitude, longitude))
        }
    }

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun cycleTheme_persistsNextMode() = runTest(dispatcher) {
        val settings = FakeSettings()
        val viewModel = MapViewModel(settings, FakeGeocoding())

        viewModel.cycleThemeMode()
        assertEquals(ThemeMode.Light, viewModel.themeMode.value)
        viewModel.cycleThemeMode()
        assertEquals(ThemeMode.Dark, settings.themeMode.value)
    }

    @Test
    fun smallMovements_doNotTriggerNewLookups() = runTest(dispatcher) {
        val geocoding = FakeGeocoding()
        val viewModel = MapViewModel(FakeSettings(), geocoding)

        viewModel.onLocation(10.0000, 76.0000)
        viewModel.onLocation(10.0005, 76.0000) // ~55 m away
        assertEquals(1, geocoding.calls)

        viewModel.onLocation(10.0050, 76.0000) // ~550 m away
        assertEquals(2, geocoding.calls)
        assertEquals("Place 2", (viewModel.place.value as PlaceUiState.Ready).place.title)
    }

    @Test
    fun failedLookup_showsErrorAndRetriesOnNextFix() = runTest(dispatcher) {
        val geocoding = FakeGeocoding(fail = true)
        val viewModel = MapViewModel(FakeSettings(), geocoding)

        viewModel.onLocation(10.0, 76.0)
        assertIs<PlaceUiState.Error>(viewModel.place.value)

        geocoding.fail = false
        viewModel.onLocation(10.0001, 76.0) // tiny move, but retried because the last one failed
        assertIs<PlaceUiState.Ready>(viewModel.place.value)
    }
}
