package com.quakescope.domain.usecase

import com.quakescope.domain.repo.UserPreferencesRepository
import javax.inject.Inject

class UpdateDeviceTokenUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(token: String) {
        repository.updateDeviceToken(token)
    }
}
