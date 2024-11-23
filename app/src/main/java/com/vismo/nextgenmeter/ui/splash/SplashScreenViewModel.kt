package com.vismo.nextgenmeter.ui.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.repository.MeterPreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashScreenViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val meterPreferenceRepository: MeterPreferenceRepository
): ViewModel() {
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _showLoginToggle = MutableStateFlow(false)
    val showLoginToggle: StateFlow<Boolean> = _showLoginToggle

    private val _showConnectionIconsToggle = MutableStateFlow<Boolean>(false)
    val showConnectionIconsToggle: StateFlow<Boolean> = _showConnectionIconsToggle

    init {
        viewModelScope.launch(ioDispatcher) {
            launch { startTimeout() }
            launch { observeShowLoginToggle() }
            launch { observeShowConnectionIconsToggle() }
        }
    }

    private suspend fun observeShowLoginToggle() {
        meterPreferenceRepository.getShowLoginToggle().collectLatest { showLoginToggle ->
            _showLoginToggle.value = showLoginToggle
        }
    }

    private suspend fun observeShowConnectionIconsToggle() {
        meterPreferenceRepository.getShowConnectionIconsToggle().collectLatest { showConnectionIconsToggle ->
            _showConnectionIconsToggle.value = showConnectionIconsToggle
        }
    }

    private suspend fun startTimeout() {
        delay(MINIMUM_TIMEOUT_DURATION)
        _isLoading.value = false
        Log.d(TAG, "Timeout reached")

    }

    companion object {
        const val MINIMUM_TIMEOUT_DURATION = 3000L
        const val TAG = "SplashScreenViewModel"
    }
}