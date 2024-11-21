package com.vismo.nextgenmeter.model

import com.vismo.nextgenmeter.util.GlobalUtils.divideBy100AndConvertToDouble
import com.vismo.nextgenmeter.util.GlobalUtils.multiplyBy10AndConvertToDouble
import com.vismo.nextgenmeter.util.MeasureBoardUtils

data class OngoingMCUHeartbeatData(
    val measureBoardStatus: Int,
    val lockedDurationHex: String,
    val distanceHex: String,
    val duration: String,
    val extrasHex: String,
    val fareHex: String,
    val totalFareHex: String,
    val currentTime: String,
    val abnormalPulseCounterHex: String,
    val overspeedCounterHex: String,
    val isStopped: Boolean = measureBoardStatus == 1,
    val tripStatus: TripStatus = if (isStopped) TripStatus.STOP else TripStatus.HIRED,
    val lockedDurationDecimal: Int = 0,
    val distance: Double = 0.0,
    val extras: Double = 0.0,
    val fare: Double = 0.0,
    val totalFare: Double = 0.0,
    val abnormalPulseCounterDecimal: Int = 0,
    val overspeedCounterDecimal: Int = 0,
    val mcuStatus: Int = measureBoardStatus
) {
    fun processHexValues(): OngoingMCUHeartbeatData {
        return copy(
            lockedDurationDecimal = MeasureBoardUtils.hexToDecimal(lockedDurationHex),
            distance = distanceHex.multiplyBy10AndConvertToDouble(),
            extras = extrasHex.divideBy100AndConvertToDouble(),
            fare = fareHex.divideBy100AndConvertToDouble(),
            totalFare = totalFareHex.divideBy100AndConvertToDouble(),
            abnormalPulseCounterDecimal = MeasureBoardUtils.hexToDecimal(abnormalPulseCounterHex),
            overspeedCounterDecimal = MeasureBoardUtils.hexToDecimal(overspeedCounterHex)
        )
    }
}

