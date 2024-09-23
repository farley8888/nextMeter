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
import kotlinx.coroutines.withContext
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

    init {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                launch {
                    MCUParamsDataStore.deviceIdData.collectLatest { deviceIdData ->
                        deviceIdData?.let {
                            _driverPairScreenUiData.value = _driverPairScreenUiData.value.copy(
                                qrString = genQR(it.licensePlate),
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
                            } else {
                                _driverPairScreenUiData.value = _driverPairScreenUiData.value.copy(
                                    driverPhoneNumber = ""
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun clearDriverSession() {
        remoteMeterControlRepository.clearDriverSession()
    }

    fun refreshQr() {
        val newQr = genQR(_driverPairScreenUiData.value.licensePlate)
        _driverPairScreenUiData.value = _driverPairScreenUiData.value.copy(qrString = newQr)
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