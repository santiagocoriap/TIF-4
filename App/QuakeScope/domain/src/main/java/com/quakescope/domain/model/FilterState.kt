package com.quakescope.domain.model

import com.quakescope.domain.model.EarthquakeType

data class FilterState(
    @Deprecated("Use realMagnitudeRange and estimatedMagnitudeRange for PAIRS")
    val magnitudeRange: ClosedFloatingPointRange<Float> = 4f..10f,
    @Deprecated("Use realDepthRange and estimatedDepthRange for PAIRS")
    val depthRange: ClosedFloatingPointRange<Float> = 0f..70f,

    val realMagnitudeRange: ClosedFloatingPointRange<Float> = 4f..10f,
    val estimatedMagnitudeRange: ClosedFloatingPointRange<Float> = 4f..10f,
    val realDepthRange: ClosedFloatingPointRange<Float> = 0f..70f,
    val estimatedDepthRange: ClosedFloatingPointRange<Float> = 0f..70f,

    val sortOption: SortOption = SortOption.TIME_DESC,
    val limit: Int = 20,
    val earthquakeType: EarthquakeType = EarthquakeType.PAIRS
)
