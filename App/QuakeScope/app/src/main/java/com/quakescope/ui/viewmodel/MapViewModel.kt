package com.quakescope.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quakescope.domain.model.EarthquakePair
import com.quakescope.domain.usecase.GetEarthquakesForMapUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val getEarthquakesForMapUseCase: GetEarthquakesForMapUseCase
) : ViewModel() {

    private lateinit var _filterState: StateFlow<com.quakescope.domain.model.FilterState>
    private val refreshTrigger = MutableStateFlow(0)

    val earthquakePairs: StateFlow<List<EarthquakePair>> = refreshTrigger
        .flatMapLatest {
            _filterState.flatMapLatest { filter ->
                getEarthquakesForMapUseCase(filter)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setFilterState(filterState: StateFlow<com.quakescope.domain.model.FilterState>) {
        _filterState = filterState
    }

    fun refresh() {
        refreshTrigger.value = refreshTrigger.value + 1
    }
}
