package com.vismo.nxgnfirebasemodule.model

import com.google.gson.annotations.SerializedName

data class MeterSdkConfiguration(
    @SerializedName("common") val common: CommonConfig,
    @SerializedName("dash_fees") val dashFeesConfig: OperatingAreaConfig,
    @SerializedName("wifi_credential") val wifiCredential: WifiCredential?,
)


data class CommonConfig(
    @SerializedName("heartbeat_interval") val heartbeatInterval: Int,
    @SerializedName("dash_fee_rate") val dashFeeRate: Double,
    @SerializedName("dash_fee_constant") val dashFeeConstant: Double,
)

data class OperatingAreaConfig(
    @SerializedName("LANTAU") val lantau: DashFeeItem,
    @SerializedName("NT") val nt: DashFeeItem,
    @SerializedName("URBAN") val urban: DashFeeItem
)

data class DashFeeItem(
    @SerializedName("dash_fee_constant") val dashFeeConstant: Double,
    @SerializedName("dash_fee_rate") val dashFeeRate: Double,
    @SerializedName("operating_area") val operatingArea: OperatingArea
)

data class WifiCredential(
    @SerializedName("ssid") val ssid: String,
    @SerializedName("password") val password: String
)


