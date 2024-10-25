package com.vismo.cablemeter.repository

import com.vismo.cablemeter.module.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.apache.commons.net.ntp.NTPUDPClient
import org.apache.commons.net.ntp.TimeInfo
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import javax.inject.Inject

class NetworkTimeRepository @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
){
    suspend fun fetchNetworkTime(): String? = withContext(ioDispatcher) {
        try {
            val client = NTPUDPClient()
            client.defaultTimeout = 10000 // 10 seconds timeout
            client.open()

            val address = InetAddress.getByName(SERVER)
            val timeInfo: TimeInfo = client.getTime(address)
            client.close()

            // Directly use the NTP time
            val ntpTime = timeInfo.message.transmitTimeStamp.time

            return@withContext ntpTimestampToHongKongTimeStr(ntpTime)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun ntpTimestampToHongKongTimeStr(ntpTimeMillis: Long): String {
//        // assuming that ntpTimeMillis is already converted to Unix time. If not, convert it to Unix time first
        val unixTimeMillis = ntpTimeMillis

        // Create a Date object from the Unix time
        val date = Date(unixTimeMillis)

        // Create a SimpleDateFormat instance and set the timezone to Hong Kong Time
        val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Hong_Kong")


        // Format the date as a string in the specified timezone
        return dateFormat.format(date)
    }


    companion object {
        private const val SERVER = "time.google.com"
    }

}