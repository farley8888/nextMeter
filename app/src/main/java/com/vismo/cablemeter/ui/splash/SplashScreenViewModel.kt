package com.vismo.cablemeter.ui.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.network.ConnectivityManager
import com.vismo.cablemeter.repository.FirebaseAuthRepository
import com.vismo.cablemeter.repository.MeasureBoardRepository
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
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val connectivityManager: ConnectivityManager,
    private val dashManager: DashManager,
    private val networkTimeRepository: NetworkTimeRepository,
    private val measureBoardRepository: MeasureBoardRepository
): ViewModel() {
    private val _isNetworkConnected = MutableStateFlow(false)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _timeoutReached = MutableStateFlow(0L)
    val timeoutReached: StateFlow<Long> = _timeoutReached

    private val _showLoginToggle = MutableStateFlow(false)
    val showLoginToggle: StateFlow<Boolean> = _showLoginToggle

    init {
        viewModelScope.launch(ioDispatcher) {
            launch { startTimeout() }
            launch { checkNetworkConnection() }
            launch { observeMeterFields() }
            launch { observeFirebaseAuthSuccess() }
        }
    }

    private suspend fun observeFirebaseAuthSuccess() {
        firebaseAuthRepository.isFirebaseAuthSuccess.collectLatest { isFirebaseAuthSuccess ->
            if (isFirebaseAuthSuccess) {
                dashManager.init()
                Log.d(TAG, "Firebase auth success")
            }
        }
    }

    private suspend fun observeMeterFields() {
        dashManager.meterFields.collectLatest {  meterFields ->
            if (meterFields?.settings != null) {
                if (meterFields.settings!!.showLoginToggle) {
                    _showLoginToggle.value = true
                    Log.d(TAG, "Show login toggle")
                } else {
                    _isLoading.value = false
                    Log.d(TAG, "Hide login toggle")
                }
            }
        }
    }

    private suspend fun checkNetworkConnection() {
        // Continuously monitor network status
        while (_timeoutReached.value <= TOTAL_TIMEOUT_DURATION) {
            val isConnected = connectivityManager.isDeviceOnline()
            _isNetworkConnected.value = isConnected
            Log.d(TAG, "Network status: $isConnected")
            if (isConnected) {
                firebaseAuthRepository.initToken()
                networkTimeRepository.fetchNetworkTime()?.let { networkTime ->
                    measureBoardRepository.updateMeasureBoardTime(networkTime)
                    Log.d(TAG, "Network time: $networkTime")
                }
                Log.d(TAG, "Firebase token initialized")
                break
            }
            delay(1000L)
        }
    }

    private suspend fun startTimeout() {
        delay(MINIMUM_TIMEOUT_DURATION)
        _timeoutReached.value = TIMEOUT_DURATION
        Log.d(TAG, "Timeout started")
        delay(TIMEOUT_DURATION)
        _timeoutReached.value = TOTAL_TIMEOUT_DURATION
        Log.d(TAG, "Timeout reached")
    }

    companion object {
        const val MINIMUM_TIMEOUT_DURATION = 5000L
        private const val TIMEOUT_DURATION = 30000L
        const val TOTAL_TIMEOUT_DURATION = MINIMUM_TIMEOUT_DURATION + TIMEOUT_DURATION
        const val TAG = "SplashScreenViewModel"
    }
}