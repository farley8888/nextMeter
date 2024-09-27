package com.vismo.cablemeter.repository

import android.util.Log
import com.ilin.util.Config
import com.ilin.util.ShellUtils
import com.serial.opt.UartWorkerCH
import com.serial.port.ByteUtils
import com.vismo.cablemeter.model.TripData
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.ui.meter.MeterOpsUtil.formatToNDecimalPlace
import com.vismo.cablemeter.ui.meter.MeterOpsUtil.getDistanceInKm
import com.vismo.cablemeter.util.MeasureBoardUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Date
import java.util.logging.Logger

class PeripheralControlRepositoryImpl(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val measureBoardRepository: MeasureBoardRepository
) : PeripheralControlRepository{
    private var mWorkCh3: UartWorkerCH? = null
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    init {
        scope.launch {
            ensureCH3Initialized()
        }
    }

    private suspend fun ensureCH3Initialized() {
        if (mWorkCh3 == null) {
            openCH3()
            delay(2000) // Consider optimizing this delay
        }
    }

    private fun openCH3() {
        try {
            mWorkCh3 = UartWorkerCH(Config.SERIAL_CH3, Config.BATE_CH, 0, "CH3")
            mWorkCh3?.setOnReceiveListener { data: String ->
                scope.launch {
                    println("CH3.Opt receive = $data")
                }
            }
            mWorkCh3?.startCommunicate()
        } catch (e: IOException) {
            Logger.getLogger(TAG).warning("openCH3: $e")
        }
    }

    override suspend fun writePrintReceiptCommand(tripData: TripData) {
        measureBoardRepository.emitBeepSound(10, 0, 1)
        ensureCH3Initialized()
        if (mWorkCh3 == null) {
            Log.w(TAG, "mWorkCh3 is still null after initialization")
            return
        } else {
            try {
                withContext(ioDispatcher) {
                    ShellUtils.execEcho("echo 0 > /sys/class/gpio/gpio64/value")
                    delay(200)
                    ShellUtils.execEcho("echo 0 > /sys/class/gpio/gpio65/value")
                    delay(200)
                }

                val licensePlate = tripData.licensePlate
                val startDateTime = tripData.startTime.toDate()
                val endDateTime = tripData.endTime?.toDate() ?: Date()
                val paidKm = getDistanceInKm(tripData.distanceInMeter)
                val waitTimeInMinutes = tripData.waitDurationInSeconds / 60.0
                val paidMin = formatToNDecimalPlace(waitTimeInMinutes, 2)
                val surcharge = formatToNDecimalPlace(tripData.extra, 2)
                val total = formatToNDecimalPlace(tripData.totalFare, 2)

                var data = Config.getSPIData()
                data =
                    String.format(
                        data,
                        MeasureBoardUtils
                            .encodeHexString(licensePlate)
                            .replace(" ", "")
                            .padStart(16, 'F')
                            .replace("FF", "20"),
                        Config.getSPIDateTime(startDateTime),
                        Config.getSPIDateTime(endDateTime),
                        Config.getSPIDecimal(paidKm, 6),
                        Config.getSPIDecimal(paidKm, 6),
                        Config.getSPIDecimal(paidMin, 6),
                        Config.getSPIDecimal(surcharge, 7),
                        Config.getSPIDecimal(total, 7),
                    )

                data = data.replace(" ", "")
                val bytes = ByteUtils.hexStr2Byte(data)
                mWorkCh3?.writer?.writeData(bytes)
            } catch (e: Exception) {
                Logger.getLogger(TAG).warning("writePrintReceiptCommand: $e")
            }
        }
    }

    override fun close() {
        mWorkCh3?.stopCommunicate()
        scope.cancel()
    }

    companion object {
        const val TAG = "PeripheralControlRepositoryImpl"
    }
}