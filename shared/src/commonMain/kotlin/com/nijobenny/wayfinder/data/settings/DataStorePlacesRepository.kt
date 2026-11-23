package com.nijobenny.wayfinder.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nijobenny.wayfinder.domain.model.Place
import com.nijobenny.wayfinder.domain.model.SavedPlace
import com.nijobenny.wayfinder.domain.model.TripRecord
import com.nijobenny.wayfinder.domain.repository.PlacesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Saved places, recent searches and trips stored as JSON in DataStore. */
class DataStorePlacesRepository(private val dataStore: DataStore<Preferences>) : PlacesRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val savedKey = stringPreferencesKey("saved_places")
    private val recentKey = stringPreferencesKey("recent_searches")
    private val tripsKey = stringPreferencesKey("trips")

    private val savedSerializer = ListSerializer(SavedPlace.serializer())
    private val placeSerializer = ListSerializer(Place.serializer())
    private val tripSerializer = ListSerializer(TripRecord.serializer())

    private fun <T> decode(raw: String?, serializer: KSerializer<List<T>>): List<T> =
        raw?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }.orEmpty()

    override val savedPlaces: Flow<List<SavedPlace>> = dataStore.data.map { decode(it[savedKey], savedSerializer) }
    override val recentSearches: Flow<List<Place>> = dataStore.data.map { decode(it[recentKey], placeSerializer) }
    override val trips: Flow<List<TripRecord>> = dataStore.data.map { decode(it[tripsKey], tripSerializer) }

    override suspend fun toggleSaved(place: Place, emoji: String) {
        dataStore.edit { prefs ->
            val current = decode(prefs[savedKey], savedSerializer)
            val updated = if (current.any { it.place.key == place.key }) current.filterNot { it.place.key == place.key }
            else listOf(SavedPlace(place, emoji)) + current
            prefs[savedKey] = json.encodeToString(savedSerializer, updated)
        }
    }

    override suspend fun addRecent(place: Place) {
        dataStore.edit { prefs ->
            val current = decode(prefs[recentKey], placeSerializer)
            val updated = (listOf(place) + current.filterNot { it.key == place.key }).take(MAX_RECENTS)
            prefs[recentKey] = json.encodeToString(placeSerializer, updated)
        }
    }

    override suspend fun clearRecents() {
        dataStore.edit { it.remove(recentKey) }
    }

    override suspend fun addTrip(trip: TripRecord) {
        dataStore.edit { prefs ->
            val updated = (listOf(trip) + decode(prefs[tripsKey], tripSerializer)).take(MAX_TRIPS)
            prefs[tripsKey] = json.encodeToString(tripSerializer, updated)
        }
    }

    override suspend fun clearTrips() {
        dataStore.edit { it.remove(tripsKey) }
    }

    private companion object {
        const val MAX_RECENTS = 8
        const val MAX_TRIPS = 30
    }
}
