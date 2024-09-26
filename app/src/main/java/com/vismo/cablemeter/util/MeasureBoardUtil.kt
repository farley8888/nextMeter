package com.vismo.cablemeter.util

import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.ceil

object MeasureBoardUtils {
    const val BEEP_SOUND_LENGTH =  10 // the unit is 0.01s
    const val WHAT_PRINT_STATUS: Int = 110
    const val READ_DEVICE_ID_DATA = "AC"
    const val TRIP_END_SUMMARY = "E4"
    const val IDLE_HEARTBEAT = "E2"
    const val PARAMETERS_ENQUIRY = "A4"
    const val ABNORMAL_PULSE = "E5"
    const val ONGOING_HEARTBEAT = "E3"
    const val REQUEST_UPGRADE_FIRMWARE = "A8"
    const val UPGRADING_FIRMWARE = "E1"

    fun getTimeInSeconds(duration: String): Long =
        if (duration.length == 6) {
            val hour = duration.substring(0, 2)
            val min = duration.substring(2, 4)
            val sec = duration.substring(4, 6)
            (hour.toLong()) * 60 * 60 + (min.toLong()) * 60 + sec.toLong()
        } else {
            0
        }

    fun getPaidMin(duration: String): BigDecimal =
        if (duration.length == 6) {
            val hour = duration.substring(0, 2)
            val min = duration.substring(2, 4)
            val sec = duration.substring(4, 6)
            BigDecimal(hour.toInt() * 60 + min.toInt() + sec.toDouble() / 60)
        } else {
            BigDecimal(0)
        }

    fun  getResultType(result: String): String? {
        return if (result.startsWith("55AA") && result.endsWith("55AA") && result.length > 16) result.substring(
            14,
            14 + 2
        )
        else null
    }

    fun getUpdateExtrasCmd(amounts: String): String {
        //beep sound
        val durationHex = decimalToHex(BEEP_SOUND_LENGTH).padStart(2,'0')
        val intervalHex = decimalToHex(0).padStart(2,'0')
        val repeatCountHex = decimalToHex(1).padStart(2,'0')

        val formattedAmount = if (amounts.length == 4) {
            "${amounts.substring(0, 4)}00"
        } else if (amounts.length < 4) {
            // append 20 x (expectedLength - decimal length) times if needed
            var result = ""
            if (amounts.length < 4) {
                for (i in 0 until 4 - amounts.length) {
                    result += "0"
                }
            }
            "${result}${amounts}00"
        } else {
            "999900"
        }
        val CMD_EXTRAS = "00 0A 00 00 10 A2 ${formattedAmount.substring(0, 2)} ${formattedAmount.substring(2, 4)} 00 $durationHex $intervalHex $repeatCountHex"
        Log.d("getUpdateExtrasCmd", "addExtras: getUpdateExtrasCmd: $CMD_EXTRAS")
        val checkSum = xorHexStrings(CMD_EXTRAS.trim().split(" "))
        val cmdStringBuilder = StringBuilder()
        cmdStringBuilder.append("55 AA ").append(CMD_EXTRAS).append(checkSum).append(" 55 AA")
        return cmdStringBuilder.toString().replace(" ", "")
    }

    fun generateTripId(): String {
        val uuidWithHyphens = UUID.randomUUID().toString()
        return uuidWithHyphens
    }

    fun getIdWithoutHyphens(uuidWithHyphens: String): String {
        // Return the UUID without hyphens for the hardware
        return uuidWithHyphens.replace("-", "").substring(0, 32)
    }

    fun getStartPauseTripCmd(tripId: String): String {
        return getStartTripCmd(tripId, true)
    }

    fun getStartTripCmd(tripId: String): String {
        return getStartTripCmd(tripId, false)
    }

    private fun getStartTripCmd(tripId: String, isPause: Boolean): String {
        //beep sound
        val durationHex = decimalToHex(BEEP_SOUND_LENGTH).padStart(2,'0')
        val intervalHex = decimalToHex(0).padStart(2,'0')
        val repeatCountHex = decimalToHex(1).padStart(2,'0')

        val mTripId = encodeHexString(tripId)
        val isPauseFlag = if (isPause) "01" else "00"
        val CMD_START = "00 28 00 00 10 A0 $isPauseFlag $mTripId $durationHex $intervalHex $repeatCountHex"
        Log.d("getStartTripCmd()", "startTrip: getStartTripCmd(): $CMD_START")
        val checkSum = xorHexStrings(CMD_START.trim().split(" "))
        val cmdStringBuilder = StringBuilder()
        cmdStringBuilder.append("55 AA ").append(CMD_START).append(checkSum).append(" 55 AA")
        return cmdStringBuilder.toString().replace(" ", "")
    }

    fun getPauseTripCmd(): String {
        //beep sound
        val durationHex = decimalToHex(BEEP_SOUND_LENGTH).padStart(2,'0')
        val intervalHex = decimalToHex(0).padStart(2,'0')
        val repeatCountHex = decimalToHex(1).padStart(2,'0')

        val CMD_PAUSE = "00 08 00 00 10 A1 01 $durationHex $intervalHex $repeatCountHex"
        //55 AA 00 05 00 00 10 A1 00 B4 55 AA
        val checkSum = xorHexStrings(CMD_PAUSE.trim().split(" "))
        val cmdStringBuilder = StringBuilder()
        cmdStringBuilder.append("55 AA ").append(CMD_PAUSE).append(checkSum).append(" 55 AA")
        return cmdStringBuilder.toString().replace(" ", "")
    }

    fun getContinueTripCmd(isMute:Boolean = false): String {
        //beep sound
        val durationHex = decimalToHex(if (isMute) 0 else BEEP_SOUND_LENGTH).padStart(2,'0')
        val intervalHex = decimalToHex(0).padStart(2,'0')
        val repeatCountHex = decimalToHex(if (isMute) 0 else 1).padStart(2,'0')

        val CMD_CONTINUE = "00 08 00 00 10 A1 00 $durationHex $intervalHex $repeatCountHex"
        //55 AA 00 05 00 00 10 A1 00 B4 55 AA
        val checkSum = xorHexStrings(CMD_CONTINUE.trim().split(" "))
        val cmdStringBuilder = StringBuilder()
        cmdStringBuilder.append("55 AA ").append(CMD_CONTINUE).append(checkSum).append(" 55 AA")
        return cmdStringBuilder.toString().replace(" ", "")
    }

    fun getEndTripCmd(): String {
        //beep sound
        val durationHex = decimalToHex(BEEP_SOUND_LENGTH).padStart(2,'0')
        val intervalHex = decimalToHex(0).padStart(2,'0')
        val repeatCountHex = decimalToHex(1).padStart(2,'1')

        val CMD_END = "00 08 00 00 10 A3 01 $durationHex $intervalHex $repeatCountHex"
        //55 AA 00 0B 00 00 10 A3 20 23 02 25 20 47 01 FA 55 AA
        val checkSum = xorHexStrings(CMD_END.trim().split(" "))
        val cmdStringBuilder = StringBuilder()
        cmdStringBuilder.append("55 AA ").append(CMD_END).append(checkSum).append(" 55 AA")
        return cmdStringBuilder.toString().replace(" ", "")
    }

    fun getUpdateTimeCmd(formattedDateStr: String): String {
        val formattedDateTime = if (isDateTimeFormatValid(formattedDateStr))
            formattedDateStr.chunked(2).joinToString(" ")
        else if (isDateTimeFormatValid("${formattedDateStr}00")) // handle the case when the time is not in the correct format, i.e. the datetime obtained from the heartbeat
            "${formattedDateStr}00".chunked(2).joinToString(" ")
        else
            "2024-01-01T16:17:18".chunked(2).joinToString(" ")
        val formattedKValue = "10 00" //this kValue won't be applied to the measure board
        val formattedPowerOffTime = "00 02" // 30mins
        val CMD_UPDATE_PARAMETERS = "00 10 00 00 10 A5 01 $formattedDateTime $formattedKValue $formattedPowerOffTime"
        val checkSum = xorHexStrings(CMD_UPDATE_PARAMETERS.trim().split(" "))
        val cmdStringBuilder = StringBuilder()
        cmdStringBuilder.append("55 AA ").append(CMD_UPDATE_PARAMETERS).append(checkSum).append(" 55 AA")
        return cmdStringBuilder.toString().replace(" ", "")
    }

    fun getUpdateKValueCmd(kValue: Int): String {
        val parsedDate = LocalDateTime.parse("2024-01-01T16:17:18", DateTimeFormatter.ISO_DATE_TIME) // a random date to be placed here, it won't be used to update the time in measureboard
        val formattedDateTime = parsedDate.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")).chunked(2).joinToString(" ")
        val formattedKValue = kValue.toString().padStart(4, '0').chunked(2).joinToString(" ")
        val formattedPowerOffTime = "00 02" // 30mins
        val CMD_UPDATE_PARAMETERS = "00 10 00 00 10 A5 06 $formattedDateTime $formattedKValue $formattedPowerOffTime"
        val checkSum = xorHexStrings(CMD_UPDATE_PARAMETERS.trim().split(" "))
        val cmdStringBuilder = StringBuilder()
        cmdStringBuilder.append("55 AA ").append(CMD_UPDATE_PARAMETERS).append(checkSum).append(" 55 AA")
        return cmdStringBuilder.toString().replace(" ", "")
    }

    fun getBeepSoundCmd(durationIn10ms:Int,intervalIn10ms: Int, repeatCount: Int):String{
        val durationHex = decimalToHex(durationIn10ms).padStart(2,'0')
        val intervalHex = decimalToHex(intervalIn10ms).padStart(2,'0')
        val repeatCountHex = decimalToHex(repeatCount).padStart(2,'0')
        val CMD_BEEP_SOUND = "00 07 00 00 10 AB $durationHex $intervalHex $repeatCountHex"
        Log.d("DurationInSecond", "DurationInSecond: DurationInSecond(): $durationIn10ms -> $durationHex")
        val checkSum = xorHexStrings(CMD_BEEP_SOUND.trim().split(" "))
        val cmdStringBuilder = StringBuilder()
        cmdStringBuilder.append("55 AA ").append(CMD_BEEP_SOUND).append(checkSum).append(" 55 AA")
        return cmdStringBuilder.toString().replace(" ", "")
    }

    fun getUnlockCmd(): String {
        val CMD_UNLOCK = "00 05 00 00 10 AA 90"
        val checkSum = xorHexStrings(CMD_UNLOCK.trim().split(" "))
        val cmdStringBuilder = StringBuilder()
        cmdStringBuilder.append("55 AA ").append(CMD_UNLOCK).append(checkSum).append(" 55 AA")
        return cmdStringBuilder.toString().replace(" ", "")
    }

    /**
     * Get Device ID from the measure board
     */
    fun getMeasureBoardDeviceIdCmd():String{
        val readCommand = "55"
//            val data = "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 01"
        val CMD_READ_DEVICE_ID = "00 05 00 00 10 AC $readCommand"
        val checkSum = xorHexStrings(CMD_READ_DEVICE_ID.trim().split(" "))
        val cmdStringBuilder = StringBuilder()
        cmdStringBuilder.append("55 AA ").append(CMD_READ_DEVICE_ID).append(checkSum).append(" 55 AA")
        return cmdStringBuilder.toString().replace(" ", "")
    }

    /**
     * Write data into the measure board
     */
    fun getWritingDataIntoMeasureBoardCmd(licensePlate:String):String{
        val writeCommand = "AA"
//            val licensePlateLength = licensePlate.length.toString().padStart(2,'0')
        val formattedLicensePlate = encodeHexString(licensePlate).replace(" ","").padStart(16,'F').chunked(2).joinToString(" ")
        val data = "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF $formattedLicensePlate"// max. 32 bytes
        val CMD_WRITE_DATA = "00 25 00 00 10 AC $writeCommand $data"
        val checkSum = xorHexStrings(CMD_WRITE_DATA.trim().split(" "))
        val cmdStringBuilder = StringBuilder()
        cmdStringBuilder.append("55 AA ").append(CMD_WRITE_DATA).append(checkSum).append(" 55 AA")
        return cmdStringBuilder.toString().replace(" ", "")
    }

    /**
     * update MCU params
     */
    fun getUpdatePriceParamCmd(startPrice: Int, stepPrice: Int, stepPrice2nd:Int, threshold:Int): String {
        val paramVersion = "240714A1".padStart(8, '0').chunked(2).joinToString(" ")
        val startDistance = "0200".padStart(4, '0').chunked(2).joinToString(" ")
        val mStartPrice = startPrice.toString().padStart(4, '0').chunked(2).joinToString(" ")
        val startPricePeak = startPrice.toString().padStart(4, '0').chunked(2).joinToString(" ")
        val mStepPrice = stepPrice.toString().padStart(4, '0').chunked(2).joinToString(" ")
        val stepPricePeak = stepPrice.toString().padStart(4, '0').chunked(2).joinToString(" ")
        val mThreshold = threshold.toString().padStart(4, '0').chunked(2).joinToString(" ")
        val mStepPrice2nd = stepPrice2nd.toString().padStart(4, '0').chunked(2).joinToString(" ")
        val mStepPrice2ndPeak = stepPrice2nd.toString().padStart(4, '0').chunked(2).joinToString(" ")

        val CMD_UPDATE_PARAMETERS = "00 26 00 00 10 A6 $paramVersion $startDistance $mStartPrice $startPricePeak $mStepPrice $stepPricePeak 08 00 10 30 17 00 19 30 $mThreshold $mStepPrice2nd $mStepPrice2ndPeak 00 20 00 60 01 50"
        val checkSum = xorHexStrings(CMD_UPDATE_PARAMETERS.trim().split(" "))?.padStart(2, '0')
        val cmdStringBuilder = StringBuilder()
        cmdStringBuilder.append("55 AA ").append(CMD_UPDATE_PARAMETERS).append(checkSum).append(" 55 AA")
        return cmdStringBuilder.toString().replace(" ", "")
    }

    /**
     * Decode the HEX into ASCII format
     */
    fun convertToASCIICharacters(hexString: String): String? {
        return hexString.replace(" ","").replace("FF","").decodeHex()
    }

    private fun String.decodeHex(): String {
        require(length % 2 == 0) {"Must have an even length"}
        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
            .toString(Charsets.ISO_8859_1)  // Or whichever encoding your input uses
    }

    /**
     * Encode the character in HEX format
     */
    fun encodeHexString(characters: String): String {
        val sb = StringBuilder()
        for (i in 0 until characters.length) {
            sb.append(Integer.toHexString(characters[i].toInt()))
        }
        return sb.toString().chunked(2).joinToString(" ")
    }


    //hex to decimal number
    fun hexToDecimal(hex: String): Int {
        var result = 0
        for (element in hex) {
            result = result * 16 + element.toString().toInt(16)
        }
        return result
    }

    //decimal to hex
    fun decimalToHex(decimal: Int): String {
        return Integer.toHexString(decimal).uppercase()
    }

    private fun xorHexStrings(hexStrings: List<String>): String? {
        Log.d("xorHexStrings", "xorHexStrings =>> ${hexStrings}")
        if (hexStrings.isEmpty()) {
            return null
        }

        var result = hexStrings[0].toInt(16)

        for (i in 1 until hexStrings.size) {
            result = result xor hexStrings[i].toInt(16)
        }

        return result.toString(16).toUpperCase()
    }

    fun getFirmwareBatchSize(firmwarePath: String):Int?{
        var result:Int?=null
        val file = File(firmwarePath)
        var rdFile: RandomAccessFile?=null
        try{
            rdFile = RandomAccessFile(file, "rw")
            result =  ceil(rdFile.length()/2048.0).toInt()

        }catch (e:Exception){
            Log.e("getFirmwareBatchSize", "error found", e)
        }finally{
            rdFile?.close()
        }
        return result
    }

    fun getPatchMeterBoardFirmwareCmd(firmwarePath: String, version: String, offset:Int):ByteArray?{

        var result:ByteArray?=null
        val file = File(firmwarePath)
        var rdFile: RandomAccessFile?=null

        try{
            rdFile = RandomAccessFile(file, "rw")
            if (rdFile!=null){

                val fileBytes = ByteArray(2048)
                rdFile.seek(offset.toLong()*2048)
                val readLen = rdFile.read(fileBytes)

                if (readLen < 2048) {
                    fileBytes.fill(0xFF.toByte(), readLen, 2048) // Pad with 0xFF if needed
                }

                if (readLen >-1){
                    var num: Short = offset.toShort()
                    val head = byteArrayOf(0x55, 0xAA.toByte())  // Use toByte() for byte literals
                    val len = ByteBuffer.allocate(2).putShort(2063).array() //ByteArray(2063) { 0xFF.toByte() }
                    val type = byteArrayOf(0x00)
                    val pro = byteArrayOf(0x00)
                    val cmd = byteArrayOf(0x00, 0xE1.toByte())
                    val jieguo = byteArrayOf(0x90.toByte())
                    val banben = convertFirmwareVersionToBytes(version) //byteArrayOf(0x23, 0x08, 0x25, 0x01)
                    val baohao = ByteBuffer.allocate(2).putShort(num.toShort()).array()
                    var crc = byteArrayOf(0x00)
                    val end = byteArrayOf(0x55, 0xAA.toByte())
                    val dataCrc = uintToByteArray(crcInt(fileBytes))
                    val allBytes = concatByteArrays(len, type, pro, cmd, jieguo, banben, baohao, fileBytes,dataCrc)
                    crc = byteArrayOf(crc(allBytes))
                    result = concatByteArrays(head,allBytes,crc,end)

                }

            }

        }catch (e:Exception){
            Log.e("getPatchMeterBoardFirmwareCmd", "error found", e)
        }finally{
            rdFile?.close()
        }
        return result
    }

    fun getRequestPatchMeterBoardFirmwareCmd(firmwarePath: String, version: String):ByteArray?{
        var result:ByteArray?=null
        val file = File(firmwarePath)
        var rdFile: RandomAccessFile?=null
        try{

            rdFile = RandomAccessFile(file, "rw")
            if (rdFile!=null){

                var allByte: ByteArray

                val head = byteArrayOf(0x55, 0xAA.toByte())  // Use toByte() for byte literals
                var num: Short = 10 // Declare with explicit type for clarity
                val len = ByteBuffer.allocate(2).putShort(num).array()
                val type = byteArrayOf(0x02)
                val pro = byteArrayOf(0x01)
                val cmd = byteArrayOf(0x10, 0xA8.toByte())

                val banben = convertFirmwareVersionToBytes(version) //byteArrayOf(0x23, 0x08, 0x25, 0x01)  // Assuming 0x01 was intended
                num = (rdFile.length() / 2048 + 1).toShort()  // Explicit conversion to Short

                val baohao = ByteBuffer.allocate(2).putShort(num).array()
                Log.d("getRequestPatchMeterBoardFirmwareCmd", "no of batch ${num} ${baohao.toHexString()}")
                var crc = byteArrayOf(0x00)
                val end = byteArrayOf(0x55, 0xAA.toByte())
                allByte = concatByteArrays(len,type,pro,cmd,banben,baohao)
                crc[0] = crc(allByte)
                result = concatByteArrays(head,allByte,crc,end)
                Log.d("getInitPatchMeterBoardFirmwareCmd", "######################## ->>> "+result.toHexString())
            }
        }catch (e:Exception){
            Log.e("getInitPatchMeterBoardFirmwareCmd", "error found", e)
        }finally {
            rdFile?.close()
        }
        return result
    }

    private fun convertFirmwareVersionToBytes(version: String): ByteArray {
        if (version.length!=8){
            throw RuntimeException("expected version name in 8 char numeric formac")
        }
        val byteArray = version.chunked(2) // Split into pairs of hex digits
            .map { it.toInt(16).toByte() }

        return byteArray.toByteArray()
    }

    private fun concatByteArrays(vararg arrays: ByteArray): ByteArray {
        val totalLength = arrays.sumOf { it.size }
        val result = ByteArray(totalLength)
        var offset = 0
        for (array in arrays) {
            System.arraycopy(array, 0, result, offset, array.size) // Use System.arraycopy for correct copying
            offset += array.size
        }
        return result
    }

    fun formatHexString(hexString: String): String {
        return hexString.chunked(2) // Split into pairs of characters
            .joinToString(" ") { it.toUpperCase() } // Capitalize and add spaces
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

    fun crc(data: ByteArray): Byte {
        var temp: Byte = 0x00
        for (i in data.indices) {
            temp = (temp.toInt() xor data[i].toInt()).toByte()
        }
        return temp
    }

    private fun crcInt(data: ByteArray): UInt {
        var temp = 0x00
        for (i in data.indices) {
            temp = temp + (data[i].toInt() and 0xFF)
        }
        return (temp and -0x1).toUInt()
    };

    private fun uintToByteArray(uint: UInt): ByteArray {
        // Allocate a buffer of 4 bytes
        val buffer = ByteBuffer.allocate(4)
        // Put the uint value into the buffer
        buffer.putInt(uint.toInt())
        // Get the byte array from the buffer
        return buffer.array()
    }

    fun isDateTimeFormatValid(datetime: String): Boolean {
        val format = SimpleDateFormat("yyyyMMddHHmmss")
        format.isLenient = false

        return try {
            format.parse(datetime)
            true
        } catch (e: Exception) {
            false
        }
    }
}
