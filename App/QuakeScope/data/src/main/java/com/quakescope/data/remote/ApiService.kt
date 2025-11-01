package com.quakescope.data.remote

import com.quakescope.data.remote.dto.DeviceTokenRequest
import com.quakescope.data.remote.dto.EarthquakePairResponseDto
import com.quakescope.data.remote.dto.EarthquakeResponseDto
import com.quakescope.data.remote.dto.UserPreferencesRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @GET("api/earthquakes/detected")
    suspend fun getDetectedEarthquakes(
        @Query("min_mag") minMagnitude: Double? = null,
        @Query("max_mag") maxMagnitude: Double? = null,
        @Query("limit") limit: Int? = null
    ): EarthquakeResponseDto

    @GET("api/earthquakes/expected")
    suspend fun getExpectedEarthquakes(
        @Query("min_mag") minMagnitude: Double? = null,
        @Query("max_mag") maxMagnitude: Double? = null,
        @Query("limit") limit: Int? = null
    ): EarthquakeResponseDto

    @GET("api/earthquakes/pairs")
    suspend fun getPairedEarthquakePairs(
        @Query("real_min_mag") realMinMagnitude: Double? = null,
        @Query("real_max_mag") realMaxMagnitude: Double? = null,
        @Query("real_min_depth") realMinDepth: Double? = null,
        @Query("real_max_depth") realMaxDepth: Double? = null,
        @Query("expected_min_mag") expectedMinMagnitude: Double? = null,
        @Query("expected_max_mag") expectedMaxMagnitude: Double? = null,
        @Query("expected_min_depth") expectedMinDepth: Double? = null,
        @Query("expected_max_depth") expectedMaxDepth: Double? = null,
        @Query("limit") limit: Int? = null
    ): EarthquakePairResponseDto

    @POST("api/alerts/preferences")
    suspend fun updateUserPreferences(
        @Body body: UserPreferencesRequest
    )

    @POST("api/alerts/device-token")
    suspend fun updateDeviceToken(
        @Body body: DeviceTokenRequest
    )
}
