package com.nijobenny.wayfinder.data

import com.nijobenny.wayfinder.data.remote.PhotonFeature
import com.nijobenny.wayfinder.data.remote.PhotonGeometry
import com.nijobenny.wayfinder.data.remote.PhotonProperties
import com.nijobenny.wayfinder.data.remote.PhotonResponse
import com.nijobenny.wayfinder.data.remote.toPlace
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PhotonMappingTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test
    fun parsesGeoJsonAndBuildsReadablePlace() {
        val body = """
            {"type":"FeatureCollection","features":[{"type":"Feature",
              "geometry":{"type":"Point","coordinates":[76.3561,10.0101]},
              "properties":{"osm_id":1,"name":"Infopark Phase 1","district":"Kakkanad",
                            "city":"Kochi","state":"Kerala","country":"India","extra":"ignored"}}]}
        """.trimIndent()
        val place = json.decodeFromString<PhotonResponse>(body).features.single().toPlace(0.0, 0.0)

        assertEquals("Infopark Phase 1", place.title)
        assertEquals("Kakkanad, Kochi, Kerala, India", place.subtitle)
        assertEquals(10.0101, place.latitude)
        assertEquals(76.3561, place.longitude)
    }

    @Test
    fun withoutName_fallsBackToStreetAndCoordinates() {
        val feature = PhotonFeature(
            geometry = null,
            properties = PhotonProperties(street = "MG Road", housenumber = "12", city = "Kochi", country = "India"),
        )
        val place = feature.toPlace(9.97, 76.28)
        assertEquals("MG Road 12", place.title)
        assertEquals("Kochi, India", place.subtitle)
        assertEquals(9.97, place.latitude)
    }

    @Test
    fun emptyProperties_givesUnnamedPlace() {
        val place = PhotonFeature(geometry = PhotonGeometry(listOf(1.0, 2.0))).toPlace(0.0, 0.0)
        assertEquals("Unnamed place", place.title)
        assertEquals("", place.subtitle)
    }
}
