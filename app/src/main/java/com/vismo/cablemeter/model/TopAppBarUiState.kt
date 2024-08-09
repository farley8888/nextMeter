package com.vismo.cablemeter.model

data class TopAppBarUiState (
    val title: String = "SUNTEC",
    val date: String = "",
    val showBackButton: Boolean = false,
    val showGPSIcon: Boolean = false,
    val signalStrength: Int = 0,
    val showWifiIcon: Boolean = false,
    val driverPhoneNumber: String = "",
    val showInternetIcon: Boolean = false,
)