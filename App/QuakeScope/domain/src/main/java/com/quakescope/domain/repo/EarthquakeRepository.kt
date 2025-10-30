package com.quakescope.domain.repo

import androidx.paging.PagingData
import com.quakescope.domain.model.EarthquakePair
import com.quakescope.domain.model.FilterState
import kotlinx.coroutines.flow.Flow

interface EarthquakeRepository {

    fun getEarthquakePairs(filterState: FilterState): Flow<PagingData<EarthquakePair>>

    fun getEarthquakesForMap(filterState: FilterState): Flow<List<EarthquakePair>>
}
