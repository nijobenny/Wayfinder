package com.nijobenny.wayfinder.data.remote

import com.nijobenny.wayfinder.domain.model.LatLng
import com.nijobenny.wayfinder.domain.model.Maneuver
import com.nijobenny.wayfinder.domain.model.Route
import com.nijobenny.wayfinder.domain.model.TravelMode
import com.nijobenny.wayfinder.domain.util.Polyline
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Valhalla routing (FOSSGIS public server, OpenStreetMap data). Free, no key, max 1 request/second.
 * Returns turn-by-turn maneuvers and a polyline6-encoded shape per leg.
 */
class ValhallaApi(private val client: HttpClient) {
    suspend fun route(request: ValhallaRequest): ValhallaResponse =
        client.post("https://valhalla1.openstreetmap.de/route") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}

@Serializable
data class ValhallaRequest(
    val locations: List<ValhallaLocation>,
    val costing: String,
    @SerialName("costing_options") val costingOptions: Map<String, ValhallaCostingOptions> = emptyMap(),
    @SerialName("directions_options") val directionsOptions: ValhallaDirectionsOptions = ValhallaDirectionsOptions(),
)

@Serializable
data class ValhallaLocation(val lat: Double, val lon: Double)

@Serializable
data class ValhallaCostingOptions(
    @SerialName("use_tolls") val useTolls: Double? = null,
    @SerialName("use_highways") val useHighways: Double? = null,
)

@Serializable
data class ValhallaDirectionsOptions(val units: String = "kilometers", val language: String = "en-US")

@Serializable
data class ValhallaResponse(val trip: ValhallaTrip)

@Serializable
data class ValhallaTrip(
    val legs: List<ValhallaLeg> = emptyList(),
    val summary: ValhallaSummary = ValhallaSummary(),
)

@Serializable
data class ValhallaLeg(
    val shape: String = "",
    val maneuvers: List<ValhallaManeuver> = emptyList(),
)

@Serializable
data class ValhallaSummary(
    val length: Double = 0.0,
    val time: Double = 0.0,
    @SerialName("has_toll") val hasToll: Boolean = false,
    @SerialName("has_highway") val hasHighway: Boolean = false,
)

@Serializable
data class ValhallaManeuver(
    val type: Int = 0,
    val instruction: String = "",
    val length: Double = 0.0,
    val time: Double = 0.0,
    @SerialName("begin_shape_index") val beginShapeIndex: Int = 0,
)

/**
 * Joins all legs into one route. Each leg's shape indices are relative to that leg, so they are
 * shifted by the number of points before it. Intermediate "arrive" maneuvers become waypoints.
 */
fun ValhallaResponse.toRoute(mode: TravelMode): Route {
    val geometry = ArrayList<LatLng>()
    val maneuvers = ArrayList<Maneuver>()
    trip.legs.forEach { leg ->
        val offset = geometry.size
        geometry += Polyline.decode(leg.shape, precision = 6)
        leg.maneuvers.forEach { m ->
            maneuvers += Maneuver(
                type = m.type,
                instruction = m.instruction,
                distanceMeters = m.length * 1000,
                durationSeconds = m.time,
                beginIndex = m.beginShapeIndex + offset,
            )
        }
    }
    return Route(
        geometry = geometry,
        maneuvers = maneuvers,
        distanceMeters = trip.summary.length * 1000,
        durationSeconds = trip.summary.time,
        mode = mode,
        hasToll = trip.summary.hasToll,
        hasHighway = trip.summary.hasHighway,
    )
}
