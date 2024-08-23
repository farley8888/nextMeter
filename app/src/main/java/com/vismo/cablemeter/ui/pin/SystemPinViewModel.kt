package com.vismo.cablemeter.ui.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.cablemeter.model.DriverPinMap
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
class SystemPinViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val driverPrefsRepository: DriverPrefsRepository,
    private val remoteMeterControlRepository: RemoteMeterControlRepository
): ViewModel() {
    private val _driverPhoneNumber = MutableStateFlow("")
    val driverPhoneNumber: StateFlow<String>  = _driverPhoneNumber

    private val _isPinVerified = MutableStateFlow<Boolean?>(null)
    val isPinVerified: StateFlow<Boolean?> = _isPinVerified

    private val _isPinSaved = MutableStateFlow<Boolean?>(null)
    val isPinSaved: StateFlow<Boolean?> = _isPinSaved

    private val _isPairSuccessful = MutableStateFlow<Boolean?>(null)
    val isPairSuccessful: StateFlow<Boolean?> = _isPairSuccessful

    init {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                launch {
                    remoteMeterControlRepository.meterInfo.collectLatest { meterInfo ->
                        meterInfo?.let {
                            if (it.session != null) {
                                _driverPhoneNumber.value = it.session.driver.driverPhoneNumber
                            } else {
                                _driverPhoneNumber.value = ""
                            }
                        }
                    }
                }
            }
        }
    }

    fun updateDriverPin(driverPin: String) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                val driverPhoneNumber = _driverPhoneNumber.value
                if (driverPhoneNumber.isNotEmpty()) {
                    val currentDriverPrefs = driverPrefsRepository.getDriverPrefs()
                    // Create a new list with the added driver pin map
                    val updatedDriverPinMap = currentDriverPrefs.driverPinMap.add(
                        DriverPinMap(driverPhoneNumber, driverPin)
                    )
                    val updatedDriverPrefs = currentDriverPrefs.copy(driverPinMap = updatedDriverPinMap)
                    driverPrefsRepository.updateDriverPrefs(updatedDriverPrefs)
                    _isPinSaved.value = true
                }
            }
        }
    }

    fun verifyDriverPin(driverPin: String, driverPhoneNumber: String) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                if (driverPhoneNumber.isNotEmpty()) {
                    val currentDriverPrefs = driverPrefsRepository.getDriverPrefs()
                    val driverPinMap = currentDriverPrefs.driverPinMap
                    for (driverPinMapItem in driverPinMap) {
                        if (driverPinMapItem.driverId == driverPhoneNumber && driverPinMapItem.pin == driverPin) {
                            _isPinVerified.value = true
                            pairDriver(driverPhoneNumber)
                            break
                        }
                    }
                }
            }
        }
    }

    fun saveSkipDriverPin(driverPhoneNumber: String) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                val currentSkippedDrivers = driverPrefsRepository.getSkippedDrivers()
                val updatedSkippedDrivers = currentSkippedDrivers.drivers.add(
                    driverPhoneNumber
                )
                val updatedSkippedDriversPrefs = currentSkippedDrivers.copy(drivers = updatedSkippedDrivers)
                driverPrefsRepository.updateSkippedDrivers(updatedSkippedDriversPrefs)
            }
        }
    }

    private fun pairDriver(driverId: String) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                _isPairSuccessful.value = driverPrefsRepository.pairWithDriver(driverId)
            }
        }
    }
}