package com.nijobenny.wayfinder.data.repository

import com.nijobenny.wayfinder.data.remote.OverpassApi
import com.nijobenny.wayfinder.data.remote.PhotonApi
import com.nijobenny.wayfinder.data.remote.toPlace
import com.nijobenny.wayfinder.domain.model.Category
import com.nijobenny.wayfinder.domain.model.LatLng
import com.nijobenny.wayfinder.domain.model.Place
import com.nijobenny.wayfinder.domain.repository.GeocodingRepository
import com.nijobenny.wayfinder.domain.util.GeoMath
import kotlinx.coroutines.CancellationException

/**
 * Search and reverse geocoding with Photon; "nearby" categories with Overpass
 * (real radius search), widening the radius when the area is quiet.
 */
class PhotonGeocodingRepository(
    private val api: PhotonApi,
    private val overpass: OverpassApi,
) : GeocodingRepository {

    private val reverseCache = mutableMapOf<Pair<Long, Long>, Place>()

    override suspend fun reverse(latitude: Double, longitude: Double): Result<Place> {
        val cell = (latitude * 1000).toLong() to (longitude * 1000).toLong()
        reverseCache[cell]?.let { return Result.success(it) }
        return safe {
            val feature = api.reverse(latitude, longitude).features.firstOrNull()
                ?: throw NoSuchElementException("No place found")
            feature.toPlace(latitude, longitude).also { reverseCache[cell] = it }
        }
    }

    override suspend fun search(query: String, near: LatLng?): Result<List<Place>> = safe {
        api.search(query, near).features.map { it.toPlace(0.0, 0.0) }.distinctBy { it.key }
    }

    override suspend fun nearby(category: Category, near: LatLng?): Result<List<Place>> = safe {
        if (near == null) {
            return@safe api.search(category.query, null, osmTag = category.osmTag, limit = 15).features
                .map { it.toPlace(0.0, 0.0).copy(category = category.query) }
        }
        val (key, value) = category.osmTag.split(":", limit = 2)
        var found = emptyList<Place>()
        for (radius in RADII) {
            found = overpass.around(key, value, near, radius).elements
                .mapNotNull { it.toPlace(category.query) }
                .distinctBy { it.title + it.latitude.toString().take(7) }
            if (found.size >= MIN_RESULTS) break
        }
        found.sortedBy { GeoMath.distanceMeters(near.latitude, near.longitude, it.latitude, it.longitude) }.take(25)
    }

    private inline fun <T> safe(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private companion object {
        val RADII = listOf(2_000, 6_000, 15_000)
        const val MIN_RESULTS = 6
    }
}
