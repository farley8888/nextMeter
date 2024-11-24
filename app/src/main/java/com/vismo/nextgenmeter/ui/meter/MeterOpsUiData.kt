package com.vismo.nextgenmeter.ui.meter

import androidx.compose.ui.graphics.Color
import com.vismo.nextgenmeter.ui.theme.red

data class MeterOpsUiData(
    val status: TripStateInMeterOpsUI,
    val totalFare: String,
    val fare: String,
    val extras: String = "0.0",
    val distanceInKM: String,
    val duration: String,
    val languagePref: TtsLanguagePref = TtsLanguagePref.OFF,
    val totalColor: Color = red,
    val remainingOverSpeedTimeInSeconds: String? = null,
    val overSpeedDurationInSeconds: Int = 0,
    val mcuStartingPrice: String = "",
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
    fun toLanguageCode(): String {
        return when (this) {
            EN -> KEY_EN
            ZH_CN -> KEY_ZH_CN
            ZH_HK -> KEY_ZH_HK
            OFF -> ""
        }
    }
    object EN : TtsLanguagePref()
    object ZH_CN : TtsLanguagePref()
    object ZH_HK : TtsLanguagePref()
    object OFF : TtsLanguagePref()

    companion object {
        const val KEY_EN = "en"
        const val KEY_ZH_CN = "zh"
        const val KEY_ZH_HK = "zh-rHK"
    }
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
data object PastTrip : TripStateInMeterOpsUI() {
    override fun toStringEN(): String {
        return "FOR HIRE"
    }
    override fun toStringCN(): String {
        return "空"
    }
}