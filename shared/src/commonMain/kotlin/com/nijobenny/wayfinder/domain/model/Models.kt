package com.nijobenny.wayfinder.domain.model

import kotlinx.serialization.Serializable

/** A coordinate. */
@Serializable
data class LatLng(val latitude: Double, val longitude: Double)

/** A named place, e.g. title = "Infopark Phase 1", subtitle = "Kakkanad, Kochi, Kerala". */
@Serializable
data class Place(
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    /** OpenStreetMap category such as "cafe" or "fuel", when known. */
    val category: String? = null,
) {
    val latLng: LatLng get() = LatLng(latitude, longitude)

    /** Stable identity for de-duplication (same name at roughly the same spot). */
    val key: String get() = "$title@${(latitude * 10_000).toLong()},${(longitude * 10_000).toLong()}"
}

/** A place the user saved, with an emoji label like 🏠 Home or 💼 Work. */
@Serializable
data class SavedPlace(val place: Place, val emoji: String = "⭐", val label: String = place.title)

enum class TravelMode(val label: String, val emoji: String, val costing: String, val demoSpeedMps: Double) {
    Car("Drive", "🚗", "auto", 22.0),
    Bike("Cycle", "🚲", "bicycle", 7.0),
    Walk("Walk", "🚶", "pedestrian", 2.2),
}

enum class Units { Metric, Imperial }

/** Quick "explore nearby" categories, mapped to OpenStreetMap tags for Photon. */
enum class Category(val label: String, val emoji: String, val osmTag: String, val query: String) {
    Coffee("Coffee", "☕", "amenity:cafe", "cafe"),
    Food("Food", "🍽️", "amenity:restaurant", "restaurant"),
    Fuel("Fuel", "⛽", "amenity:fuel", "fuel"),
    Atm("ATM", "🏧", "amenity:atm", "atm"),
    Hospital("Hospital", "🏥", "amenity:hospital", "hospital"),
    Hotel("Hotels", "🏨", "tourism:hotel", "hotel"),
}

/** One turn instruction. */
@Serializable
data class Maneuver(
    val type: Int,
    val instruction: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    /** Index into [Route.geometry] where this maneuver starts. */
    val beginIndex: Int,
) {
    val isArrival: Boolean get() = type in 4..6
}

/** A planned route through one or more stops. */
@Serializable
data class Route(
    val geometry: List<LatLng>,
    val maneuvers: List<Maneuver>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val mode: TravelMode,
    val hasToll: Boolean = false,
    val hasHighway: Boolean = false,
)

data class RouteOptions(val avoidTolls: Boolean = false, val avoidHighways: Boolean = false)

/** A completed (or demo) trip shown in the profile. */
@Serializable
data class TripRecord(
    val title: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val mode: TravelMode,
    val finishedAtMillis: Long,
    val stops: Int,
)

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val units: Units = Units.Metric,
    val defaultMode: TravelMode = TravelMode.Car,
    val avoidTolls: Boolean = false,
    val avoidHighways: Boolean = false,
    val displayName: String = "Nijo Benny",
    val demoSpeed: Int = 3,
)
