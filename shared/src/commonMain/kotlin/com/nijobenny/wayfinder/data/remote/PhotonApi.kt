package com.nijobenny.wayfinder.data.remote

import com.nijobenny.wayfinder.domain.model.Place
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

/** Photon (photon.komoot.io): free OpenStreetMap geocoding, no API key. Responses are GeoJSON. */
class PhotonApi(private val client: HttpClient) {

    suspend fun reverse(latitude: Double, longitude: Double): PhotonResponse =
        client.get("https://photon.komoot.io/reverse") {
            parameter("lat", latitude)
            parameter("lon", longitude)
            parameter("limit", 1)
        }.body()
}

@Serializable
data class PhotonResponse(val features: List<PhotonFeature> = emptyList())

@Serializable
data class PhotonFeature(
    val geometry: PhotonGeometry? = null,
    val properties: PhotonProperties = PhotonProperties(),
)

@Serializable
data class PhotonGeometry(val coordinates: List<Double> = emptyList())

@Serializable
data class PhotonProperties(
    val name: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val district: String? = null,
    val locality: String? = null,
    val city: String? = null,
    val county: String? = null,
    val state: String? = null,
    val country: String? = null,
)

/** Turns a Photon feature into a readable [Place]. Falls back to the queried coordinate. */
fun PhotonFeature.toPlace(fallbackLatitude: Double, fallbackLongitude: Double): Place {
    val p = properties
    val street = listOfNotNull(p.street, p.housenumber).joinToString(" ").ifBlank { null }
    val title = p.name ?: street ?: p.locality ?: p.district ?: p.city ?: p.county ?: "Unnamed place"
    val subtitle = listOfNotNull(p.locality ?: p.district, p.city ?: p.county, p.state, p.country)
        .distinct()
        .filter { it != title }
        .joinToString(", ")
    val coords = geometry?.coordinates
    return Place(
        title = title,
        subtitle = subtitle,
        latitude = coords?.getOrNull(1) ?: fallbackLatitude,
        longitude = coords?.getOrNull(0) ?: fallbackLongitude,
    )
}
