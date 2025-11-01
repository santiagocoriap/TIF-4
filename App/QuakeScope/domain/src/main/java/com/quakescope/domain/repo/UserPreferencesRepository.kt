package com.quakescope.domain.repo

import com.quakescope.domain.model.AlertPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun observePreferences(): Flow<AlertPreferences>
    suspend fun updatePreferences(preferences: AlertPreferences)
    suspend fun updateDeviceToken(token: String)
}
