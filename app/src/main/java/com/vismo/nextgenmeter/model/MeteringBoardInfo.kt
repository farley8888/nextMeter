package com.vismo.nextgenmeter.model

import com.google.gson.annotations.SerializedName
import com.vismo.nextgenmeter.util.MeasureBoardUtils

/**
 * Data model for the metering board information response (0x10B0 command)
 * This contains key information obtained from the metering board after app startup
 */
data class MeteringBoardInfo(
    @SerializedName("metering_plate_status") val meteringPlateStatus: Int,
    @SerializedName("mcu_time") val mcuTime: String,
    @SerializedName("k_value") val kValue: String,
    @SerializedName("metering_plate_id") val meteringPlateId: String,
    @SerializedName("memory_data") val memoryData: String,
    @SerializedName("trip_id") val tripId: String,
    @SerializedName("android_board_power_off_time") val androidBoardPowerOffTime: String,
) {
    
    /**
     * Parses the hex response from the metering board into a MeteringBoardInfo object
     * @param result The hex string response from the metering board
     * @return MeteringBoardInfo object or null if parsing fails
     */
    companion object {
        fun parseFromHexResponse(result: String): MeteringBoardInfo? {
            // Expected minimum length: 55AA prefix + response type + data + checksum + 55AA suffix
            // Response should be at least 55AA + 02 + (response type) + 81 bytes of data + checksum + 55AA
            if (result.length < 170 || !result.startsWith("55AA") || !result.endsWith("55AA")) {
                return null
            }
            
            // Extract the data portion (skip 55AA prefix and response headers)
            val dataStartIndex = 16 // Skip 55AA + length + type + flags + command
            val dataHex = result.substring(dataStartIndex, dataStartIndex + 162) // 81 bytes * 2 = 162 hex chars
            
            if (dataHex.length < 162) {
                return null
            }
            
            try {
                // Parse according to the protocol specification
                val meteringPlateStatus = MeasureBoardUtils.hexToDecimal(dataHex.substring(0, 2))
                val mcuTime = dataHex.substring(2, 16) // 7 bytes = 14 hex chars
                val kValue = dataHex.substring(16, 20) // 2 bytes = 4 hex chars
                val meteringPlateId = dataHex.substring(20, 30) // 5 bytes = 10 hex chars
                val memoryData = dataHex.substring(30, 94) // 32 bytes = 64 hex chars
                val tripIdHex = dataHex.substring(94, 158) // 32 bytes = 64 hex chars
                val androidBoardPowerOffTime = dataHex.substring(158, 162) // 2 bytes = 4 hex chars
                
                // Convert trip ID from hex to ASCII
                val tripId = MeasureBoardUtils.convertToASCIICharacters(tripIdHex) ?: ""
                
                return MeteringBoardInfo(
                    meteringPlateStatus = meteringPlateStatus,
                    mcuTime = mcuTime,
                    kValue = kValue,
                    meteringPlateId = meteringPlateId,
                    memoryData = memoryData,
                    tripId = tripId,
                    androidBoardPowerOffTime = androidBoardPowerOffTime
                )
            } catch (e: Exception) {
                return null
            }
        }
    }
    
    /**
     * Gets the formatted MCU time in readable format
     */
    fun getFormattedMcuTime(): String {
        return if (MeasureBoardUtils.isValidDate(mcuTime, "yyyyMMddHHmmss")) {
            // Format: YYYYMMDDHHMMSS -> YYYY-MM-DD HH:MM:SS
            val year = mcuTime.substring(0, 4)
            val month = mcuTime.substring(4, 6)
            val day = mcuTime.substring(6, 8)
            val hour = mcuTime.substring(8, 10)
            val minute = mcuTime.substring(10, 12)
            val second = mcuTime.substring(12, 14)
            "$year-$month-$day $hour:$minute:$second"
        } else {
            "Invalid time format"
        }
    }
    
    /**
     * Gets the metering plate status as a readable string
     */
    fun getMeteringPlateStatusString(): String {
        return when (meteringPlateStatus) {
            0x00 -> "Empty"
            0x01 -> "To"
            0x02 -> "Stop"
            0x03 -> "Locked"
            else -> "Unknown ($meteringPlateStatus)"
        }
    }
    
    /**
     * Gets the formatted K value
     */
    fun getFormattedKValue(): String {
        return try {
            val kValueDecimal = MeasureBoardUtils.hexToDecimal(kValue)
            kValueDecimal.toString()
        } catch (e: Exception) {
            "Invalid K value"
        }
    }
    
    /**
     * Gets the formatted android board power off time in minutes
     */
    fun getFormattedPowerOffTime(): String {
        return try {
            val powerOffMinutes = MeasureBoardUtils.hexToDecimal(androidBoardPowerOffTime)
            "$powerOffMinutes"
        } catch (e: Exception) {
            "Invalid power off time"
        }
    }
} 