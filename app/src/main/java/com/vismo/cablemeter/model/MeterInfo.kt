package com.vismo.cablemeter.model

import com.google.gson.annotations.SerializedName


data class MeterInfo(
    @SerializedName("settings") val settings: Settings?,
    @SerializedName("mcu_info") val mcuInfo: McuInfo?,
    @SerializedName("session") val session: Session?,
)

data class Session(
    @SerializedName("id") val sessionId: String,
    @SerializedName("driver") val driver: Driver,
)

data class Driver(
    @SerializedName("id") val driverPhoneNumber: String,
    @SerializedName("name") val driverName: String,
    @SerializedName("name_ch") val driverChineseName: String,
    @SerializedName("driver_license") val driverLicense: String,
    @SerializedName("license_plate") val licensePlate: String,
)

data class Settings(
    @SerializedName("dash_fee_constant") val dashFeeConstant: Double,
    @SerializedName("dash_fee_rate") val dashFeeRate: Double,
    @SerializedName("heartbeat_interval") val heartbeatInterval: Int,
    @SerializedName("meter_software_version") val meterSoftwareVersion: String,
    @SerializedName("sdk_version") val sdkVersion: String,
    @SerializedName("show_login_toggle") val showLoginToggle: Boolean,
    @SerializedName("sim_iccid") val simIccid: String,
    @SerializedName("vehicle") val vehicle: Vehicle,
    @SerializedName("operating_area") val operatingArea: OperatingArea,
)

data class McuInfo(
    @SerializedName("firmware_version") val firmwareVersion: String,
    @SerializedName("k_value") val kValue: String,
    @SerializedName("starting_distance") val startingDistance: String,
    @SerializedName("start_price") val startingPrice: String,
    @SerializedName("step_price") val stepPrice: String,
    @SerializedName("changed_step_price") val changedPriceAt: String,
    @SerializedName("step_price_change_at") val changedStepPrice: String,
)

data class Vehicle(
    @SerializedName("make") val make: String,
    @SerializedName("model") val model: String,
)

enum class OperatingArea {
    LANTAU,
    NT,
    URBAN
}