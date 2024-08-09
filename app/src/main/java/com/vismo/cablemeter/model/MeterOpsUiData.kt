package com.vismo.cablemeter.model

data class MeterOpsUiData(
    val status: TripStateInMeterOpsUI,
    val fare: String,
    val extras: String,
    val distanceInKM: String,
    val duration: String,
)

sealed class TripStateInMeterOpsUI {
    open fun toStringEN(): String {
        return ""
    }
    open fun toStringCN(): String {
        return ""
    }
}
data object ForHire : TripStateInMeterOpsUI() {
    override fun toStringEN(): String {
        return "FOR HIRE"
    }
    override fun toStringCN(): String {
        return "空"
    }
}
data object Hired : TripStateInMeterOpsUI() {
    override fun toStringEN(): String {
        return "HIRED"
    }
    override fun toStringCN(): String {
        return "往"
    }
}
data object Paused : TripStateInMeterOpsUI() {
    override fun toStringEN(): String {
        return "STOPPED"
    }
    override fun toStringCN(): String {
        return "停"
    }
}