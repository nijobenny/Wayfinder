package com.nijobenny.wayfinder.domain.util

import com.nijobenny.wayfinder.domain.model.LatLng
import com.nijobenny.wayfinder.domain.model.Maneuver
import com.nijobenny.wayfinder.domain.model.Route
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sqrt

/** Where the traveller is along a route, and what's next. */
data class RouteProgress(
    val position: LatLng,
    val bearing: Double,
    val traveledMeters: Double,
    val remainingMeters: Double,
    val remainingSeconds: Double,
    val nextManeuver: Maneuver?,
    val distanceToNextManeuver: Double,
    val fraction: Float,
    val arrived: Boolean,
)

/**
 * Precomputes cumulative distances along a route so progress, interpolation and
 * "next turn" lookups are fast enough to run every animation frame.
 */
class RouteTracker(val route: Route) {
    private val points = route.geometry
    private val cumulative: DoubleArray = DoubleArray(points.size).also { acc ->
        for (i in 1 until points.size) {
            acc[i] = acc[i - 1] + GeoMath.distanceMeters(points[i - 1].latitude, points[i - 1].longitude, points[i].latitude, points[i].longitude)
        }
    }
    val totalMeters: Double = cumulative.lastOrNull() ?: 0.0

    private fun distanceAtIndex(index: Int): Double = cumulative[index.coerceIn(0, cumulative.lastIndex.coerceAtLeast(0))]

    /** Position and heading after travelling [meters] from the start. */
    fun progressAt(meters: Double): RouteProgress {
        val d = meters.coerceIn(0.0, totalMeters)
        var i = 0
        while (i < cumulative.lastIndex && cumulative[i + 1] < d) i++
        val a = points.getOrElse(i) { points.last() }
        val b = points.getOrElse(i + 1) { a }
        val segment = (cumulative.getOrElse(i + 1) { d } - cumulative[i]).takeIf { it > 0 } ?: 1.0
        val t = ((d - cumulative[i]) / segment).coerceIn(0.0, 1.0)
        val position = LatLng(a.latitude + (b.latitude - a.latitude) * t, a.longitude + (b.longitude - a.longitude) * t)
        return build(position, bearing(a, b), d)
    }

    /** Snaps a GPS fix onto the route (nearest segment) and returns progress from there. */
    fun progressNear(location: LatLng): RouteProgress {
        if (points.size < 2) return progressAt(0.0)
        var best = 0.0
        var bestDistance = Double.MAX_VALUE
        val cosLat = cos(location.latitude * kotlin.math.PI / 180)
        for (i in 0 until points.lastIndex) {
            val a = points[i]
            val b = points[i + 1]
            // Equirectangular projection is accurate enough at street scale.
            val ax = a.longitude * cosLat; val ay = a.latitude
            val bx = b.longitude * cosLat; val by = b.latitude
            val px = location.longitude * cosLat; val py = location.latitude
            val dx = bx - ax; val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 == 0.0) 0.0 else (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0.0, 1.0)
            val qx = ax + t * dx; val qy = ay + t * dy
            val dist = sqrt((px - qx) * (px - qx) + (py - qy) * (py - qy))
            if (dist < bestDistance) {
                bestDistance = dist
                best = cumulative[i] + (cumulative[i + 1] - cumulative[i]) * t
            }
        }
        return progressAt(best).copy(position = location)
    }

    /** The part of the route still ahead of [meters]. */
    fun remainingGeometry(meters: Double): List<LatLng> {
        val current = progressAt(meters).position
        val ahead = points.indices.filter { cumulative[it] > meters }.map { points[it] }
        return listOf(current) + ahead
    }

    /** The route up to [fraction] of its length (used to animate the line being drawn). */
    fun partialGeometry(fraction: Float): List<LatLng> {
        if (fraction >= 1f) return points
        val meters = totalMeters * fraction
        val done = points.indices.filter { cumulative[it] <= meters }.map { points[it] }
        return done + progressAt(meters).position
    }

    private fun build(position: LatLng, bearing: Double, traveled: Double): RouteProgress {
        val next = route.maneuvers.firstOrNull { distanceAtIndex(it.beginIndex) > traveled + 1.0 }
        val remaining = (totalMeters - traveled).coerceAtLeast(0.0)
        val ratio = if (totalMeters > 0) remaining / totalMeters else 0.0
        return RouteProgress(
            position = position,
            bearing = bearing,
            traveledMeters = traveled,
            remainingMeters = remaining,
            remainingSeconds = route.durationSeconds * ratio,
            nextManeuver = next,
            distanceToNextManeuver = next?.let { distanceAtIndex(it.beginIndex) - traveled } ?: remaining,
            fraction = if (totalMeters > 0) (traveled / totalMeters).toFloat() else 1f,
            arrived = remaining < 15.0,
        )
    }

    companion object {
        /** Compass bearing from [a] to [b] in degrees (0 = north, clockwise). */
        fun bearing(a: LatLng, b: LatLng): Double {
            val lat1 = a.latitude * kotlin.math.PI / 180
            val lat2 = b.latitude * kotlin.math.PI / 180
            val dLon = (b.longitude - a.longitude) * kotlin.math.PI / 180
            val y = kotlin.math.sin(dLon) * cos(lat2)
            val x = cos(lat1) * kotlin.math.sin(lat2) - kotlin.math.sin(lat1) * cos(lat2) * cos(dLon)
            return (atan2(y, x) * 180 / kotlin.math.PI + 360) % 360
        }
    }
}
