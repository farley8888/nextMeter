package com.vismo.cablemeter.repository

import android.content.Context
import android.util.Log
import android_serialport_api.Command
import com.google.firebase.Timestamp
import com.ilin.atelec.BusModel
import com.ilin.atelec.IAtCmd
import com.ilin.util.Config
import com.ilin.util.ShellUtils
import com.serial.opt.UartWorkerCH
import com.serial.port.ByteUtils
import com.vismo.cablemeter.datastore.MCUParamsDataStore
import com.vismo.cablemeter.datastore.TripDataStore
import com.vismo.cablemeter.model.DeviceIdData
import com.vismo.cablemeter.model.MCUParams
import com.vismo.cablemeter.model.MCUMessage
import com.vismo.cablemeter.model.TripData
import com.vismo.cablemeter.model.TripStatus
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.util.GlobalUtils.divideBy100AndConvertToDouble
import com.vismo.cablemeter.util.GlobalUtils.multiplyBy10AndConvertToDouble
import com.vismo.cablemeter.util.MeasureBoardUtils
import com.vismo.cablemeter.util.MeasureBoardUtils.IDLE_HEARTBEAT
import com.vismo.cablemeter.util.MeasureBoardUtils.ONGOING_HEARTBEAT
import com.vismo.cablemeter.util.MeasureBoardUtils.PARAMETERS_ENQUIRY
import com.vismo.cablemeter.util.MeasureBoardUtils.TRIP_END_SUMMARY
import com.vismo.cablemeter.util.MeasureBoardUtils.getResultType
import com.vismo.cablemeter.util.MeasureBoardUtils.getTimeInSeconds
import com.vismo.nxgnfirebasemodule.DashManagerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Date
import java.util.logging.Logger
import javax.inject.Inject

@Suppress("detekt.TooManyFunctions")
class MeasureBoardRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val dashManagerConfig: DashManagerConfig
) : MeasureBoardRepository {
    private var mBusModel: BusModel? = null
    private var mWorkCh3: UartWorkerCH? = null

    private val taskChannel = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val messageChannel = Channel<MCUMessage>(Channel.UNLIMITED)

    private val _deviceIdData = MutableStateFlow<DeviceIdData?>(null)
    val deviceIdData = _deviceIdData

    private val _mcuTime = MutableStateFlow<String?>(null)
    val mcuTime = _mcuTime

    private fun startMessageProcessor() {
        CoroutineScope(ioDispatcher).launch {
            for (msg in messageChannel) {
                when (msg.what) {
                    IAtCmd.W_MSG_DISPLAY -> {
                        val receiveData = msg.obj?.toString() ?: continue
                        checkStatues(receiveData)
                    }
                    WHAT_PRINT_STATUS -> {
                        ShellUtils.execShellCmd("cat /sys/class/gpio/gpio73/value")
                        addTask {
                            delay(1800)
                            sendMessage(MCUMessage(WHAT_PRINT_STATUS, null))
                        }
                    }
                }
            }
        }
    }

    private fun sendMessage(msg: MCUMessage) {
        CoroutineScope(ioDispatcher).launch {
            messageChannel.send(msg)
        }
    }

    private fun startTaskProcessor() {
        CoroutineScope(ioDispatcher).launch {
            for (task in taskChannel) {
                task()
            }
        }
    }

    private fun addTask(task: suspend () -> Unit) {
        CoroutineScope(ioDispatcher).launch {
            taskChannel.send(task)
        }
    }

    init {
        startTaskProcessor()
        startMessageProcessor()
        addTask {
            openCommonUart()
            delay(200)
            setReceiveEvalDataLs()
            mBusModel?.startCommunicate()
        }
        initHardware()
        setSwitchLs(false)
    }

    private suspend fun checkStatues(result: String) {
        when (getResultType(result)) {
            IDLE_HEARTBEAT -> {
                val measureBoardDeviceId = result.substring(52, 52 + 10)
                val licensePlateHex = result.substring(110, 110 + 16)
                val licensePlate = MeasureBoardUtils.convertToASCIICharacters(licensePlateHex) ?: ""

                _deviceIdData.value = DeviceIdData(measureBoardDeviceId, licensePlate)
                _mcuTime.value = result.substring(40, 40 + 12)

                TripDataStore.tripData.value?.tripStatus?.let { tripStatus ->
                    if (tripStatus == TripStatus.ENDED) {
                        TripDataStore.clearTripData()
                    }
                }
                dashManagerConfig.setLicensePlate(licensePlate)
                Log.d(TAG, "IDLE_HEARTBEAT: $result")
            }

            ONGOING_HEARTBEAT -> {
                val measureBoardStatus = result.substring(16, 16 + 2)
                val isStopped = (measureBoardStatus.toInt() == 1)
                val tripStatus = if (isStopped) TripStatus.PAUSED else TripStatus.HIRED
                val lockedDuration = result.substring(18, 18 + 4)
                val overSpeedLockupDuration = MeasureBoardUtils.hexToDecimal(lockedDuration)
                val distance = result.substring(22, 22 + 6).multiplyBy10AndConvertToDouble()
                val duration = result.substring(28, 28 + 6)
                val extras = result.substring(38, 38 + 6).divideBy100AndConvertToDouble()
                val fare = result.substring(44, 44 + 6).divideBy100AndConvertToDouble()
                val totalFare = result.substring(50, 50 + 6).divideBy100AndConvertToDouble()
                val currentTime = result.substring(56, 56 + 12)
                val measureBoardDeviceId = result.substring(68, 68 + 10)
                val licensePlateHex = result.substring(126, 126 + 16)
                val licensePlate = MeasureBoardUtils.convertToASCIICharacters(licensePlateHex) ?: ""

                _deviceIdData.value = DeviceIdData(measureBoardDeviceId, licensePlate)
                _mcuTime.value = currentTime

                val currentTrip = TripDataStore.tripData.value
                val requiresUpdate = currentTrip?.fare != fare || currentTrip.tripStatus != tripStatus

                val currentOngoingTrip = TripData(
                    tripId = null,
                    startTime = null,
                    tripStatus = tripStatus,
                    isLocked = overSpeedLockupDuration > 0,
                    fare = fare,
                    extra = extras,
                    totalFare = totalFare,
                    distanceInMeter = distance,
                    waitDurationInSeconds = getTimeInSeconds(duration),
                    overSpeedDurationInSeconds = overSpeedLockupDuration,
                    requiresUpdateOnFirestore = requiresUpdate,
                )
                TripDataStore.updateTripDataValue(currentOngoingTrip)
                dashManagerConfig.setLicensePlate(licensePlate)
                Log.d(TAG, "ONGOING_HEARTBEAT: $result")
            }

            TRIP_END_SUMMARY -> {
                val distance = result.substring(118, 118 + 4).multiplyBy10AndConvertToDouble()
                val duration = result.substring(124, 124 + 6)
                val fare = result.substring(130, 130 + 6).divideBy100AndConvertToDouble()
                val extras = result.substring(136, 136 + 6).divideBy100AndConvertToDouble()
                val totalFare = result.substring(142, 142 + 6).divideBy100AndConvertToDouble()

                val currentOngoingTrip = TripData(
                    tripId = null,
                    startTime = null,
                    tripStatus = TripStatus.ENDED,
                    fare = fare,
                    extra = extras,
                    totalFare = totalFare,
                    distanceInMeter = distance,
                    waitDurationInSeconds = getTimeInSeconds(duration),
                    endTime = Timestamp.now(),
                    requiresUpdateOnFirestore = true,
                )
                TripDataStore.updateTripDataValue(currentOngoingTrip)

                addTask {
                    // after a trip ends, MCU will only continue sending IDLE heartbeats after it receives this response
                    mBusModel?.write(Command.CMD_END_RESPONSE)
                }
            }

            PARAMETERS_ENQUIRY -> {
                //parameters enquiry
                val firmwareVersion = result.substring(18, 18 + 8)
                val parametersVersion = result.substring(26, 26 + 8)
                val kValue = result.substring(34, 34 + 4)
                val startDistance = result.substring(38, 38 + 4)
                val startPrice = result.substring(42, 42 + 4) // 13
                val peakPrice = result.substring(46, 46 + 4)
                val stepPrice = result.substring(50, 50 + 4) // 17
                val peakStepPrice = result.substring(54, 54 + 4)
                val morningPeakStartTime = result.substring(58, 58 + 4)
                val morningPeakEndTime = result.substring(62, 62 + 4)
                val nightPeakStartTime = result.substring(66, 66 + 4)
                val nightPeakEndTime = result.substring(70, 70 + 4)
                val stepPriceChangedAt = result.substring(74, 74 + 4) // 29
                val changedStepPrice = result.substring(78, 78 + 4)  // 31
                val changedPeakStepPrice = result.substring(82, 82 + 4)
                val distanceInterval = result.substring(86, 86 + 4)
                val waitingTimeInterval = result.substring(90, 90 + 4)
                val overSpeed = result.substring(94, 94 + 4)

                val mcuData = MCUParams(
                    parametersVersion = parametersVersion,
                    firmwareVersion = firmwareVersion,
                    kValue = kValue,
                    startingDistance = startDistance,
                    startingPrice = startPrice,
                    stepPrice = stepPrice,
                    changedPriceAt = stepPriceChangedAt,
                    changedStepPrice = changedStepPrice,
                )
                MCUParamsDataStore.setMCUData(mcuData)
            }
        }
    }

    override fun updateKValue(kValue: Int) {
        addTask {
            mBusModel?.write(MeasureBoardUtils.getUpdateKValueCmd(kValue = kValue))
            delay(200)
        }
    }

    private fun initHardware() {
        ShellUtils.execEcho("echo 0 > /sys/class/gpio/gpio64/value")
        ShellUtils.execEcho("echo 1 > /sys/class/gpio/gpio65/value")
    }

    private fun emitBeepSound(
        duration: Int,
        interval: Int,
        repeatCount: Int,
    ) {
        mBusModel?.write(MeasureBoardUtils.getBeepSoundCmd(duration, interval, repeatCount))
    }

    override fun writeStartTripCommand(tripId: String) {
        addTask {
            mBusModel?.write(MeasureBoardUtils.getStartTripCmd(tripId = tripId))
            delay(200)
            setSwitchLs(true)
        }
    }

    override fun writeResumeTripCommand() {
        addTask {
            mBusModel?.write(MeasureBoardUtils.getContinueTripCmd())
            delay(200)
        }
    }

    override fun writeEndTripCommand() {
        addTask {
            mBusModel?.write(MeasureBoardUtils.getEndTripCmd())
            delay(200)
            setSwitchLs(false)
        }
    }

    override fun writePauseTripCommand() {
        addTask {
            mBusModel?.write(MeasureBoardUtils.getPauseTripCmd())
            delay(200)
        }
    }

    override fun writeStartAndPauseTripCommand(tripId: String) {
        addTask {
            mBusModel?.write(MeasureBoardUtils.getStartPauseTripCmd(tripId))
            delay(200)
            setSwitchLs(true)
        }
    }

    override fun writeAddExtrasCommand(extrasAmount: Int) {
        addTask {
            mBusModel?.write(MeasureBoardUtils.getUpdateExtrasCmd("$extrasAmount"))
            delay(200)
        }
    }

    private fun setSwitchLs(isChecked: Boolean) {
        Log.d("setSwitchLs", "setSwitchLs: isChecked: $isChecked")
        // isChecked: 落旗
        CoroutineScope(ioDispatcher).launch {
            try {
                if (isChecked) {
                    ShellUtils.echo(arrayOf("echo 0 >/sys/class/gpio/gpio117/value"))
                    delay(300)
                    ShellUtils.echo(arrayOf("echo 1 > /sys/class/gpio/gpio116/value"))
                } else {
                    ShellUtils.echo(arrayOf("echo 1 > /sys/class/gpio/gpio117/value"))
                    delay(300)
                    ShellUtils.echo(arrayOf("echo 0 > /sys/class/gpio/gpio116/value"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setReceiveEvalDataLs() {
        mBusModel?.setListener { data: String ->
            sendMessage(MCUMessage(IAtCmd.W_MSG_DISPLAY, data))
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
            Logger.getLogger(TAG).warning("openCommonUart: $e")
        }
    }

    override suspend fun writePrintReceiptCommand() {
        // TODO: Implement this method - take trip details from flow and send to printer
        if (mWorkCh3 == null) {
            openCH3()
            delay(2000)
            writePrintReceiptCommand()
        } else {
            try {
                ShellUtils.execEcho("echo 0 > /sys/class/gpio/gpio64/value")
                delay(200)
                ShellUtils.execEcho("echo 0 > /sys/class/gpio/gpio65/value")
                delay(200)

                val licensePlate = "CABLE03T"
                val startDateTime = Date()
                val endDateTime = Date()
//                val paidKm = String.format("%.2f", BigDecimal(distance).divide(BigDecimal("100")))
//                val paidMin = String.format("%.2f", getPaidMin(duration))
//                val surcharge = String.format("%.2f", BigDecimal(extras).divide(BigDecimal("100")))
//                val total = String.format("%.2f", BigDecimal(totalFare).divide(BigDecimal("100")))

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
//                        Config.getSPIDecimal(paidKm, 6),
//                        Config.getSPIDecimal(paidKm, 6),
//                        Config.getSPIDecimal(paidMin, 6),
//                        Config.getSPIDecimal(surcharge, 7),
//                        Config.getSPIDecimal(total, 7),
                    )

                data = data.replace(" ", "")
                val bytes = ByteUtils.hexStr2Byte(data)
                mWorkCh3?.writer?.writeData(bytes)
            } catch (e: Exception) {
                Logger.getLogger(TAG).warning("sendPrintCmd: $e")
            }
        }
    }

    private fun openCH3() {
        try {
            mWorkCh3 = UartWorkerCH(Config.SERIAL_CH3, Config.BATE_CH, 0, "CH3")
            mWorkCh3?.setOnReceiveListener(
                UartWorkerCH.OnReceiveListener { data: String ->
                    CoroutineScope(ioDispatcher).launch {
                        println("CH3.Opt receive = $data")
                    }
                },
            )
            mWorkCh3?.startCommunicate()
        } catch (e: IOException) {
            Logger.getLogger(TAG).warning("openCH3: $e")
        }
    }

    fun stopCommunication() {
        mBusModel?.stopCommunicate()
    }

    companion object {
        private const val WHAT_PRINT_STATUS: Int = 110
        private const val TAG = "MeasureBoardRepository"
    }
}
