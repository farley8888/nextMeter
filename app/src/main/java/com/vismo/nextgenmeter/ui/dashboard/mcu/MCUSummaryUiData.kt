package com.vismo.nextgenmeter.ui.dashboard.mcu

import com.vismo.nextgenmeter.BuildConfig
import com.vismo.nextgenmeter.model.DeviceIdData
import com.vismo.nextgenmeter.model.MCUFareParams
import com.vismo.nxgnfirebasemodule.DashManagerConfig

data class MCUSummaryUiData(
    val vehicleModel: String = "",
    val androidROMVersion: String = "",
    val androidId: String = "",
    val appVersion: String = "v${BuildConfig.VERSION_NAME} (${DashManagerConfig.VERSION_NAME})",
    val fareParams: MCUFareParams = MCUFareParams("", "", "", "", "", "", "", ""),
    val deviceIdData: DeviceIdData = DeviceIdData("", "")
)
