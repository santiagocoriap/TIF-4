package com.quakescope.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.quakescope.domain.model.EarthquakePair
import com.quakescope.domain.model.FilterState
import com.quakescope.domain.model.EarthquakeType
import com.quakescope.domain.model.SortOption
import com.quakescope.domain.model.SortOptionRules
import com.quakescope.domain.usecase.GetEarthquakePairsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ListViewModel @Inject constructor(
    private val getEarthquakePairsUseCase: GetEarthquakePairsUseCase
) : ViewModel() {

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState
    private val refreshTrigger = MutableStateFlow(0)

    val earthquakePairs: StateFlow<PagingData<EarthquakePair>> = combine(_filterState, refreshTrigger) { filter, _ -> filter }
        .flatMapLatest { filter -> getEarthquakePairsUseCase(filter) }
        .cachedIn(viewModelScope)
        .stateIn(viewModelScope, SharingStarted.Lazily, PagingData.empty())

    fun setMagnitudeRange(range: ClosedFloatingPointRange<Float>) {
        _filterState.value = _filterState.value.copy(magnitudeRange = range)
    }

    fun setDepthRange(range: ClosedFloatingPointRange<Float>) {
        _filterState.value = _filterState.value.copy(depthRange = range)
    }

    fun setSortOption(sortOption: SortOption) {
        val normalized = SortOptionRules.ensureAllowed(sortOption, _filterState.value.earthquakeType)
        _filterState.value = _filterState.value.copy(sortOption = normalized)
    }

    fun setLimit(limit: Int) {
        _filterState.value = _filterState.value.copy(limit = limit)
    }

    fun setEarthquakeType(earthquakeType: EarthquakeType) {
        val normalizedSort = SortOptionRules.ensureAllowed(_filterState.value.sortOption, earthquakeType)
        _filterState.value = _filterState.value.copy(
            earthquakeType = earthquakeType,
            sortOption = normalizedSort
        )
    }

    fun setRealMagnitudeRange(range: ClosedFloatingPointRange<Float>) {
        _filterState.value = _filterState.value.copy(realMagnitudeRange = range)
    }

    fun setEstimatedMagnitudeRange(range: ClosedFloatingPointRange<Float>) {
        _filterState.value = _filterState.value.copy(estimatedMagnitudeRange = range)
    }

    fun setRealDepthRange(range: ClosedFloatingPointRange<Float>) {
        _filterState.value = _filterState.value.copy(realDepthRange = range)
    }

    fun setEstimatedDepthRange(range: ClosedFloatingPointRange<Float>) {
        _filterState.value = _filterState.value.copy(estimatedDepthRange = range)
    }

    fun refresh() {
        refreshTrigger.value = refreshTrigger.value + 1
    }
}
