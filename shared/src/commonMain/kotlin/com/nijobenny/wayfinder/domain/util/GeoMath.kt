package com.nijobenny.wayfinder.domain.util

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoMath {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /** Great-circle distance between two coordinates in meters (haversine formula). */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = (lat2 - lat1).toRadians()
        val dLon = (lon2 - lon1).toRadians()
        val a = sin(dLat / 2).pow(2) + cos(lat1.toRadians()) * cos(lat2.toRadians()) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
    }

    /** "12.97° N, 77.59° E" */
    fun formatCoordinates(latitude: Double, longitude: Double): String {
        val lat = "${latitude.absRounded(4)}° ${if (latitude >= 0) "N" else "S"}"
        val lon = "${longitude.absRounded(4)}° ${if (longitude >= 0) "E" else "W"}"
        return "$lat, $lon"
    }

    private fun Double.toRadians() = this * kotlin.math.PI / 180.0

    private fun Double.absRounded(decimals: Int): String {
        val factor = 10.0.pow(decimals)
        val scaled = kotlin.math.round(kotlin.math.abs(this) * factor).toLong()
        val whole = scaled / factor.toLong()
        val fraction = (scaled % factor.toLong()).toString().padStart(decimals, '0')
        return "$whole.$fraction"
    }
}
