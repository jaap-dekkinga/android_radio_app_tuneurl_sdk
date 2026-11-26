package com.tuneurlradio.app.ui.screens.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneurlradio.app.data.ksal.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository
) : ViewModel() {
    val weather = weatherRepository.state

    init {
        viewModelScope.launch { weatherRepository.refreshIfNeeded() }
    }

    suspend fun refresh() = weatherRepository.refresh()
}
