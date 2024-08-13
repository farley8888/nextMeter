package com.vismo.nxgnfirebasemodule

import com.google.firebase.firestore.GeoPoint
import com.vismo.nxgnfirebasemodule.model.MeterLocation
import com.vismo.nxgnfirebasemodule.model.NOT_SET
import com.vismo.nxgnfirebasemodule.util.Constant.DEFAULT_LICENSE_PLATE
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class DashManagerConfig @Inject constructor() {
    var meterIdentifier: String = DEFAULT_LICENSE_PLATE

    private val defaultMeterLocation = MeterLocation(GeoPoint(0.0, 0.0), NOT_SET)
    private val _meterLocation: MutableStateFlow<MeterLocation> = MutableStateFlow(defaultMeterLocation)
    val meterLocation: MutableStateFlow<MeterLocation> = _meterLocation

    fun setLicensePlate(licensePlate: String) {
        if (licensePlate.isBlank()) {
            this.meterIdentifier = DEFAULT_LICENSE_PLATE
        } else if (this.meterIdentifier != licensePlate) {
            this.meterIdentifier = licensePlate
        }
    }

    fun setLocation(location: MeterLocation) {
        this._meterLocation.value = location
    }
}