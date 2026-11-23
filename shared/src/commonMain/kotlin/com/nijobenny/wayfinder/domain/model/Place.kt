package com.nijobenny.wayfinder.domain.model

/** A named place, e.g. title = "Infopark Phase 1", subtitle = "Kakkanad, Kochi, Kerala". */
data class Place(
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
)
