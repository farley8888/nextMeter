package com.vismo.nextgenmeter.ui.dashboard.mcu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilin.util.ShellUtils
import com.vismo.nextgenmeter.datastore.DeviceDataStore
import com.vismo.nextgenmeter.datastore.DeviceDataStore.mcuPriceParams
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.repository.MeasureBoardRepository
import com.vismo.nextgenmeter.repository.MeterPreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class MCUSummaryDashboardViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val measureBoardRepository: MeasureBoardRepository,
    private val meterPreferenceRepository: MeterPreferenceRepository
) : ViewModel() {

    private val _mcuSummaryUiState = MutableStateFlow(MCUSummaryUiData())
    val mcuSummaryUiState: StateFlow<MCUSummaryUiData> = _mcuSummaryUiState

    private val mutex = Mutex() // Mutex for synchronization

    init {
        measureBoardRepository.enquireParameters()
        val romVersion = getROMVersion()
        val androidId = getAndroidId()
        viewModelScope.launch(ioDispatcher) {
            launch {
                DeviceDataStore.mcuPriceParams.collectLatest {
                    it?.let {
                        mutex.withLock {
                            _mcuSummaryUiState.value = _mcuSummaryUiState.value.copy(
                                fareParams = it
                            )
                            val newMcuStartPrice = mcuPriceParams.value?.startingPrice?.replace("$", "")
                            val savedMcuStartPrice = meterPreferenceRepository.getMcuStartPrice().first()
                            if (newMcuStartPrice != null && savedMcuStartPrice != newMcuStartPrice && newMcuStartPrice.toDoubleOrNull() != null) {
                                meterPreferenceRepository.saveMcuStartPrice(newMcuStartPrice)
                            }
                        }
                    }
                }
            }
            launch {
                DeviceDataStore.deviceIdData.collectLatest {
                    it?.let {
                        mutex.withLock {
                            _mcuSummaryUiState.value = _mcuSummaryUiState.value.copy(
                                deviceIdData = it,
                                androidId = androidId,
                                androidROMVersion = romVersion
                            )
                        }
                    }
                }
            }
        }
    }

    private fun getROMVersion(): String {
        return ShellUtils.execShellCmd("getprop firmwareVersion")
    }

    private fun getAndroidId(): String {
        return ShellUtils.execShellCmd("getprop ro.serialno")
    }
}