package com.nijobenny.wayfinder.domain.util

import com.nijobenny.wayfinder.domain.model.LatLng

/** Decodes Google's encoded polyline format. Valhalla uses precision 6. */
object Polyline {
    fun decode(encoded: String, precision: Int = 6): List<LatLng> {
        val factor = generateSequence(1.0) { it * 10 }.elementAt(precision)
        val result = ArrayList<LatLng>()
        var index = 0
        var lat = 0L
        var lon = 0L
        while (index < encoded.length) {
            var shift = 0
            var value = 0L
            var byte: Int
            do {
                byte = encoded[index++].code - 63
                value = value or ((byte and 0x1F).toLong() shl shift)
                shift += 5
            } while (byte >= 0x20)
            lat += if ((value and 1L) != 0L) (value shr 1).inv() else value shr 1

            shift = 0
            value = 0L
            do {
                byte = encoded[index++].code - 63
                value = value or ((byte and 0x1F).toLong() shl shift)
                shift += 5
            } while (byte >= 0x20)
            lon += if ((value and 1L) != 0L) (value shr 1).inv() else value shr 1

            result += LatLng(lat / factor, lon / factor)
        }
        return result
    }
}
