package com.vismo.nextgenmeter.model

import com.vismo.nextgenmeter.util.GlobalUtils.divideBy100AndConvertToDouble
import com.vismo.nextgenmeter.util.GlobalUtils.isStopped
import com.vismo.nextgenmeter.util.GlobalUtils.multiplyBy10AndConvertToDouble
import com.vismo.nextgenmeter.util.MeasureBoardUtils

data class OngoingMCUHeartbeatData(
    val measureBoardStatus: Int,
    val lockedDurationHex: String,
    val paidDistanceHex: String,
    val unpaidDistanceHex: String,
    val duration: String,
    val extrasHex: String,
    val fareHex: String,
    val totalFareHex: String,
    val currentTime: String,
    val abnormalPulseCounterHex: String,
    val overspeedCounterHex: String,
    val isStopped: Boolean = measureBoardStatus.isStopped(),
    val tripStatus: TripStatus = if (isStopped) TripStatus.STOP else TripStatus.HIRED,
    val lockedDurationDecimal: Int = 0,
    val paidDistance: Double = 0.0,
    val unpaidDistance: Double = 0.0,
    val extras: Double = 0.0,
    val fare: Double = 0.0,
    val totalFare: Double = 0.0,
    val abnormalPulseCounterDecimal: Int = 0,
    val overspeedCounterDecimal: Int = 0,
    val mcuStatus: Int = measureBoardStatus
) {
    fun processHexValues(): OngoingMCUHeartbeatData? {
        if (!MeasureBoardUtils.isDurationValid(duration)) return null
        MeasureBoardUtils.hexToDecimal(lockedDurationHex).takeIf { it >= 0 } ?: return null
        paidDistanceHex.toDoubleOrNull()?.takeIf { it >= 0.0 } ?: return null
        unpaidDistanceHex.toDoubleOrNull()?.takeIf { it >= 0.0 } ?: return null
        extrasHex.toDoubleOrNull()?.takeIf { it > 0.0 } ?: return null
        fareHex.toDoubleOrNull()?.takeIf { it > 0.0 } ?: return null
        totalFareHex.toDoubleOrNull()?.takeIf { it > 0.0 } ?: return null
        MeasureBoardUtils.hexToDecimal(abnormalPulseCounterHex).takeIf { it >= 0 } ?: return null
        MeasureBoardUtils.hexToDecimal(overspeedCounterHex).takeIf { it >= 0 } ?: return null

        return copy(
            lockedDurationDecimal = MeasureBoardUtils.hexToDecimal(lockedDurationHex),
            paidDistance = paidDistanceHex.multiplyBy10AndConvertToDouble(),
            unpaidDistance = unpaidDistanceHex.multiplyBy10AndConvertToDouble(),
            extras = extrasHex.divideBy100AndConvertToDouble(),
            fare = fareHex.divideBy100AndConvertToDouble(),
            totalFare = totalFareHex.divideBy100AndConvertToDouble(),
            abnormalPulseCounterDecimal = MeasureBoardUtils.hexToDecimal(abnormalPulseCounterHex),
            overspeedCounterDecimal = MeasureBoardUtils.hexToDecimal(overspeedCounterHex)
        )
    }
}

