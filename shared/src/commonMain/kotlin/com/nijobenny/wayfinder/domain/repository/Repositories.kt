package com.nijobenny.wayfinder.domain.repository

import com.nijobenny.wayfinder.domain.model.Place
import com.nijobenny.wayfinder.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}

interface GeocodingRepository {
    /** Finds the nearest named place for a coordinate. */
    suspend fun reverse(latitude: Double, longitude: Double): Result<Place>
}
