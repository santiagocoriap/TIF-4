package com.quakescope.data.remote.dto

data class EarthquakeResponseDto(
    val count: Int,
    val items: List<EarthquakeDto>
)
