package com.nijobenny.wayfinder.domain

import com.nijobenny.wayfinder.domain.model.LatLng
import com.nijobenny.wayfinder.domain.model.Maneuver
import com.nijobenny.wayfinder.domain.model.Route
import com.nijobenny.wayfinder.domain.model.TravelMode
import com.nijobenny.wayfinder.domain.model.Units
import com.nijobenny.wayfinder.domain.util.Format
import com.nijobenny.wayfinder.domain.util.Polyline
import com.nijobenny.wayfinder.domain.util.RouteTracker
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutingMathTest {

    @Test
    fun polyline_decodesGoogleReferenceExample() {
        // Example from Google's polyline algorithm documentation (precision 5).
        val points = Polyline.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@", precision = 5)
        assertEquals(listOf(LatLng(38.5, -120.2), LatLng(40.7, -120.95), LatLng(43.252, -126.453)), points)
    }

    private val route = Route(
        geometry = (0..10).map { LatLng(10.0 + it * 0.001, 76.0) },
        maneuvers = listOf(Maneuver(1, "Start", 0.0, 0.0, 0), Maneuver(10, "Turn right", 0.0, 0.0, 5), Maneuver(4, "Arrive", 0.0, 0.0, 10)),
        distanceMeters = 1112.0,
        durationSeconds = 100.0,
        mode = TravelMode.Car,
    )

    @Test
    fun tracker_interpolatesAndFindsNextTurn() {
        val tracker = RouteTracker(route)
        assertTrue(abs(tracker.totalMeters - 1112) < 5)

        val halfway = tracker.progressAt(tracker.totalMeters / 2)
        assertTrue(abs(halfway.position.latitude - 10.005) < 0.0001)
        assertEquals("Arrive", halfway.nextManeuver?.instruction)

        val early = tracker.progressAt(100.0)
        assertEquals("Turn right", early.nextManeuver?.instruction)
        assertTrue(abs(early.bearing - 0.0) < 1.0) // heading north
        assertTrue(abs(early.remainingSeconds - 91.0) < 2.0)
    }

    @Test
    fun tracker_snapsNearbyGpsFixOntoRoute() {
        val tracker = RouteTracker(route)
        val progress = tracker.progressNear(LatLng(10.003, 76.0002)) // ~22 m east of the line
        assertTrue(abs(progress.traveledMeters - 333.6) < 5, "was ${progress.traveledMeters}")
    }

    @Test
    fun tracker_arrivesNearTheEnd() {
        assertTrue(RouteTracker(route).progressAt(1110.0).arrived)
    }

    @Test
    fun format_distanceAndDuration() {
        assertEquals("850 m", Format.distance(849.0, Units.Metric))
        assertEquals("1.2 km", Format.distance(1234.0, Units.Metric))
        assertEquals("42 km", Format.distance(42_100.0, Units.Metric))
        assertEquals("0.8 mi", Format.distance(1234.0, Units.Imperial))
        assertEquals("12 min", Format.duration(720.0))
        assertEquals("1 h 05 min", Format.duration(3900.0))
        assertEquals("45 s", Format.duration(45.0))
    }
}
