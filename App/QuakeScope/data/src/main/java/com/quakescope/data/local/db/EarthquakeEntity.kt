package com.quakescope.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "earthquakes")
data class EarthquakeEntity(
    @PrimaryKey val id: String,
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
    val timeMs: Long,
    val time: String
)
