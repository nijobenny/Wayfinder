package com.nijobenny.wayfinder.di

import com.nijobenny.wayfinder.data.remote.OverpassApi
import com.nijobenny.wayfinder.data.remote.PhotonApi
import com.nijobenny.wayfinder.data.remote.ValhallaApi
import com.nijobenny.wayfinder.data.remote.createHttpClient
import com.nijobenny.wayfinder.data.repository.PhotonGeocodingRepository
import com.nijobenny.wayfinder.data.repository.ValhallaRoutingRepository
import com.nijobenny.wayfinder.data.settings.DataStorePlacesRepository
import com.nijobenny.wayfinder.data.settings.DataStoreSettingsRepository
import com.nijobenny.wayfinder.data.settings.createPreferencesDataStore
import com.nijobenny.wayfinder.domain.repository.GeocodingRepository
import com.nijobenny.wayfinder.domain.repository.PlacesRepository
import com.nijobenny.wayfinder.domain.repository.RoutingRepository
import com.nijobenny.wayfinder.domain.repository.SettingsRepository
import com.nijobenny.wayfinder.platform.currentTimeMillis
import com.nijobenny.wayfinder.platform.dataStoreFilePath
import com.nijobenny.wayfinder.ui.map.MapViewModel
import com.nijobenny.wayfinder.ui.profile.ProfileViewModel
import com.nijobenny.wayfinder.ui.search.SearchViewModel
import com.nijobenny.wayfinder.ui.settings.SettingsViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val dataModule = module {
    single { createPreferencesDataStore(dataStoreFilePath()) }
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }
    single<PlacesRepository> { DataStorePlacesRepository(get()) }

    single { createHttpClient() }
    single { PhotonApi(get()) }
    single { ValhallaApi(get()) }
    single { OverpassApi(get()) }
    single<GeocodingRepository> { PhotonGeocodingRepository(get(), get()) }
    single<RoutingRepository> { ValhallaRoutingRepository(get(), ::currentTimeMillis) }
}

val uiModule = module {
    viewModel { MapViewModel(get(), get(), get(), get(), ::currentTimeMillis) }
    viewModel { SearchViewModel(get(), get()) }
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
}

private var koinStarted = false

/** Starts Koin once per process (from Android's MainActivity and iOS's MainViewController). */
fun initKoin() {
    if (koinStarted) return
    startKoin { modules(dataModule, uiModule) }
    koinStarted = true
}
