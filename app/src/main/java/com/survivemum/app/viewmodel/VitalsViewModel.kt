package com.survivemum.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.survivemum.app.data.VitalsRepository
import com.survivemum.app.model.VitalsState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VitalsViewModel(
    private val repository: VitalsRepository = VitalsRepository()
) : ViewModel() {

    private val _isEngineReady = MutableStateFlow(false)
    val isEngineReady: StateFlow<Boolean> = _isEngineReady.asStateFlow()

    private val _alertTriggered = MutableStateFlow(false)
    val alertTriggered: StateFlow<Boolean> = _alertTriggered.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<VitalsState> = _alertTriggered
        .flatMapLatest { isHighRisk ->
            repository.getVitalsStream(isHighRisk)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VitalsState()
        )

    init {
        // Simulate Engine Loading
        viewModelScope.launch {
            delay(1500)
            _isEngineReady.value = true
        }

        // Auto-trigger alert simulation after 12 seconds
        viewModelScope.launch {
            delay(12000)
            _alertTriggered.value = true
        }
    }

    fun toggleAlert() {
        _alertTriggered.value = !_alertTriggered.value
    }
}
