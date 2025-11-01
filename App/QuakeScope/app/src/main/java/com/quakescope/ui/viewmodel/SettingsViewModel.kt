package com.quakescope.ui.viewmodel

import android.location.Location
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.quakescope.R
import com.quakescope.domain.model.AlertPreferences
import com.quakescope.domain.usecase.ObserveAlertPreferencesUseCase
import com.quakescope.domain.usecase.UpdateAlertPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val observeAlertPreferences: ObserveAlertPreferencesUseCase,
    private val updateAlertPreferencesUseCase: UpdateAlertPreferencesUseCase,
    private val fusedLocationProviderClient: FusedLocationProviderClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            observeAlertPreferences().collect { preferences ->
                _uiState.update {
                    it.copy(alertPreferences = preferences)
                }
            }
        }
    }

    fun onRadiusChanged(value: Float) {
        _uiState.update {
            it.copy(alertPreferences = it.alertPreferences.copy(alertRadiusKm = value.toDouble()))
        }
    }

    fun onMinimumMagnitudeChanged(value: Float) {
        _uiState.update {
            it.copy(alertPreferences = it.alertPreferences.copy(minimumMagnitude = value.toDouble()))
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingLocation = true) }
            try {
                val location = fetchBestEffortLocation()
                if (location != null) {
                    _uiState.update {
                        it.copy(
                            alertPreferences = it.alertPreferences.copy(
                                latitude = location.latitude,
                                longitude = location.longitude
                            ),
                            isFetchingLocation = false
                        )
                    }
                    _events.emit(SettingsEvent.ShowMessage(messageRes = R.string.settings_alert_location_updated))
                } else {
                    _uiState.update { it.copy(isFetchingLocation = false) }
                    _events.emit(SettingsEvent.ShowMessage(messageRes = R.string.settings_alert_location_failed))
                }
            } catch (security: SecurityException) {
                _uiState.update { it.copy(isFetchingLocation = false) }
                _events.emit(SettingsEvent.ShowMessage(messageRes = R.string.settings_alert_location_permission_denied))
            } catch (ex: Exception) {
                _uiState.update { it.copy(isFetchingLocation = false) }
                _events.emit(SettingsEvent.ShowMessage(message = ex.localizedMessage ?: ""))
            }
        }
    }

    fun onLocationPermissionDenied() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            viewModelScope.launch {
                _events.emit(SettingsEvent.ShowMessage(messageRes = R.string.settings_alert_location_permission_denied))
            }
        }
    }

    fun savePreferences() {
        val preferences = _uiState.value.alertPreferences
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                updateAlertPreferencesUseCase(preferences)
                _events.emit(SettingsEvent.ShowMessage(messageRes = R.string.settings_alert_save_success))
            } catch (ex: Exception) {
                _events.emit(SettingsEvent.ShowMessage(messageRes = R.string.settings_alert_save_error))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private suspend fun fetchBestEffortLocation(): Location? {
        val priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY
        val cancellationToken = CancellationTokenSource()
        val current = try {
            fusedLocationProviderClient.getCurrentLocation(priority, cancellationToken.token).await()
        } catch (ex: Exception) {
            null
        }
        if (current != null) return current
        return try {
            fusedLocationProviderClient.lastLocation.await()
        } catch (ex: Exception) {
            null
        }
    }
}

data class SettingsUiState(
    val alertPreferences: AlertPreferences = AlertPreferences(),
    val isFetchingLocation: Boolean = false,
    val isSaving: Boolean = false
) {
    val hasLocation: Boolean
        get() = alertPreferences.latitude != null && alertPreferences.longitude != null
}

sealed class SettingsEvent {
    data class ShowMessage(val messageRes: Int? = null, val message: String? = null) : SettingsEvent()
}
