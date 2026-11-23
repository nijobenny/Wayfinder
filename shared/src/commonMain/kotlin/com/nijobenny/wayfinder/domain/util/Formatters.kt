package com.nijobenny.wayfinder.domain.util

import com.nijobenny.wayfinder.domain.model.Units
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object Format {
    /** 850 m · 12.4 km · 0.5 mi · 300 ft */
    fun distance(meters: Double, units: Units): String = when (units) {
        Units.Metric -> when {
            meters < 1000 -> "${(meters / 10).roundToInt() * 10} m"
            meters < 10_000 -> "${oneDecimal(meters / 1000)} km"
            else -> "${(meters / 1000).roundToInt()} km"
        }
        Units.Imperial -> {
            val miles = meters / 1609.344
            if (miles < 0.1) "${(meters * 3.28084 / 10).roundToInt() * 10} ft"
            else if (miles < 10) "${oneDecimal(miles)} mi"
            else "${miles.roundToInt()} mi"
        }
    }

    /** 45 s · 12 min · 1 h 05 min */
    fun duration(seconds: Double): String {
        val totalMinutes = (seconds / 60).roundToLong()
        return when {
            seconds < 60 -> "${seconds.roundToInt()} s"
            totalMinutes < 60 -> "$totalMinutes min"
            else -> "${totalMinutes / 60} h ${(totalMinutes % 60).toString().padStart(2, '0')} min"
        }
    }

    private fun oneDecimal(value: Double): String {
        val tenths = (value * 10).roundToLong()
        return "${tenths / 10}.${tenths % 10}"
    }
}
