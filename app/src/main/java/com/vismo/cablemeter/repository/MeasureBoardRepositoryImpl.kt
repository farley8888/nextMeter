package com.vismo.cablemeter.repository

import android.content.Context
import android.util.Log
import android_serialport_api.Command
import com.google.firebase.Timestamp
import com.ilin.atelec.BusModel
import com.ilin.atelec.IAtCmd
import com.ilin.util.Config
import com.ilin.util.ShellUtils
import com.vismo.cablemeter.datastore.MCUParamsDataStore
import com.vismo.cablemeter.datastore.TripDataStore
import com.vismo.cablemeter.model.DeviceIdData
import com.vismo.cablemeter.model.MCUFareParams
import com.vismo.cablemeter.model.MCUMessage
import com.vismo.cablemeter.model.TripData
import com.vismo.cablemeter.model.TripStatus
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.util.GlobalUtils.divideBy100AndConvertToDouble
import com.vismo.cablemeter.util.GlobalUtils.multiplyBy10AndConvertToDouble
import com.vismo.cablemeter.util.MeasureBoardUtils
import com.vismo.cablemeter.util.MeasureBoardUtils.ABNORMAL_PULSE
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.logging.Logger
import javax.inject.Inject

@Suppress("detekt.TooManyFunctions")
class MeasureBoardRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val dashManagerConfig: DashManagerConfig,
    private val localTripsRepository: LocalTripsRepository,
) : MeasureBoardRepository {
    private var mBusModel: BusModel? = null
    private val taskChannel = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val messageChannel = Channel<MCUMessage>(Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private fun startMessageProcessor() {
        scope.launch {
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
        scope.launch {
            messageChannel.send(msg)
        }
    }

    private fun startTaskProcessor() {
        scope.launch {
            for (task in taskChannel) {
                task()
            }
        }
    }

    private fun addTask(task: suspend () -> Unit) {
        scope.launch {
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
            delay(200)
        }
    }

    private suspend fun handleIdleHeartbeatResult(result: String) {
        val measureBoardDeviceId = result.substring(52, 52 + 10)
        val licensePlateHex = result.substring(110, 110 + 16)
        val licensePlate = MeasureBoardUtils.convertToASCIICharacters(licensePlateHex) ?: ""

        MCUParamsDataStore.setDeviceIdData(DeviceIdData(measureBoardDeviceId, licensePlate))
        MCUParamsDataStore.setMCUTime(result.substring(40, 40 + 12))

        TripDataStore.tripData.value?.tripStatus?.let { tripStatus ->
            if (tripStatus == TripStatus.ENDED) {
                TripDataStore.clearTripData()
            }
        }
        dashManagerConfig.setLicensePlate(licensePlate)
        Log.d(TAG, "IDLE_HEARTBEAT: $result")
    }

    private suspend  fun handleOngoingHeartbearResult(result: String) {
        val measureBoardStatus = result.substring(17, 17 + 1)
        val isStopped = (measureBoardStatus.toInt() == 1)
        val tripStatus = if (isStopped) TripStatus.STOP else TripStatus.HIRED
        val lockedDuration = result.substring(18, 18 + 4)
        val lockedDurationDecimal = MeasureBoardUtils.hexToDecimal(lockedDuration)
        val distance = result.substring(22, 22 + 6).multiplyBy10AndConvertToDouble()
        val duration = result.substring(28, 28 + 6)
        val extras = result.substring(38, 38 + 6).divideBy100AndConvertToDouble()
        val fare = result.substring(44, 44 + 6).divideBy100AndConvertToDouble()
        val totalFare = result.substring(50, 50 + 6).divideBy100AndConvertToDouble()
        val currentTime = result.substring(56, 56 + 12)
        val measureBoardDeviceId = result.substring(68, 68 + 10)
        val licensePlateHex = result.substring(126, 126 + 16)
        val licensePlate = MeasureBoardUtils.convertToASCIICharacters(licensePlateHex) ?: ""

        MCUParamsDataStore.setDeviceIdData(DeviceIdData(measureBoardDeviceId, licensePlate))
        MCUParamsDataStore.setMCUTime(currentTime)


        val currentOngoingTripInDB = localTripsRepository.getLatestOnGoingTrip()

        currentOngoingTripInDB?.let {
            val requiresUpdate = it.fare != fare || it.tripStatus != tripStatus || it.extra != extras

            val currentOngoingTrip = TripData(
                tripId = it.tripId,
                startTime = it.startTime,
                tripStatus = tripStatus,
                pauseTime = if (tripStatus == TripStatus.STOP) Timestamp.now() else null,
                fare = fare,
                extra = extras,
                totalFare = totalFare,
                distanceInMeter = distance,
                waitDurationInSeconds = getTimeInSeconds(duration),
                overSpeedDurationInSeconds = lockedDurationDecimal,
                requiresUpdateOnDatabase = requiresUpdate,
                licensePlate = licensePlate,
            )
            TripDataStore.updateTripDataValue(currentOngoingTrip)
        }
        dashManagerConfig.setLicensePlate(licensePlate)
        Log.d(TAG, "ONGOING_HEARTBEAT: $result")
    }

    private suspend fun handleTripEndSummaryResult(result: String) {
        val distance = result.substring(118, 118 + 6).multiplyBy10AndConvertToDouble()
        val duration = result.substring(124, 124 + 6)
        val fare = result.substring(130, 130 + 6).divideBy100AndConvertToDouble()
        val extras = result.substring(136, 136 + 6).divideBy100AndConvertToDouble()
        val totalFare = result.substring(142, 142 + 6).divideBy100AndConvertToDouble()

        Log.d(TAG, "TRIP_END_SUMMARY: $distance, ${getTimeInSeconds(duration)}, $fare, $extras, $totalFare")

        val currentOngoingTripInDB = localTripsRepository.getLatestOnGoingTrip()

        currentOngoingTripInDB?.let {
            val currentOngoingTrip = TripData(
                tripId = it.tripId,
                startTime = it.startTime,
                tripStatus = TripStatus.ENDED,
                fare = fare,
                extra = extras,
                totalFare = totalFare,
                distanceInMeter = distance,
                waitDurationInSeconds = getTimeInSeconds(duration),
                pauseTime = it.pauseTime,
                endTime = Timestamp.now(),
                requiresUpdateOnDatabase = true,
                licensePlate = it.licensePlate,
            )
            TripDataStore.updateTripDataValue(currentOngoingTrip)
        }

        addTask {
            // after a trip ends, MCU will only continue sending IDLE heartbeats after it receives this response
            mBusModel?.write(Command.CMD_END_RESPONSE)
        }
    }

    private suspend fun checkStatues(result: String) {
        when (getResultType(result)) {
            IDLE_HEARTBEAT -> handleIdleHeartbeatResult(result = result)
            ONGOING_HEARTBEAT -> handleOngoingHeartbearResult(result = result)
            TRIP_END_SUMMARY -> handleTripEndSummaryResult(result = result)
            PARAMETERS_ENQUIRY -> handleParametersEnquiryResult(result = result)
            ABNORMAL_PULSE -> handleAbnormalPulse(result = result)
        }
    }

    private suspend fun handleAbnormalPulse(result: String) {
        TripDataStore.setAbnormalPulseTriggered(isAbnormalPulseTriggered = true)
        Log.d(TAG, "ABNORMAL_PULSE: $result")
    }

    private suspend fun handleParametersEnquiryResult(result: String) {
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

        val mcuData = MCUFareParams(
            parametersVersion = parametersVersion,
            firmwareVersion = firmwareVersion,
            kValue = kValue,
            startingDistance = startDistance,
            startingPrice = startPrice,
            stepPrice = stepPrice,
            changedPriceAt = stepPriceChangedAt,
            changedStepPrice = changedStepPrice,
        )
        MCUParamsDataStore.setMCUFareData(mcuData)
    }

    override fun enquireParameters() {
        addTask {
            mBusModel?.write(Command.CMD_PARAMETERS_ENQUIRY)
            delay(200)
        }
    }

    override fun updateKValue(kValue: Int) {
        addTask {
            mBusModel?.write(MeasureBoardUtils.getUpdateKValueCmd(kValue = kValue))
            delay(200)
        }
    }

    override fun updateLicensePlate(licensePlate: String) {
        addTask {
            mBusModel?.write(MeasureBoardUtils.getWritingDataIntoMeasureBoardCmd(licensePlate = licensePlate))
            delay(200)
        }
    }

    override fun updatePriceParams(
        startPrice: Int, stepPrice: Int, stepPrice2nd:Int, threshold:Int
    ) {
        addTask {
            mBusModel?.write(
                MeasureBoardUtils.getUpdatePriceParamCmd(
                    startPrice,
                    stepPrice,
                    stepPrice2nd,
                    threshold,
                )
            )
            delay(200)
        }
    }

    override fun emitBeepSound(
        duration: Int,
        interval: Int,
        repeatCount: Int,
    ) {
        addTask {
            mBusModel?.write(MeasureBoardUtils.getBeepSoundCmd(duration, interval, repeatCount))
            delay(200)
        }
    }

    override fun writeStartTripCommand(tripId: String) {
        addTask {
            mBusModel?.write(MeasureBoardUtils.getStartTripCmd(tripId = tripId))
            delay(200)
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
        }
    }

    override fun writeAddExtrasCommand(extrasAmount: Int) {
        addTask {
            mBusModel?.write(MeasureBoardUtils.getUpdateExtrasCmd("$extrasAmount"))
            delay(200)
        }
    }

    override fun unlockMeter() {
        addTask {
            mBusModel?.write(MeasureBoardUtils.getUnlockCmd()) ?: false
            delay(200)
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

    override fun stopCommunication() {
        mBusModel?.stopCommunicate()
        scope.cancel()
    }

    companion object {
        private const val WHAT_PRINT_STATUS: Int = 110
        private const val TAG = "MeasureBoardRepository"
    }
}
