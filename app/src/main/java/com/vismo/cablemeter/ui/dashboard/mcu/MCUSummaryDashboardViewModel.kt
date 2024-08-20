package com.vismo.cablemeter.ui.dashboard.mcu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilin.util.ShellUtils
import com.vismo.cablemeter.datastore.MCUParamsDataStore
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.repository.MeasureBoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MCUSummaryDashboardViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val measureBoardRepository: MeasureBoardRepository
) : ViewModel() {

    private val _mcuSummaryUiState = MutableStateFlow(MCUSummaryUiData())
    val mcuSummaryUiState: StateFlow<MCUSummaryUiData> = _mcuSummaryUiState

    init {
        measureBoardRepository.enquireParameters()
        val romVersion = getROMVersion()
        val androidId = getAndroidId()
        viewModelScope.launch {
            withContext(ioDispatcher) {
                launch {
                    MCUParamsDataStore.mcuPriceParams.collectLatest {
                        it?.let {
                            _mcuSummaryUiState.value = _mcuSummaryUiState.value.copy(
                                fareParams = it
                            )
                        }
                    }
                }
                launch {
                    MCUParamsDataStore.deviceIdData.collectLatest {
                        it?.let {
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