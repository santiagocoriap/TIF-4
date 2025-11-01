package com.quakescope.data.remote.dto

data class UserPreferencesRequest(
    val latitude: Double?,
    val longitude: Double?,
    val alertRadiusKm: Double,
    val minimumMagnitude: Double,
    val fcmToken: String?
)
