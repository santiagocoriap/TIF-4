package com.quakescope.domain.model

data class AlertPreferences(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val alertRadiusKm: Double = 50.0,
    val minimumMagnitude: Double = 4.0
)
