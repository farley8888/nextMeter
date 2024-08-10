package com.vismo.cablemeter.model

data class TopAppBarUiState (
    val title: String = "SUNTEC",
    val dateTime: String = "",
    val isBackButtonVisible: Boolean = false,
    val isGPSIconVisible: Boolean = false,
    val signalStrength: Int = 0,
    val isWifiIconVisible: Boolean = false,
    val driverPhoneNumber: String = "",
    val isInternetIconVisible: Boolean = false,
)