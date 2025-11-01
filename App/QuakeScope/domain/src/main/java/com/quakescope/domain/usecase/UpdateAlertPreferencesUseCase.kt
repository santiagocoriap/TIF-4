package com.quakescope.domain.usecase

import com.quakescope.domain.model.AlertPreferences
import com.quakescope.domain.repo.UserPreferencesRepository
import javax.inject.Inject

class UpdateAlertPreferencesUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(preferences: AlertPreferences) {
        repository.updatePreferences(preferences)
    }
}
