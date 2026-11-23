package com.nijobenny.wayfinder.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nijobenny.wayfinder.domain.model.ThemeMode
import com.nijobenny.wayfinder.domain.model.TravelMode
import com.nijobenny.wayfinder.domain.model.Units
import com.nijobenny.wayfinder.domain.model.UserSettings
import com.nijobenny.wayfinder.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

fun createPreferencesDataStore(path: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { path.toPath() })

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: default

/** Persists user settings with Jetpack DataStore (shared across Android and iOS). */
class DataStoreSettingsRepository(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val units = stringPreferencesKey("units")
        val mode = stringPreferencesKey("default_mode")
        val tolls = booleanPreferencesKey("avoid_tolls")
        val highways = booleanPreferencesKey("avoid_highways")
        val name = stringPreferencesKey("display_name")
        val demoSpeed = intPreferencesKey("demo_speed")
    }

    override val settings: Flow<UserSettings> = dataStore.data.map { p ->
        val defaults = UserSettings()
        UserSettings(
            themeMode = p[Keys.theme].toEnum(defaults.themeMode),
            units = p[Keys.units].toEnum(defaults.units),
            defaultMode = p[Keys.mode].toEnum(defaults.defaultMode),
            avoidTolls = p[Keys.tolls] ?: defaults.avoidTolls,
            avoidHighways = p[Keys.highways] ?: defaults.avoidHighways,
            displayName = p[Keys.name] ?: defaults.displayName,
            demoSpeed = p[Keys.demoSpeed] ?: defaults.demoSpeed,
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) { dataStore.edit { it[Keys.theme] = mode.name } }
    override suspend fun setUnits(units: Units) { dataStore.edit { it[Keys.units] = units.name } }
    override suspend fun setDefaultMode(mode: TravelMode) { dataStore.edit { it[Keys.mode] = mode.name } }
    override suspend fun setAvoidTolls(avoid: Boolean) { dataStore.edit { it[Keys.tolls] = avoid } }
    override suspend fun setAvoidHighways(avoid: Boolean) { dataStore.edit { it[Keys.highways] = avoid } }
    override suspend fun setDisplayName(name: String) { dataStore.edit { it[Keys.name] = name.trim().ifEmpty { UserSettings().displayName } } }
    override suspend fun setDemoSpeed(multiplier: Int) { dataStore.edit { it[Keys.demoSpeed] = multiplier } }
}
