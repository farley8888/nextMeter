package com.vismo.cablemeter.ui.meter

data class MeterOpsUiData(
    val status: TripStateInMeterOpsUI,
    val totalFare: String,
    val fare: String,
    val extras: String,
    val distanceInKM: String,
    val duration: String,
    val languagePref: TtsLanguagePref = TtsLanguagePref.OFF
)

sealed class TtsLanguagePref {
    override fun toString(): String {
        return when (this) {
            EN -> "英"
            ZH_CN -> "國"
            ZH_HK -> "粵"
            OFF -> ""
        }
    }
    object EN : TtsLanguagePref()
    object ZH_CN : TtsLanguagePref()
    object ZH_HK : TtsLanguagePref()
    object OFF : TtsLanguagePref()
}

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