package com.quakescope.domain.model

object SortOptionRules {
    private val genericOptions = listOf(
        SortOption.TIME_DESC,
        SortOption.TIME_ASC,
        SortOption.MAGNITUDE_DESC,
        SortOption.MAGNITUDE_ASC,
        SortOption.DEPTH_DESC,
        SortOption.DEPTH_ASC
    )

    private val pairOptions = genericOptions + listOf(
        SortOption.REAL_TIME_DESC,
        SortOption.REAL_TIME_ASC,
        SortOption.REAL_MAGNITUDE_DESC,
        SortOption.REAL_MAGNITUDE_ASC,
        SortOption.REAL_DEPTH_DESC,
        SortOption.REAL_DEPTH_ASC,
        SortOption.ESTIMATED_TIME_DESC,
        SortOption.ESTIMATED_TIME_ASC,
        SortOption.ESTIMATED_MAGNITUDE_DESC,
        SortOption.ESTIMATED_MAGNITUDE_ASC,
        SortOption.ESTIMATED_DEPTH_DESC,
        SortOption.ESTIMATED_DEPTH_ASC
    )

    fun availableFor(type: EarthquakeType): List<SortOption> = when (type) {
        EarthquakeType.PAIRS -> pairOptions
        EarthquakeType.REAL,
        EarthquakeType.ESTIMATED -> genericOptions
    }

    fun ensureAllowed(option: SortOption, type: EarthquakeType): SortOption {
        val available = availableFor(type)
        return if (available.contains(option)) {
            option
        } else {
            available.first()
        }
    }
}
