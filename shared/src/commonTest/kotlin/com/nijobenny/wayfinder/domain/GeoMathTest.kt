package com.nijobenny.wayfinder.domain

import com.nijobenny.wayfinder.domain.model.ThemeMode
import com.nijobenny.wayfinder.domain.util.GeoMath
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeoMathTest {

    @Test
    fun distance_kochiToBengaluru_isAbout360km() {
        val meters = GeoMath.distanceMeters(9.9816, 76.2999, 12.9716, 77.5946)
        assertTrue(abs(meters - 357_000) < 10_000, "was $meters")
    }

    @Test
    fun distance_samePoint_isZero() {
        assertEquals(0.0, GeoMath.distanceMeters(10.0, 76.0, 10.0, 76.0))
    }

    @Test
    fun formatCoordinates_usesHemispheres() {
        assertEquals("9.9816° N, 76.2999° E", GeoMath.formatCoordinates(9.9816, 76.2999))
        assertEquals("33.8688° S, 151.2093° E", GeoMath.formatCoordinates(-33.8688, 151.2093))
        assertEquals("40.7128° N, 74.0060° W", GeoMath.formatCoordinates(40.7128, -74.006))
    }

    @Test
    fun themeMode_cyclesThroughAllModes() {
        assertEquals(ThemeMode.Light, ThemeMode.System.next())
        assertEquals(ThemeMode.Dark, ThemeMode.Light.next())
        assertEquals(ThemeMode.System, ThemeMode.Dark.next())
    }
}
