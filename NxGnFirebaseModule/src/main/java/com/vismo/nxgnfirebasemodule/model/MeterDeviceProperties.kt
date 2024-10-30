package com.vismo.nxgnfirebasemodule.model

import com.google.gson.annotations.SerializedName

data class MeterDeviceProperties(
    @SerializedName("is_health_check_complete") val isHealthCheckComplete: Boolean?,
    @SerializedName("k_value") val kValue: String?,
    @SerializedName("license_plate") val licensePlate: String?,
)