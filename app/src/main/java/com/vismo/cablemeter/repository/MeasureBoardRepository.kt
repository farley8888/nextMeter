package com.vismo.cablemeter.repository

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android_serialport_api.Command
import com.ilin.atelec.BusModel
import com.ilin.atelec.IAtCmd
import com.ilin.util.Config
import com.ilin.util.ShellUtils
import com.serial.opt.UartWorkerCH
import com.serial.port.ByteUtils
import com.vismo.cablemeter.util.MeasureBoardUtils
import kotlinx.coroutines.*
import java.io.IOException
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject

class MeasureBoardRepository @Inject constructor(
    private val context: Context
) {

    private val TAG = javaClass.simpleName
    private var mBusModel: BusModel? = null
    private var mWorkCh3: UartWorkerCH? = null
    private val handler = Handler(Looper.getMainLooper())

    init {
        initHardwares()
        openCommonUart()
        setReceiveEvalDataLs()
    }

    private fun initHardwares() {
        ShellUtils.execEcho("echo 0 > /sys/class/gpio/gpio64/value")
        ShellUtils.execEcho("echo 1 > /sys/class/gpio/gpio65/value")
    }

    private fun setReceiveEvalDataLs() {
        mBusModel?.setListener { data: String ->
            handler.sendMessage(handler.obtainMessage(IAtCmd.W_MSG_DISPLAY, data))
            Log.d("setReceiveEvalDataLs", "setReceiveEvalDataLs $data")
        }
    }

    private fun openCommonUart() {
        try {
            if (mBusModel == null) {
                mBusModel = BusModel.getInstance(context)
            }
            mBusModel?.init(Config.SERIAL_CH1, Config.BATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun updateKValue(kValue: Int) {
        mBusModel?.write(MeasureBoardUtils.getUpdateKValueCmd(kValue))
    }

    fun updateMeasureBoardTime(formattedDateStr: String) {
        mBusModel?.write(MeasureBoardUtils.getUpdateTimeCmd(formattedDateStr))
    }

    fun readMeasureBoardDeviceId(): Boolean {
        return mBusModel?.write(MeasureBoardUtils.getMeasureBoardDeviceIdCmd()) ?: false
    }

    fun writeMeasureBoardData(licensePlate: String): Boolean {
        return mBusModel?.write(MeasureBoardUtils.getWritingDataIntoMeasureBoardCmd(licensePlate)) ?: false
    }

    fun unlockMeter(): Boolean {
        return mBusModel?.write(MeasureBoardUtils.getUnlockCmd()) ?: false
    }

    fun restoreMeter() {
        mBusModel?.write(Command.CMD_INIT)
    }

    fun emitBeepSound(duration: Int, interval: Int, repeatCount: Int) {
        mBusModel?.write(MeasureBoardUtils.getBeepSoundCmd(duration, interval, repeatCount))
    }

    suspend fun sendPrintCmd(fare: String, extras: String, duration: String, distance: String, totalFare: String) {
        if (mWorkCh3 == null) {
            withContext(Dispatchers.Main) {
//                CustomSnackbar(context).show("未打开Ch3,請再試", duration = Snackbar.LENGTH_SHORT)
            }
            openCH3()
            delay(2000)
            sendPrintCmd(fare, extras, duration, distance, totalFare)
        } else {
            try {
                ShellUtils.execEcho("echo 0 > /sys/class/gpio/gpio64/value")
                delay(200)
                ShellUtils.execEcho("echo 0 > /sys/class/gpio/gpio65/value")
                delay(200)

                val licensePlate = "CABLE03T"
                val startDateTime = Date()
                val endDateTime = Date()
                val paidKm = String.format("%.2f", BigDecimal(distance).divide(BigDecimal("100")))
                val paidMin = String.format("%.2f", getPaidMin(duration))
                val surcharge = String.format("%.2f", BigDecimal(extras).divide(BigDecimal("100")))
                val total = String.format("%.2f", BigDecimal(totalFare).divide(BigDecimal("100")))

                var data = Config.getSPIData()
                data = String.format(
                    data,
                    MeasureBoardUtils.encodeHexString(licensePlate).replace(" ", "").padStart(16, 'F').replace("FF", "20"),
                    Config.getSPIDateTime(startDateTime),
                    Config.getSPIDateTime(endDateTime),
                    Config.getSPIDecimal(paidKm, 6),
                    Config.getSPIDecimal(paidKm, 6),
                    Config.getSPIDecimal(paidMin, 6),
                    Config.getSPIDecimal(surcharge, 7),
                    Config.getSPIDecimal(total, 7)
                )

                data = data.replace(" ", "")
                val bytes = ByteUtils.hexStr2Byte(data)
                mWorkCh3?.getWriter()?.writeData(bytes)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("sendPrintCmd", "sendPrintCmd: exception: ${e.toString()}")
            }
        }
    }

    private fun getPaidMin(duration: String): BigDecimal {
        return if (duration.length == 6) {
            val hour = duration.substring(0, 2)
            val min = duration.substring(2, 4)
            val sec = duration.substring(4, 6)
            BigDecimal(hour.toInt() * 60 + min.toInt() + sec.toDouble() / 60)
        } else {
            BigDecimal(0)
        }
    }

    private fun openCH3() {
        try {
            mWorkCh3 = UartWorkerCH(Config.SERIAL_CH3, Config.BATE_CH, 0, "CH3")
            mWorkCh3?.setOnReceiveListener(UartWorkerCH.OnReceiveListener { data: String ->
                handler.post {
                    println("CH3.Opt receive = $data")
                }
            })
            mWorkCh3?.startCommunicate()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun enquiryParameters() {
        mBusModel?.write(Command.CMD_PARAMETERS_ENQUIRY)
    }

    fun requestPatchFirmware(fileName: String) {
//        CoroutineScope(Dispatchers.IO).launch {
//            SystemPreferencesRepository.getSystemPreferences()
//                .catch { e -> Log.e(TAG, "requestPatchFirmware Error loading system preferences:", e) }
//                .collect { systemPreferencesData ->
//                    val batchSize = MeasureBoardUtils.getFirmwareBatchSize(fileName)
//                    val version = fileName.split("/").lastOrNull()?.substringBefore(".") ?: ""
//                    val ok = mBusModel?.write(MeasureBoardUtils.getRequestPatchMeterBoardFirmwareCmd(fileName, version)!!.toHexString().replace(" ", ""))
//                }
//        }
    }

    fun patchFirmware(fileName: String, version: String, offset: Int) {
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val firmwareBytes = MeasureBoardUtils.getPatchMeterBoardFirmwareCmd(fileName, version, offset)
            mBusModel?.sendCmd(firmwareBytes)
        }
    }

    private fun String.format(pattern: String): String {
        return String.format(pattern, this)
    }

    private fun ByteArray.toHexString(): String {
        val sb = StringBuilder()
        for (byte in this) {
            sb.append(String.format("%02X", byte))
            sb.append(" ")
        }
        return sb.toString()
    }
}
