package com.quakescope.domain.usecase

import com.quakescope.domain.model.AlertPreferences
import com.quakescope.domain.repo.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAlertPreferencesUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<AlertPreferences> = repository.observePreferences()
}
