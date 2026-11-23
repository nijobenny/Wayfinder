package com.nijobenny.wayfinder.di

import com.nijobenny.wayfinder.data.remote.PhotonApi
import com.nijobenny.wayfinder.data.remote.createHttpClient
import com.nijobenny.wayfinder.data.repository.PhotonGeocodingRepository
import com.nijobenny.wayfinder.data.settings.DataStoreSettingsRepository
import com.nijobenny.wayfinder.data.settings.createPreferencesDataStore
import com.nijobenny.wayfinder.domain.repository.GeocodingRepository
import com.nijobenny.wayfinder.domain.repository.SettingsRepository
import com.nijobenny.wayfinder.platform.dataStoreFilePath
import com.nijobenny.wayfinder.ui.map.MapViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val dataModule = module {
    single { createPreferencesDataStore(dataStoreFilePath()) }
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }

    single { createHttpClient() }
    single { PhotonApi(get()) }
    single<GeocodingRepository> { PhotonGeocodingRepository(get()) }
}

val uiModule = module {
    viewModel { MapViewModel(settings = get(), geocoding = get()) }
}

private var koinStarted = false

/** Starts Koin once per process (from Android's MainActivity and iOS's MainViewController). */
fun initKoin() {
    if (koinStarted) return
    startKoin { modules(dataModule, uiModule) }
    koinStarted = true
}
