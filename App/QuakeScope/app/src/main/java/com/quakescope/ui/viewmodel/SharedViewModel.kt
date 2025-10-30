package com.quakescope.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.quakescope.domain.model.Earthquake
import com.quakescope.domain.model.EarthquakePair
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor() : ViewModel() {

    private val _selectedPair = MutableStateFlow<EarthquakePair?>(null)
    val selectedPair: StateFlow<EarthquakePair?> = _selectedPair

    private val _selectedEarthquake = MutableStateFlow<Earthquake?>(null)
    val selectedEarthquake: StateFlow<Earthquake?> = _selectedEarthquake

    fun selectPair(pair: EarthquakePair) {
        _selectedPair.value = pair
        _selectedEarthquake.value = null
    }

    fun selectEarthquake(pair: EarthquakePair, earthquake: Earthquake) {
        _selectedPair.value = pair
        _selectedEarthquake.value = earthquake
    }

    fun clearSelection() {
        _selectedPair.value = null
        _selectedEarthquake.value = null
    }
}
