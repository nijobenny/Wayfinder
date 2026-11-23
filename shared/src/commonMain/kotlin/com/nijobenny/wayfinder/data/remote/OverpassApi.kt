package com.nijobenny.wayfinder.data.remote

import com.nijobenny.wayfinder.domain.model.LatLng
import com.nijobenny.wayfinder.domain.model.Place
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

/**
 * Overpass API (overpass-api.de): queries OpenStreetMap directly, so "nearby" really means nearby,
 * which a text search can't guarantee. Free, no API key.
 */
class OverpassApi(private val client: HttpClient) {

    /** Named places tagged [key]=[value] within [radiusMeters] of [center]. */
    suspend fun around(key: String, value: String, center: LatLng, radiusMeters: Int, limit: Int = 40): OverpassResponse {
        val query = "[out:json][timeout:20];" +
            "nwr[\"$key\"=\"$value\"][\"name\"](around:$radiusMeters,${center.latitude},${center.longitude});" +
            "out center $limit;"
        return client.get("https://overpass-api.de/api/interpreter") { parameter("data", query) }.body()
    }
}

@Serializable
data class OverpassResponse(val elements: List<OverpassElement> = emptyList())

@Serializable
data class OverpassElement(
    val id: Long = 0,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String> = emptyMap(),
)

@Serializable
data class OverpassCenter(val lat: Double, val lon: Double)

/** Converts an OSM element into a [Place], or null when it has no position or name. */
fun OverpassElement.toPlace(category: String): Place? {
    val latitude = lat ?: center?.lat ?: return null
    val longitude = lon ?: center?.lon ?: return null
    val name = tags["name:en"] ?: tags["name"] ?: return null
    val street = listOfNotNull(tags["addr:housenumber"], tags["addr:street"]).joinToString(" ").ifBlank { null }
    val subtitle = listOfNotNull(
        street,
        tags["addr:suburb"] ?: tags["addr:place"],
        tags["addr:city"],
        tags["cuisine"]?.replace('_', ' ')?.replace(";", ", ")?.replaceFirstChar { it.uppercase() },
    ).distinct().joinToString(" · ")
    return Place(title = name, subtitle = subtitle, latitude = latitude, longitude = longitude, category = category)
}
