package com.quakescope.domain.usecase

import androidx.paging.PagingData
import com.quakescope.domain.model.EarthquakePair
import com.quakescope.domain.model.FilterState
import com.quakescope.domain.repo.EarthquakeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEarthquakePairsUseCase @Inject constructor(
    private val earthquakeRepository: EarthquakeRepository
) {
    operator fun invoke(filterState: FilterState): Flow<PagingData<EarthquakePair>> = earthquakeRepository.getEarthquakePairs(filterState)
}
