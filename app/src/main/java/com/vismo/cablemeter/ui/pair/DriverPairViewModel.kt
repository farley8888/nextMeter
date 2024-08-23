package com.vismo.cablemeter.ui.pair

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.cablemeter.datastore.MCUParamsDataStore
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.repository.DriverPrefsRepository
import com.vismo.cablemeter.repository.RemoteMeterControlRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DriverPairViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val remoteMeterControlRepository: RemoteMeterControlRepository,
    private val driverPrefsRepository: DriverPrefsRepository
) : ViewModel() {

    private val _driverPairScreenUiData = MutableStateFlow(DriverPairUiData())
    val driverPairScreenUiData: StateFlow<DriverPairUiData> = _driverPairScreenUiData

    private val _savedDriverIds = MutableStateFlow<MutableList<String>>(mutableListOf())
    val savedDriverIds: StateFlow<List<String>> = _savedDriverIds

    init {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                launch {
                    MCUParamsDataStore.deviceIdData.collectLatest { deviceIdData ->
                        deviceIdData?.let {
                            _driverPairScreenUiData.value = _driverPairScreenUiData.value.copy(
                                qrString = driverPrefsRepository.genQR(it.licensePlate),
                                licensePlate = it.licensePlate,
                                deviceSerialNumber = it.deviceId,
                            )
                        }
                    }
                }
                launch {
                    remoteMeterControlRepository.meterInfo.collectLatest { meterInfo ->
                        meterInfo?.let {
                            if (it.session != null) {
                                _driverPairScreenUiData.value = _driverPairScreenUiData.value.copy(
                                    driverPhoneNumber = it.session.driver.driverPhoneNumber
                                )
                                isDriverPinSet(it.session.driver.driverPhoneNumber)
                            } else {
                                _driverPairScreenUiData.value = _driverPairScreenUiData.value.copy(
                                    driverPhoneNumber = "",
                                    isDriverPinSet = null
                                )
                            }
                        }
                    }
                }
                launch {
                    val prefs = driverPrefsRepository.getDriverPrefs()
                    prefs.driverPinMap.forEach { item ->
                        _savedDriverIds.value = _savedDriverIds.value.apply { add(item.driverId) }
                    }
                }
            }
        }
    }

    private fun isDriverPinSet(driverPhoneNumber: String) {
        viewModelScope.launch {
            val driverPrefs = driverPrefsRepository.getDriverPrefs()
            val skippedDrivers = driverPrefsRepository.getSkippedDrivers()
            var isPinExist: Boolean? = false
            driverPrefs.driverPinMap.forEach { mapItem ->
                if (mapItem.driverId == driverPhoneNumber) {
                    isPinExist = true
                }
            }
            if (skippedDrivers.drivers.contains(driverPhoneNumber)) {
                isPinExist = null
            }
            _driverPairScreenUiData.value = _driverPairScreenUiData.value.copy(
                isDriverPinSet = isPinExist
            )
        }
    }

    fun clearDriverSession() {
        remoteMeterControlRepository.clearDriverSession()
    }

    fun clearIsDriverPinSet() {
        _driverPairScreenUiData.value = _driverPairScreenUiData.value.copy(isDriverPinSet = null)
    }

    fun refreshQr() {
        val newQr = driverPrefsRepository.genQR(_driverPairScreenUiData.value.licensePlate)
        _driverPairScreenUiData.value = _driverPairScreenUiData.value.copy(qrString = newQr)
    }
}