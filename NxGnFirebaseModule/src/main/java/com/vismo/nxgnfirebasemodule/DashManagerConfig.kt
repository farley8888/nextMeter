package com.vismo.nxgnfirebasemodule

import com.google.firebase.firestore.GeoPoint
import com.vismo.nxgnfirebasemodule.model.MeterLocation
import com.vismo.nxgnfirebasemodule.model.NOT_SET
import com.vismo.nxgnfirebasemodule.util.Constant.DEFAULT_LICENSE_PLATE
import javax.inject.Inject

class DashManagerConfig @Inject constructor() {
    var meterIdentifier: String = DEFAULT_LICENSE_PLATE
    var meterLocation: MeterLocation = DEFAULT_METER_LOCATION

    fun setLicensePlate(licensePlate: String) {
        if (licensePlate.isBlank()) {
            this.meterIdentifier = DEFAULT_LICENSE_PLATE
        } else if (this.meterIdentifier != licensePlate) {
            this.meterIdentifier = licensePlate
        }
    }

    fun setLocation(location: MeterLocation) {
        this.meterLocation = location
    }

    companion object {
        val DEFAULT_METER_LOCATION = MeterLocation(GeoPoint(0.0, 0.0), NOT_SET)
    }
}