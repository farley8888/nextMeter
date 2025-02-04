package com.vismo.nxgnfirebasemodule.model

import com.google.gson.annotations.SerializedName

data class MeterDeviceProperties(
    @SerializedName("k_value") val kValue: String?,
    @SerializedName("license_plate") val licensePlate: String?,
    @SerializedName("health_check_status") val healthCheckStatus: HealthCheckStatus?,
)

enum class HealthCheckStatus {
    REQUESTED,
    UPDATED,
    APPROVED,
    REJECTED,
    LICENSE_PLATE_SET,
}