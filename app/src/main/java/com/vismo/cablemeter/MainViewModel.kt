package com.vismo.cablemeter

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.cablemeter.model.TopAppBarUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.repository.FirebaseAuthRepository
import com.vismo.cablemeter.repository.MeasureBoardRepository
import com.vismo.cablemeter.repository.RemoteMCUControlRepository
import com.vismo.nxgnfirebasemodule.DashManagerConfig
import com.vismo.nxgnfirebasemodule.model.MeterLocation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val measureBoardRepository: MeasureBoardRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val remoteMCUControlRepository: RemoteMCUControlRepository,
    private val dashManagerConfig: DashManagerConfig,
    ) : ViewModel(){

    private val _topAppBarUiState = MutableStateFlow(TopAppBarUiState())
    val topAppBarUiState: StateFlow<TopAppBarUiState> = _topAppBarUiState

    val mcuTime = measureBoardRepository.mcuTime
    private val dateFormat = SimpleDateFormat("M月d日 HH:mm", Locale.TRADITIONAL_CHINESE)

    private fun observeFlows() {
        viewModelScope.launch(ioDispatcher) {
            launch {
                mcuTime.collectLatest {
                    it?.let { dateTime ->
                        try {
                            val formatter = SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH)
                            val date = formatter.parse(dateTime)
                            date?.let {
                                val formattedDate = dateFormat.format(date)
                                _topAppBarUiState.value = _topAppBarUiState.value.copy(
                                    dateTime = formattedDate
                                )
                            }
                        } catch (e: Exception) {
                            Log.d("MainViewModel", "Error parsing date: $dateTime")
                        }
                    }
                }
            }
        }
    }

    fun updateBackButtonVisibility(isVisible: Boolean) {
        _topAppBarUiState.value = _topAppBarUiState.value.copy(
            isBackButtonVisible = isVisible
        )
    }

    private fun updateLocationIconVisibility(isVisible: Boolean = true) {
        if (_topAppBarUiState.value.isLocationIconVisible != isVisible) {
            _topAppBarUiState.value = _topAppBarUiState.value.copy(
                isLocationIconVisible = isVisible
            )
        }
    }

    fun updateSignalStrength(signalStrength: Int) {
        if (_topAppBarUiState.value.signalStrength == signalStrength) return
        _topAppBarUiState.value = _topAppBarUiState.value.copy(
            signalStrength = signalStrength
        )
    }

    fun setWifiIconVisibility(isVisible: Boolean) {
        _topAppBarUiState.value = _topAppBarUiState.value.copy(
            isWifiIconVisible = isVisible
        )
    }
    
    fun setLocation(meterLocation: MeterLocation) {
        dashManagerConfig.setLocation(meterLocation)
        updateLocationIconVisibility()
    }


    init {
        viewModelScope.launch {
            firebaseAuthRepository.initToken()
            remoteMCUControlRepository.observeFlows()
        }
        observeFlows()
    }

    override fun onCleared() {
        super.onCleared()
        measureBoardRepository.stopCommunication()
    }
}
