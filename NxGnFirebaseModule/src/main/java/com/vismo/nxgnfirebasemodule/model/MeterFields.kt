package com.vismo.nxgnfirebasemodule.model

import com.google.gson.annotations.SerializedName

data class MeterFields(
    @SerializedName("settings") val settings: Settings,
    @SerializedName("mcu_info") val mcuInfo: McuInfo,
)


data class Settings(
    @SerializedName("dash_fee_constant") val dashFeeConstant: Number,
    @SerializedName("dash_fee_rate") val dashFeeRate: Number,
    @SerializedName("heartbeat_interval") val heartbeatInterval: Int,
    @SerializedName("meter_software_version") val model: String,
    @SerializedName("sdk_version") val operatingArea: String,
    @SerializedName("show_login_toggle") val showLoginToggle: Boolean,
    @SerializedName("sim_iccid") val simIccid: String,
)

data class McuInfo(
    @SerializedName("firmware_version") val firmwareVersion: String,
    @SerializedName("k_value") val kValue: String,
    @SerializedName("starting_distance") val startingDistance: String,
    @SerializedName("starting_price") val startingPrice: String,
    @SerializedName("step_price") val stepPrice: String,
    @SerializedName("changed__step_price") val changedPriceAt: String,
    @SerializedName("step_price_change_at") val changedStepPrice: String,
)