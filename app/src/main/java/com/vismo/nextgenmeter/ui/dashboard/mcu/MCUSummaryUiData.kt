package com.vismo.nextgenmeter.ui.dashboard.mcu

import androidx.compose.ui.graphics.Color
import com.vismo.nextgenmeter.BuildConfig
import com.vismo.nextgenmeter.model.DeviceIdData
import com.vismo.nextgenmeter.model.MCUFareParams
import com.vismo.nextgenmeter.model.OperatingArea
import com.vismo.nextgenmeter.model.Vehicle
import com.vismo.nextgenmeter.ui.theme.urbanRed
import com.vismo.nxgnfirebasemodule.DashManagerConfig

data class MCUSummaryUiData(
    val vehicle: Vehicle? = null,
    val operatingArea: String? = null,
    val operatingAreaColor: Color = urbanRed,
    val androidROMVersion: String = "",
    val androidId: String = "",
    val appVersion: String = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
    val fareParams: MCUFareParams = MCUFareParams("", "", "", "", "", "", "", ""),
    val deviceIdData: DeviceIdData = DeviceIdData("", "")
)