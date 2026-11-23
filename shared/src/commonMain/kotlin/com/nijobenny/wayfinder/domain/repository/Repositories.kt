package com.nijobenny.wayfinder.domain.repository

import com.nijobenny.wayfinder.domain.model.Category
import com.nijobenny.wayfinder.domain.model.LatLng
import com.nijobenny.wayfinder.domain.model.Place
import com.nijobenny.wayfinder.domain.model.Route
import com.nijobenny.wayfinder.domain.model.RouteOptions
import com.nijobenny.wayfinder.domain.model.SavedPlace
import com.nijobenny.wayfinder.domain.model.ThemeMode
import com.nijobenny.wayfinder.domain.model.TravelMode
import com.nijobenny.wayfinder.domain.model.TripRecord
import com.nijobenny.wayfinder.domain.model.Units
import com.nijobenny.wayfinder.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setUnits(units: Units)
    suspend fun setDefaultMode(mode: TravelMode)
    suspend fun setAvoidTolls(avoid: Boolean)
    suspend fun setAvoidHighways(avoid: Boolean)
    suspend fun setDisplayName(name: String)
    suspend fun setDemoSpeed(multiplier: Int)
}

interface GeocodingRepository {
    /** Finds the nearest named place for a coordinate. */
    suspend fun reverse(latitude: Double, longitude: Double): Result<Place>

    /** Autocomplete search, biased towards [near] when given. */
    suspend fun search(query: String, near: LatLng?): Result<List<Place>>

    /** Places of a [category] near [near]. */
    suspend fun nearby(category: Category, near: LatLng?): Result<List<Place>>
}

interface RoutingRepository {
    /** Route from the first point through every following point, in order. */
    suspend fun route(points: List<LatLng>, mode: TravelMode, options: RouteOptions): Result<Route>
}

/** Locally stored user data: saved places, recent searches and trip history. */
interface PlacesRepository {
    val savedPlaces: Flow<List<SavedPlace>>
    val recentSearches: Flow<List<Place>>
    val trips: Flow<List<TripRecord>>
    suspend fun toggleSaved(place: Place, emoji: String = "⭐")
    suspend fun addRecent(place: Place)
    suspend fun clearRecents()
    suspend fun addTrip(trip: TripRecord)
    suspend fun clearTrips()
}
