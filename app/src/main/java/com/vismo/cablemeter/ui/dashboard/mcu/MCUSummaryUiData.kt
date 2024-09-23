package com.vismo.cablemeter.ui.dashboard.mcu

import com.vismo.cablemeter.BuildConfig
import com.vismo.cablemeter.model.DeviceIdData
import com.vismo.cablemeter.model.MCUFareParams
import com.vismo.nxgnfirebasemodule.DashManagerConfig

data class MCUSummaryUiData(
    val vehicleModel: String = "",
    val androidROMVersion: String = "",
    val androidId: String = "",
    val appVersion: String = "v${BuildConfig.VERSION_NAME} (${DashManagerConfig.VERSION_NAME})",
    val fareParams: MCUFareParams = MCUFareParams("", "", "", "", "", "", "", ""),
    val deviceIdData: DeviceIdData = DeviceIdData("", "")
)
