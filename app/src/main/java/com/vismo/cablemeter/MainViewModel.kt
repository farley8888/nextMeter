package com.vismo.cablemeter

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.cablemeter.datastore.MCUParamsDataStore
import com.vismo.cablemeter.ui.topbar.TopAppBarUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.repository.MeasureBoardRepository
import com.vismo.cablemeter.repository.PeripheralControlRepository
import com.vismo.cablemeter.repository.RemoteMeterControlRepository
import com.vismo.cablemeter.repository.TripRepository
import com.vismo.cablemeter.ui.theme.gold600
import com.vismo.cablemeter.ui.theme.nobel600
import com.vismo.cablemeter.ui.theme.pastelGreen600
import com.vismo.cablemeter.ui.theme.primary700
import com.vismo.nxgnfirebasemodule.DashManagerConfig
import com.vismo.nxgnfirebasemodule.model.MeterLocation
import com.vismo.nxgnfirebasemodule.model.TripPaidStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val measureBoardRepository: MeasureBoardRepository,
    private val peripheralControlRepository: PeripheralControlRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val remoteMCUControlRepository: RemoteMeterControlRepository,
    private val dashManagerConfig: DashManagerConfig,
    private val tripRepository: TripRepository
    ) : ViewModel(){

    private val _topAppBarUiState = MutableStateFlow(TopAppBarUiState())
    val topAppBarUiState: StateFlow<TopAppBarUiState> = _topAppBarUiState

    private val _showLoginToggle = MutableStateFlow<Boolean?>(null)
    val showLoginToggle: StateFlow<Boolean?> = _showLoginToggle

    private val dateFormat = SimpleDateFormat("M月d日 HH:mm", Locale.TRADITIONAL_CHINESE)

    private val toolbarUiDataUpdateMutex = Mutex()

    private fun observeFlows() {
        viewModelScope.launch(ioDispatcher) {
            launch {
                MCUParamsDataStore.mcuTime.collectLatest {
                    it?.let { dateTime ->
                        try {
                            val formatter = SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH)
                            val date = formatter.parse(dateTime)
                            date?.let {
                                val formattedDate = dateFormat.format(date)
                                toolbarUiDataUpdateMutex.withLock {
                                    _topAppBarUiState.value = _topAppBarUiState.value.copy(
                                        dateTime = formattedDate
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.d("MainViewModel", "Error parsing date: $dateTime")
                        }
                    }
                }
            }

            launch {
                remoteMCUControlRepository.heartBeatInterval.collectLatest { interval ->
                    if (interval > 0) {
                        while (true) {
                            remoteMCUControlRepository.sendHeartBeat()
                            delay(interval* 1000L)
                        }
                    }
                }
            }

            launch {
                combine(
                    remoteMCUControlRepository.meterInfo,
                    tripRepository.currentTripPaidStatus
                ) { meterInfo, tripPaidStatus ->
                    Pair(meterInfo, tripPaidStatus)
                }.collectLatest { (meterInfo, tripPaidStatus) ->
                    meterInfo?.let {
                        _showLoginToggle.value = it.settings.showLoginToggle

                        toolbarUiDataUpdateMutex.withLock {
                            _topAppBarUiState.value = _topAppBarUiState.value.copy(
                                showLoginToggle = it.settings.showLoginToggle,
                                driverPhoneNumber = it.session?.driver?.driverPhoneNumber ?: "",
                            )
                        }
                    }

                    val toolbarColor = when (tripPaidStatus) {
                        TripPaidStatus.NOT_PAID -> if (meterInfo?.session != null) primary700 else nobel600
                        TripPaidStatus.COMPLETELY_PAID -> pastelGreen600
                        TripPaidStatus.PARTIALLY_PAID -> gold600
                    }
                    toolbarUiDataUpdateMutex.withLock {
                        _topAppBarUiState.value = _topAppBarUiState.value.copy(
                            color = toolbarColor
                        )
                    }
                }
            }
        }
    }

    fun updateBackButtonVisibility(isVisible: Boolean) {
        viewModelScope.launch {
            toolbarUiDataUpdateMutex.withLock {
                _topAppBarUiState.value = _topAppBarUiState.value.copy(
                    isBackButtonVisible = isVisible
                )
            }
        }
    }

    private fun updateLocationIconVisibility(isVisible: Boolean = true) {
        if (_topAppBarUiState.value.isLocationIconVisible != isVisible) {
            viewModelScope.launch {
                toolbarUiDataUpdateMutex.withLock {
                    _topAppBarUiState.value = _topAppBarUiState.value.copy(
                        isLocationIconVisible = isVisible
                    )
                }
            }
        }
    }

    fun updateSignalStrength(signalStrength: Int) {
        if (_topAppBarUiState.value.signalStrength == signalStrength) return
        viewModelScope.launch {
            toolbarUiDataUpdateMutex.withLock {
                _topAppBarUiState.value = _topAppBarUiState.value.copy(
                    signalStrength = signalStrength
                )
            }
        }
    }

    fun setWifiIconVisibility(isVisible: Boolean) {
        viewModelScope.launch {
            toolbarUiDataUpdateMutex.withLock {
                _topAppBarUiState.value = _topAppBarUiState.value.copy(
                    isWifiIconVisible = isVisible
                )
            }
        }
    }

    fun setLocation(meterLocation: MeterLocation) {
        dashManagerConfig.setLocation(meterLocation)
        updateLocationIconVisibility()
    }


    init {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                remoteMCUControlRepository.observeFlows()
            }
        }
        observeFlows()
    }

    override fun onCleared() {
        super.onCleared()
        measureBoardRepository.stopCommunication()
        remoteMCUControlRepository.onCleared()
        peripheralControlRepository.close()

    }
}
