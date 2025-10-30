package com.quakescope.data.remote.dto

import com.squareup.moshi.Json

data class EarthquakePairItemDto(
    @Json(name = "earthquake_id") val earthquakeId: String? = null,
    val real: EarthquakeDto? = null,
    val expected: EarthquakeDto? = null
)

data class EarthquakePairResponseDto(
    val count: Int,
    val items: List<EarthquakePairItemDto>
)
