package com.quakescope.data.mapper

import com.quakescope.data.local.db.EarthquakeEntity
import com.quakescope.domain.model.Earthquake
import java.time.Instant

fun EarthquakeEntity.toDomain(): Earthquake {
    return Earthquake(
        id = id,
        earthquakeId = earthquakeId,
        source = source,
        isReal = isReal,
        magnitude = magnitude,
        depth = depth,
        latitude = latitude,
        longitude = longitude,
        originalLatitude = originalLatitude,
        originalLongitude = originalLongitude,
        place = place,
        radiusKm = radiusKm,
        time = Instant.ofEpochMilli(timeMs)
    )
}
