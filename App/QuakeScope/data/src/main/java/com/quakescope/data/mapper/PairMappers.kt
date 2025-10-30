package com.quakescope.data.mapper

import com.quakescope.data.remote.dto.EarthquakePairItemDto
import com.quakescope.domain.model.EarthquakePair

fun EarthquakePairItemDto.toDomain(): EarthquakePair = EarthquakePair(
    real = real?.toDomainModel(true),
    estimated = expected?.toDomainModel(false)
)
