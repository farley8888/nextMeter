package com.vismo.cablemeter.ui.topbar

import com.vismo.cablemeter.BuildConfig
import com.vismo.cablemeter.util.Constant.ENV_DEV
import com.vismo.cablemeter.util.Constant.ENV_PROD
import com.vismo.cablemeter.util.Constant.ENV_QA

data class TopAppBarUiState (
    val title: String = "SUNTEC",
    val dateTime: String = "",
    val isBackButtonVisible: Boolean = false,
    val isLocationIconVisible: Boolean = false,
    val signalStrength: Int = 0,
    val isWifiIconVisible: Boolean = false,
    val driverPhoneNumber: String = "",
    val isInternetIconVisible: Boolean = false,
    val envVariable: String = when(BuildConfig.FLAVOR) {
        ENV_DEV -> "D"
        ENV_QA -> "Q"
        ENV_PROD -> "P"
        else -> "INVALID"
    }
)