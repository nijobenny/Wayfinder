package com.nijobenny.wayfinder.data

import com.nijobenny.wayfinder.data.remote.OverpassResponse
import com.nijobenny.wayfinder.data.remote.toPlace
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OverpassMappingTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test
    fun parsesNodesAndWays_andSkipsUnnamed() {
        val body = """
            {"elements":[
              {"type":"node","id":1,"lat":9.68,"lon":76.33,"tags":{"amenity":"restaurant","name":"Kayal Kitchen","addr:street":"NH66","addr:city":"Cherthala","cuisine":"kerala;seafood"}},
              {"type":"way","id":2,"center":{"lat":9.69,"lon":76.34},"tags":{"amenity":"restaurant","name":"Hotel Aryas"}},
              {"type":"node","id":3,"lat":9.70,"lon":76.35,"tags":{"amenity":"restaurant"}}
            ]}
        """.trimIndent()
        val elements = json.decodeFromString<OverpassResponse>(body).elements
        val first = elements[0].toPlace("restaurant")!!
        assertEquals("Kayal Kitchen", first.title)
        assertEquals("NH66 · Cherthala · Kerala, seafood", first.subtitle)
        assertEquals(9.69, elements[1].toPlace("restaurant")!!.latitude)
        assertNull(elements[2].toPlace("restaurant"))
    }
}
