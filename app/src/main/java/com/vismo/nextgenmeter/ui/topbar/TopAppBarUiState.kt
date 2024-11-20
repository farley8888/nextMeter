package com.vismo.nextgenmeter.ui.topbar

import androidx.compose.ui.graphics.Color
import com.vismo.nextgenmeter.BuildConfig
import com.vismo.nextgenmeter.ui.theme.nobel600
import com.vismo.nextgenmeter.util.Constant.ENV_DEV
import com.vismo.nextgenmeter.util.Constant.ENV_DEV_2
import com.vismo.nextgenmeter.util.Constant.ENV_PROD
import com.vismo.nextgenmeter.util.Constant.ENV_QA

data class TopAppBarUiState (
    val title: String = "SUNTEC",
    val dateTime: String = "",
    val isBackButtonVisible: Boolean = false,
    val isLocationIconVisible: Boolean = false,
    val signalStrength: Int = 0,
    val isWifiIconVisible: Boolean = false,
    val driverPhoneNumber: String = "",
    val isInternetIconVisible: Boolean = false,
    val color: Color = nobel600,
    val envVariable: String = when(BuildConfig.FLAVOR) {
        ENV_DEV -> "D"
        ENV_DEV_2 -> "D2"
        ENV_QA -> "Q"
        ENV_PROD -> "P"
        else -> "INVALID"
    },
    val showLoginToggle:Boolean = false,
)