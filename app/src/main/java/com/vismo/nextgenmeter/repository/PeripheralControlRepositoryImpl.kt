package com.vismo.nextgenmeter.repository

import android.util.Log
import com.ilin.util.Config
import com.ilin.util.ShellUtils
import com.serial.opt.UartWorkerCH
import com.serial.port.ByteUtils
import com.vismo.nextgenmeter.datastore.TripDataStore
import com.vismo.nextgenmeter.model.TripData
import com.vismo.nextgenmeter.model.TripStatus
import com.vismo.nextgenmeter.model.TripSummary
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.ui.meter.MeterOpsUtil.formatToNDecimalPlace
import com.vismo.nextgenmeter.ui.meter.MeterOpsUtil.getDistanceInKm
import com.vismo.nextgenmeter.util.MeasureBoardUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
            initHardware()
            ensureCH3Initialized()
            TripDataStore.ongoingTripData.collect { trip ->
                trip?.let {
                    if (trip.tripStatus == TripStatus.HIRED || trip.tripStatus == TripStatus.STOP) {
                        toggleForHireFlag(goDown = true)
                    } else {
                        toggleForHireFlag(goDown = false)
                    }
                }
            }
        }
    }

    private suspend fun initHardware() {
        // Set the GPIOs to the correct status for the printer
        ShellUtils.execEcho("echo 0 > /sys/class/gpio/gpio64/value")
        delay(200)
        ShellUtils.execEcho("echo 0 > /sys/class/gpio/gpio65/value")
        delay(200)

        //correct the flag status if needed
        val gpio117 = ShellUtils.execShellCmd("cat /sys/class/gpio/gpio117/value")
        val gpio116 = ShellUtils.execShellCmd("cat /sys/class/gpio/gpio116/value")
        Log.d(TAG, "initHardware gpio117 = $gpio117, io116 = $gpio116")

        toggleForHireFlag(goDown = false)
    }

    override fun toggleForHireFlag(goDown: Boolean) {
        scope.launch {
            val isCurrentlyDown = isFlagCurrentlyDown()
            try {
                if (goDown  && !isCurrentlyDown) {
                    ShellUtils.echo(arrayOf("echo 0 > /sys/class/gpio/gpio117/value"))
                    delay(300)
                    ShellUtils.echo(arrayOf("echo 1 > /sys/class/gpio/gpio116/value"))
                } else if (!goDown && isCurrentlyDown) {
                    ShellUtils.echo(arrayOf("echo 1 > /sys/class/gpio/gpio117/value"))
                    delay(300)
                    ShellUtils.echo(arrayOf("echo 0 > /sys/class/gpio/gpio116/value"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isFlagCurrentlyDown(): Boolean {
        val gpio117 = ShellUtils.execShellCmd("cat /sys/class/gpio/gpio117/value")
        val gpio116 = ShellUtils.execShellCmd("cat /sys/class/gpio/gpio116/value")
        val isCurrentlyDown = gpio117.trim() == "0" && gpio116.trim() == "1"
        return isCurrentlyDown
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

    override suspend fun printSummaryReceiptCommand(tripSummary: TripSummary) {
        try {
            var data = Config.getStatisticSPIData()
            data = String.format(
                data,
                MeasureBoardUtils.encodeHexString(tripSummary.licensePlate)
                    .replace(" ", "")
                    .padStart(16, 'F')
                    .replace("FF", "20"),
                Config.getSPIDateTime(tripSummary.firstStartTime.toDate()),
                Config.getSPIDateTime(tripSummary.lastEndTime.toDate()),
                Config.getSPIInt(tripSummary.allTripsCount.toString(), 3),
                Config.getSPIInt(tripSummary.cashTripsCount.toString(), 3),
                Config.getSPIInt(tripSummary.dashTripsCount.toString(), 3),
                Config.getSPIDecimal(formatToNDecimalPlace(tripSummary.allTripsDistanceInKm, 2), 6),
                Config.getSPIDecimal(
                    formatToNDecimalPlace(tripSummary.cashTripsDistanceInKm, 2),
                    6
                ),
                Config.getSPIDecimal(
                    formatToNDecimalPlace(tripSummary.dashTripsDistanceInKm, 2),
                    6
                ),
                Config.getSPIDecimal(
                    formatToNDecimalPlace(tripSummary.allTripsWaitTime / 60.0, 2),
                    7
                ),
                Config.getSPIDecimal(
                    formatToNDecimalPlace(tripSummary.cashTripsWaitTime / 60.0, 2),
                    7
                ),
                Config.getSPIDecimal(
                    formatToNDecimalPlace(tripSummary.dashTripsWaitTime / 60.0, 2),
                    7
                ),
                Config.getSPIDecimal(formatToNDecimalPlace(tripSummary.allTripsFare, 2), 8),
                Config.getSPIDecimal(formatToNDecimalPlace(tripSummary.cashTripsFare, 2), 7),
                Config.getSPIDecimal(formatToNDecimalPlace(tripSummary.dashTripsFare, 2), 7),
                Config.getSPIDecimal(formatToNDecimalPlace(tripSummary.allTripsExtras, 2), 8),
                Config.getSPIDecimal(formatToNDecimalPlace(tripSummary.cashTripsExtras, 2), 7),
                Config.getSPIDecimal(formatToNDecimalPlace(tripSummary.dashTripsExtras, 2), 7),
            )
            data = data.replace(" ", "")
            printData(data)
        } catch (e: Exception) {
            Logger.getLogger(TAG).warning("writePrintSummaryCommand: $e")
        }
    }

    override suspend fun printTripReceiptCommand(tripData: TripData) {
        try {
            val licensePlate = tripData.licensePlate
            val startDateTime = tripData.startTime.toDate()
            val pauseDateTime = tripData.pauseTime?.toDate() ?: Date()
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
                    Config.getSPIDateTime(pauseDateTime),
                    Config.getSPIDecimal(paidKm, 6),
                    Config.getSPIDecimal(paidKm, 6),
                    Config.getSPIDecimal(paidMin, 6),
                    Config.getSPIDecimal(surcharge, 7),
                    Config.getSPIDecimal(total, 7),
                )

            data = data.replace(" ", "")
            printData(data)
        } catch (e: Exception) {
            Logger.getLogger(TAG).warning("writePrintReceiptCommand: $e")
        }
    }

    private suspend fun printData(data: String) {
        measureBoardRepository.emitBeepSound(10, 0, 1)
        ensureCH3Initialized()
        if (mWorkCh3 == null) {
            Log.w(TAG, "mWorkCh3 is still null after initialization")
            return
        } else {
            try {
                val bytes = ByteUtils.hexStr2Byte(data)
                mWorkCh3?.writer?.writeData(bytes)
            } catch (e: Exception) {
                Logger.getLogger(TAG).warning("printData: $e")
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