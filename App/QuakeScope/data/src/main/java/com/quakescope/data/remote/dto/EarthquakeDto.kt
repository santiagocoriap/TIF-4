package com.quakescope.data.remote.dto

import com.squareup.moshi.Json

data class EarthquakeDto(
    val id: String? = null,
    @Json(name = "earthquake_id") val earthquakeId: String? = null,
    val source: String? = null,
    val magnitude: Double? = null,
    val depth: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "original_latitude") val originalLatitude: Double? = null,
    @Json(name = "original_longitude") val originalLongitude: Double? = null,
    val place: String? = null,
    @Json(name = "radius_km") val radiusKm: Double? = null,
    @Json(name = "time_ms") val timeMs: Long? = null,
    val time: String? = null
)
