package com.quakescope.data.mapper

import com.quakescope.data.local.db.EarthquakeEntity
import com.quakescope.data.remote.dto.EarthquakeDto
import com.quakescope.domain.model.Earthquake
import java.util.UUID

fun EarthquakeDto.toEntity(isReal: Boolean): EarthquakeEntity {
    val resolvedEarthquakeId = if (isReal) {
        earthquakeId?.takeUnless { it.isBlank() } ?: id
    } else {
        earthquakeId
    }
    val resolvedId = id ?: resolvedEarthquakeId ?: UUID.randomUUID().toString()
    return EarthquakeEntity(
        id = resolvedId,
        earthquakeId = resolvedEarthquakeId?.takeUnless { it.isBlank() } ?: resolvedId,
        source = if (isReal) "detected" else "expected",
        isReal = isReal,
        magnitude = magnitude ?: 0.0,
        depth = depth ?: 0.0,
        latitude = latitude ?: 0.0,
        longitude = longitude ?: 0.0,
        originalLatitude = originalLatitude,
        originalLongitude = originalLongitude,
        place = place,
        radiusKm = radiusKm ?: 0.0,
        timeMs = timeMs ?: 0L,
        time = time ?: ""
    )
}

fun EarthquakeDto.toDomainModel(isReal: Boolean): Earthquake =
    toEntity(isReal).toDomain()
