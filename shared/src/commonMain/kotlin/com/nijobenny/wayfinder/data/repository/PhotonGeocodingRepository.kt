package com.nijobenny.wayfinder.data.repository

import com.nijobenny.wayfinder.data.remote.PhotonApi
import com.nijobenny.wayfinder.data.remote.toPlace
import com.nijobenny.wayfinder.domain.model.Place
import com.nijobenny.wayfinder.domain.repository.GeocodingRepository
import kotlinx.coroutines.CancellationException

/** Reverse-geocodes with Photon and caches results per ~100 m cell to avoid repeat calls. */
class PhotonGeocodingRepository(private val api: PhotonApi) : GeocodingRepository {

    private val cache = mutableMapOf<Pair<Long, Long>, Place>()

    override suspend fun reverse(latitude: Double, longitude: Double): Result<Place> {
        val cell = (latitude * 1000).toLong() to (longitude * 1000).toLong()
        cache[cell]?.let { return Result.success(it) }
        return try {
            val feature = api.reverse(latitude, longitude).features.firstOrNull()
                ?: return Result.failure(NoSuchElementException("No place found"))
            val place = feature.toPlace(latitude, longitude)
            cache[cell] = place
            Result.success(place)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
