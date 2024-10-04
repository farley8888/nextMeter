package com.vismo.cablemeter.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilin.util.ShellUtils
import com.vismo.cablemeter.datastore.MCUParamsDataStore
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.repository.MeasureBoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class EditAdminPropertiesViewModel @Inject constructor(
    private val measureBoardRepository: MeasureBoardRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ): ViewModel() {

    val mcuPriceParams = MCUParamsDataStore.mcuPriceParams.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = null
    )

    val deviceIdData = MCUParamsDataStore.deviceIdData.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = null
    )

    private val _currentADBStatus: MutableStateFlow<ADBStatus?> = MutableStateFlow(null)
    val currentADBStatus: StateFlow<ADBStatus?> = _currentADBStatus

    init {
        setInitialADBStatus()
    }

    fun updateKValue(kValue: Int) {
        measureBoardRepository.updateKValue(kValue)
    }

    fun updateLicensePlate(licensePlate: String) {
        measureBoardRepository.updateLicensePlate(licensePlate)
    }

    fun updatePriceParams(
        startPrice: Int, stepPrice: Int, stepPrice2nd:Int, threshold:Int
    ) {
        measureBoardRepository.updatePriceParams(
            startPrice, stepPrice, stepPrice2nd, threshold
        )
    }

    fun reEnquireParameters() {
        measureBoardRepository.enquireParameters()
    }

    private fun setInitialADBStatus() {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                val status = ShellUtils.execShellCmd("getprop persist.service.adb.enable")
                if (status.contains("1")) {
                    _currentADBStatus.value = ADBStatus.ENABLED
                } else {
                    _currentADBStatus.value = ADBStatus.DISABLED
                }
            }
        }
    }

    fun toggleADB() {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                if (_currentADBStatus.value == ADBStatus.ENABLED) {
                    ShellUtils.execShellCmd("setprop persist.service.adb.enable 0")
                    _currentADBStatus.value = ADBStatus.DISABLED
                } else {
                    ShellUtils.execShellCmd("setprop persist.service.adb.enable 1")
                    _currentADBStatus.value = ADBStatus.ENABLED
                }
            }
        }
    }

    enum class ADBStatus {
        ENABLED, DISABLED
    }
}