package com.vismo.cablemeter.ui.pair

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.cablemeter.datastore.MCUParamsDataStore
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.repository.RemoteMeterControlRepository
import com.vismo.cablemeter.util.GlobalUtils.encrypt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DriverPairViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val remoteMeterControlRepository: RemoteMeterControlRepository
) : ViewModel() {

    private val _driverPairScreenUiData = MutableStateFlow(DriverPairUiData())
    val driverPairScreenUiData: StateFlow<DriverPairUiData> = _driverPairScreenUiData

    private val uiUpdateMutex = Mutex()

    init {
        viewModelScope.launch {
            launch(ioDispatcher) { observeDeviceIdDate() }
            launch(ioDispatcher) { observeMeterInfo() }
        }
    }

    private suspend fun observeMeterInfo() {
        remoteMeterControlRepository.meterInfo.collectLatest { meterInfo ->
            meterInfo?.let {
                uiUpdateMutex.withLock {
                    if (it.session != null) {
                        _driverPairScreenUiData.value = _driverPairScreenUiData.value.copy(
                            driverPhoneNumber = it.session.driver.driverPhoneNumber
                        )
                    } else {
                        _driverPairScreenUiData.value = _driverPairScreenUiData.value.copy(
                            driverPhoneNumber = ""
                        )
                    }
                }
            }
        }
    }

    private suspend fun observeDeviceIdDate() {
        MCUParamsDataStore.deviceIdData.collectLatest { deviceIdData ->
            deviceIdData?.let {
                uiUpdateMutex.withLock {
                    _driverPairScreenUiData.value = _driverPairScreenUiData.value.copy(
                        qrString = genQR(it.licensePlate),
                        licensePlate = it.licensePlate,
                        deviceSerialNumber = it.deviceId,
                    )
                }
            }
        }
    }

    fun clearDriverSession() {
        remoteMeterControlRepository.clearDriverSession()
    }

    fun refreshQr() {
        viewModelScope.launch {
            val newQr = genQR(_driverPairScreenUiData.value.licensePlate)
            uiUpdateMutex.withLock {
                _driverPairScreenUiData.value = _driverPairScreenUiData.value.copy(qrString = newQr)
            }
        }
    }

    /**
     * Generate the QR code to allow the driver app to scan and bind with the corresponding meter
     */
    private fun genQR(meterId: String): String {
        try {
            val version = 1
            val keyVersion = 1
            val licensePlate = meterId //"AZ1099"
            val date = Calendar.getInstance().time
            val sdf = SimpleDateFormat("yyMMddHHmmss", Locale.US)
            val timeStamp = sdf.format(date)

            /**
             *  11 - version + key version
            AZ1099 - lic plate
            2206271010001 - YYMMDDHHMMSS+seed <= encrypted
             */
            val seed = "1"
            return "$version$keyVersion$licensePlate-${encrypt("$timeStamp$seed")}"
        } catch (e: UninitializedPropertyAccessException) {
            return ""
        }
    }
}