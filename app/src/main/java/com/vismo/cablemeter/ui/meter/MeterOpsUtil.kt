package com.vismo.cablemeter.ui.meter

import java.math.BigDecimal
import java.util.Locale

object MeterOpsUtil {
    fun formatToNDecimalPlace(number: Double, n: Int): String {
        val formatString = String.format(Locale.US,"%%.%df", n)
        return String.format(Locale.US, formatString, number)
    }

    fun getDistanceInKm(distanceInMeters: Double): String {
        val distanceInKm = BigDecimal(distanceInMeters).divide(BigDecimal("1000"))
        return formatToNDecimalPlace(distanceInKm.toDouble(), 2)
    }

    fun getFormattedDurationFromSeconds(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return String.format(Locale.US,"%02d:%02d:%02d", hours, minutes, secs)
    }
}