package com.quakescope.domain.model

import java.time.Instant

data class Earthquake(
    val id: String,
    val earthquakeId: String,
    val source: String,
    val isReal: Boolean,
    val magnitude: Double,
    val depth: Double,
    val latitude: Double,
    val longitude: Double,
    val originalLatitude: Double?,
    val originalLongitude: Double?,
    val place: String?,
    val radiusKm: Double,
    val time: Instant
)
