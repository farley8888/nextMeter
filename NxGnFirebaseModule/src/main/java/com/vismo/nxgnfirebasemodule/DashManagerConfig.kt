package com.vismo.nxgnfirebasemodule

import com.google.firebase.firestore.GeoPoint
import com.vismo.nxgnfirebasemodule.model.MeterLocation
import com.vismo.nxgnfirebasemodule.model.NOT_SET
import com.vismo.nxgnfirebasemodule.util.Constant.DEFAULT_LICENSE_PLATE
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class DashManagerConfig @Inject constructor(
    private val ioDispatcher: CoroutineDispatcher
) {
    private val _meterIdentifier: MutableStateFlow<String> = MutableStateFlow(DEFAULT_LICENSE_PLATE)
    val meterIdentifier: MutableStateFlow<String> = _meterIdentifier

    private val _meterLocation: MutableStateFlow<MeterLocation> = MutableStateFlow(defaultMeterLocation)
    val meterLocation: MutableStateFlow<MeterLocation> = _meterLocation

    fun setLicensePlate(licensePlate: String) {
        CoroutineScope(ioDispatcher).launch {
            if (licensePlate.isBlank()) {
                _meterIdentifier.value = DEFAULT_LICENSE_PLATE
            } else if (_meterIdentifier.value != licensePlate) {
                _meterIdentifier.value = licensePlate
            }
        }
    }

    fun setLocation(location: MeterLocation) {
        CoroutineScope(ioDispatcher).launch {
            _meterLocation.value = location
        }
    }

    companion object {
        private val defaultMeterLocation = MeterLocation(GeoPoint(0.0, 0.0), NOT_SET)
    }
}