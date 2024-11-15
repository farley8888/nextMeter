package com.vismo.cablemeter.ui.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.repository.FirebaseAuthRepository
import com.vismo.cablemeter.repository.MeasureBoardRepository
import com.vismo.cablemeter.repository.MeterPreferenceRepository
import com.vismo.cablemeter.repository.NetworkTimeRepository
import com.vismo.nxgnfirebasemodule.DashManager
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

    init {
        viewModelScope.launch(ioDispatcher) {
            launch { startTimeout() }
            launch { observeMeterFields() }
        }
    }

    private suspend fun observeMeterFields() {
        meterPreferenceRepository.getShowLoginToggle().collectLatest { showLoginToggle ->
            _showLoginToggle.value = showLoginToggle
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