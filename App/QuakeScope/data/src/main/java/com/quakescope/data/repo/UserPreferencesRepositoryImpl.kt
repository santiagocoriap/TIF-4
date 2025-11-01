package com.quakescope.data.repo

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quakescope.data.remote.ApiService
import com.quakescope.data.remote.dto.DeviceTokenRequest
import com.quakescope.data.remote.dto.UserPreferencesRequest
import com.quakescope.domain.model.AlertPreferences
import com.quakescope.domain.repo.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val apiService: ApiService
) : UserPreferencesRepository {

    override fun observePreferences(): Flow<AlertPreferences> =
        dataStore.data.map { preferences ->
            AlertPreferences(
                latitude = preferences[LATITUDE_KEY],
                longitude = preferences[LONGITUDE_KEY],
                alertRadiusKm = preferences[RADIUS_KEY] ?: DEFAULT_RADIUS,
                minimumMagnitude = preferences[MIN_MAGNITUDE_KEY] ?: DEFAULT_MIN_MAGNITUDE
            )
        }

    override suspend fun updatePreferences(preferences: AlertPreferences) {
        dataStore.edit { store ->
            store[RADIUS_KEY] = preferences.alertRadiusKm
            store[MIN_MAGNITUDE_KEY] = preferences.minimumMagnitude
            preferences.latitude?.let { store[LATITUDE_KEY] = it }
            preferences.longitude?.let { store[LONGITUDE_KEY] = it }
            if (preferences.latitude == null) store.remove(LATITUDE_KEY)
            if (preferences.longitude == null) store.remove(LONGITUDE_KEY)
        }

        val storedToken = dataStore.data.firstOrNull()?.get(FCM_TOKEN_KEY)
        try {
            apiService.updateUserPreferences(
                UserPreferencesRequest(
                    latitude = preferences.latitude,
                    longitude = preferences.longitude,
                    alertRadiusKm = preferences.alertRadiusKm,
                    minimumMagnitude = preferences.minimumMagnitude,
                    fcmToken = storedToken
                )
            )
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to sync user preferences", ex)
        }
    }

    override suspend fun updateDeviceToken(token: String) {
        dataStore.edit { store ->
            store[FCM_TOKEN_KEY] = token
        }
        try {
            apiService.updateDeviceToken(DeviceTokenRequest(token))
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to sync device token", ex)
        }
    }

    private companion object {
        private const val TAG = "UserPreferencesRepo"
        private const val DEFAULT_RADIUS = 50.0
        private const val DEFAULT_MIN_MAGNITUDE = 4.0

        private val LATITUDE_KEY = doublePreferencesKey("alert_latitude")
        private val LONGITUDE_KEY = doublePreferencesKey("alert_longitude")
        private val RADIUS_KEY = doublePreferencesKey("alert_radius_km")
        private val MIN_MAGNITUDE_KEY = doublePreferencesKey("alert_min_magnitude")
        private val FCM_TOKEN_KEY = stringPreferencesKey("fcm_token")
    }
}
