package com.vismo.nextgenmeter.repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import android_serialport_api.Command
import com.google.firebase.Timestamp
import com.ilin.atelec.BusModel
import com.ilin.atelec.IAtCmd
import com.ilin.util.Config
import com.ilin.util.ShellUtils
import com.vismo.nextgenmeter.datastore.DeviceDataStore
import com.vismo.nextgenmeter.datastore.TripDataStore
import com.vismo.nextgenmeter.model.DeviceIdData
import com.vismo.nextgenmeter.model.MCUFareParams
import com.vismo.nextgenmeter.model.MCUMessage
import com.vismo.nextgenmeter.model.OngoingMCUHeartbeatData
import com.vismo.nextgenmeter.model.TripData
import com.vismo.nextgenmeter.model.TripStatus
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.module.MainDispatcher
import com.vismo.nextgenmeter.util.GlobalUtils.divideBy100AndConvertToDouble
import com.vismo.nextgenmeter.util.GlobalUtils.extractSubstring
import com.vismo.nextgenmeter.util.GlobalUtils.multiplyBy10AndConvertToDouble
import com.vismo.nextgenmeter.util.MeasureBoardUtils
import com.vismo.nextgenmeter.util.MeasureBoardUtils.ABNORMAL_PULSE
import com.vismo.nextgenmeter.util.MeasureBoardUtils.IDLE_HEARTBEAT
import com.vismo.nextgenmeter.util.MeasureBoardUtils.ONGOING_HEARTBEAT
import com.vismo.nextgenmeter.util.MeasureBoardUtils.PARAMETERS_ENQUIRY
import com.vismo.nextgenmeter.util.MeasureBoardUtils.TRIP_END_SUMMARY
import com.vismo.nextgenmeter.util.MeasureBoardUtils.getResultType
import com.vismo.nextgenmeter.util.MeasureBoardUtils.getTimeInSeconds
import com.vismo.nxgnfirebasemodule.DashManagerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.IScope
import io.sentry.Sentry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.logging.Logger
import javax.inject.Inject

@Suppress("detekt.TooManyFunctions")
class MeasureBoardRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    private val dashManagerConfig: DashManagerConfig,
    private val localTripsRepository: LocalTripsRepository,
    private val meterPreferenceRepository: MeterPreferenceRepository,
) : MeasureBoardRepository {
    private var mBusModel: BusModel? = null
    private val taskChannel = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val messageChannel = Channel<MCUMessage>(Channel.UNLIMITED)

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Scope exception", throwable)
        Sentry.captureException(throwable)
    }
    private var scope: CoroutineScope? = null

    override val meterIdentifierInRemote: StateFlow<String> = dashManagerConfig.meterIdentifier

    private fun startMessageProcessor() {
        scope?.launch {
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
        scope?.launch {
            messageChannel.send(msg)
        }
    }

    private fun startTaskProcessor() {
        scope?.launch {
            for (task in taskChannel) {
                task()
            }
        }
    }

    private fun addTask(task: suspend () -> Unit) {
        scope?.launch {
            taskChannel.send(task)
        }
    }

    override fun init() {
        scope = CoroutineScope(SupervisorJob() + ioDispatcher + exceptionHandler)
        startTaskProcessor()
        startMessageProcessor()
        addTask {
            openCommonUart()
            delay(200)
            setReceiveEvalDataLs()
            mBusModel?.startCommunicate()
            if (mBusModel == null) {
                Log.e(TAG, "init: mBusModel is null")
                Sentry.captureMessage("init: mBusModel is null")
            }
            delay(200)
        }
        Log.d(TAG, "MeasureBoardRepositoryImpl: init")
    }

    private suspend fun handleIdleHeartbeatResult(result: String) {
        val measureBoardDeviceId = result.substring(52, 52 + 10)
        val licensePlateHex = result.substring(110, 110 + 16)
        val licensePlate = MeasureBoardUtils.convertToASCIICharacters(licensePlateHex) ?: ""

        DeviceDataStore.setDeviceIdData(DeviceIdData(measureBoardDeviceId, licensePlate))
        DeviceDataStore.setMCUTime(result.substring(40, 40 + 12))

        TripDataStore.ongoingTripData.value?.let { _ ->
            TripDataStore.clearTripData()
        }
        dashManagerConfig.setDeviceIdData(deviceId = measureBoardDeviceId, licensePlate =  licensePlate)
        meterPreferenceRepository.saveDeviceId(measureBoardDeviceId)
        meterPreferenceRepository.saveLicensePlate(licensePlate)
        Sentry.configureScope { scope: IScope ->
            scope.setTag("license_plate", licensePlate)
        }
        Log.d(TAG, "IDLE_HEARTBEAT: $result")
    }

    private suspend  fun handleOngoingHeartbearResult(result: String) {
        Log.d(TAG, "ONGOING_HEARTBEAT: $result")
        // Parse the heartbeat result into a data class for better organization
        val heartbeatData = parseHeartbeatResult(result)

        // Update the MCU time
        DeviceDataStore.setMCUTime(heartbeatData.currentTime)

        // Retrieve the current ongoing local trip
        val currentOngoingLocalTrip = localTripsRepository.getLatestOnGoingTrip()

        // Retrieve saved device ID and license plate
        val savedDeviceId = meterPreferenceRepository.getDeviceId().firstOrNull() ?: ""
        val savedLicensePlate = meterPreferenceRepository.getLicensePlate().firstOrNull() ?: ""

        // Use the device ID and license plate from the current trip or saved preferences
        val deviceId = currentOngoingLocalTrip?.deviceId ?: savedDeviceId
        val licensePlate = currentOngoingLocalTrip?.licensePlate ?: savedLicensePlate

        // Update device ID data
        dashManagerConfig.setDeviceIdData(deviceId = deviceId, licensePlate = licensePlate)
        DeviceDataStore.setDeviceIdData(DeviceIdData(deviceId, licensePlate))

        if (currentOngoingLocalTrip == null) {
            // edge case: happens on some devices when device is reset while trip is ongoing
            withContext(mainDispatcher) {
                Toast.makeText(context, "Ongoing local trip not found. Creating a new trip.", Toast.LENGTH_SHORT).show()
            }

            val newTrip = TripData(
                tripId = MeasureBoardUtils.generateTripId(),
                startTime = Timestamp.now(),
                tripStatus = heartbeatData.tripStatus,
                pauseTime = if (heartbeatData.tripStatus == TripStatus.STOP) Timestamp.now() else null,
                fare = heartbeatData.fare,
                extra = heartbeatData.extras,
                totalFare = heartbeatData.totalFare,
                distanceInMeter = heartbeatData.distance,
                waitDurationInSeconds = getTimeInSeconds(heartbeatData.duration),
                overSpeedDurationInSeconds = heartbeatData.lockedDurationDecimal,
                requiresUpdateOnDatabase = true,
                licensePlate = licensePlate,
                deviceId = deviceId,
                overSpeedCounter = heartbeatData.overspeedCounterDecimal,
                abnormalPulseCounter = heartbeatData.abnormalPulseCounterDecimal,
                mcuStatus = heartbeatData.mcuStatus
            )

            TripDataStore.setFallbackTripDataToStartNewTrip(newTrip)
            Log.d(TAG, "handleOngoingHeartbeatResult: currentOngoingLocalTrip is null")
            Sentry.captureMessage("handleOngoingHeartbeatResult: currentOngoingLocalTrip is null")
        }
        else {
            val requiresUpdate = currentOngoingLocalTrip.fare != heartbeatData.fare ||
                    currentOngoingLocalTrip.tripStatus != heartbeatData.tripStatus ||
                    currentOngoingLocalTrip.extra != heartbeatData.extras ||
                    heartbeatData.lockedDurationDecimal > 0

            val overSpeedDurationInSeconds = if (currentOngoingLocalTrip.overSpeedDurationInSeconds > heartbeatData.lockedDurationDecimal) {
                currentOngoingLocalTrip.overSpeedDurationInSeconds + heartbeatData.lockedDurationDecimal
            } else {
                heartbeatData.lockedDurationDecimal
            }

            val updatedTrip = currentOngoingLocalTrip.copy(
                tripStatus = heartbeatData.tripStatus,
                pauseTime = getPauseTime(tripStatus = heartbeatData.tripStatus, currentPauseTime = currentOngoingLocalTrip.pauseTime),
                fare = heartbeatData.fare,
                extra = heartbeatData.extras,
                totalFare = heartbeatData.totalFare,
                distanceInMeter = heartbeatData.distance,
                waitDurationInSeconds = getTimeInSeconds(heartbeatData.duration),
                overSpeedDurationInSeconds = overSpeedDurationInSeconds,
                requiresUpdateOnDatabase = requiresUpdate,
                overSpeedCounter = heartbeatData.overspeedCounterDecimal,
                abnormalPulseCounter = heartbeatData.abnormalPulseCounterDecimal,
                mcuStatus = heartbeatData.mcuStatus
            )

            TripDataStore.updateTripDataValue(updatedTrip)

            if(currentOngoingLocalTrip.fare != heartbeatData.fare && currentOngoingLocalTrip.fare != 0.0) {
                emitBeepSound(5, 0, 1)
            }
        }

    }

    private fun getPauseTime(tripStatus: TripStatus, currentPauseTime: Timestamp?): Timestamp? {
        return if (tripStatus == TripStatus.STOP) {
            if(currentPauseTime == null) {
                Timestamp.now()
            } else {
                currentPauseTime
            }
        } else {
            null
        }
    }

    private fun parseHeartbeatResult(result: String): OngoingMCUHeartbeatData {
        val heartbeatData = OngoingMCUHeartbeatData(
            measureBoardStatus = result.extractSubstring(17, 1).toIntOrNull() ?: 1,
            lockedDurationHex = result.extractSubstring(18, 4),
            distanceHex = result.extractSubstring(22, 6),
            duration = result.extractSubstring(28, 6),
            extrasHex = result.extractSubstring(38, 6),
            fareHex = result.extractSubstring(44, 6),
            totalFareHex = result.extractSubstring(50, 6),
            currentTime = result.extractSubstring(56, 12),
            abnormalPulseCounterHex = result.extractSubstring(68, 2),
            overspeedCounterHex = result.extractSubstring(70, 2)
        )

        return heartbeatData.processHexValues()
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
        if(currentOngoingTripInDB == null) {
            Log.d(TAG, "handleTripEndSummaryResult: currentOngoingTripInDB is null")
            Sentry.captureMessage("handleTripEndSummaryResult: currentOngoingTripInDB is null")
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
            else -> {
                Log.d(TAG, "Unknown result type: ${getResultType(result)}")
                Sentry.captureMessage("Unknown result: $result")
            }
        }
        Log.d(TAG, "checkStatues: $result")
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
        DeviceDataStore.setMCUFareData(mcuData)
        Log.d(TAG, "handleParametersEnquiryResult: ${mcuData.kValue} ${mcuData.startingPrice} ${mcuData.stepPrice} ${mcuData.changedStepPrice}")
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

    override fun updateMeasureBoardTime(formattedDateStr: String) {
        addTask {
            mBusModel?.write(MeasureBoardUtils.getUpdateTimeCmd(formattedDateStr = formattedDateStr))
            delay(200)
        }
    }

    private fun setReceiveEvalDataLs() {
        mBusModel?.setListener { data: String ->
            sendMessage(MCUMessage(IAtCmd.W_MSG_DISPLAY, data))
            Log.d(TAG, "setReceiveEvalDataLs $data")
        }
        if(mBusModel == null) {
           Log.e(TAG, "setReceiveEvalDataLs: mBusModel is null")
            Sentry.captureMessage("setReceiveEvalDataLs: mBusModel is null")
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
        scope?.cancel()
    }

    companion object {
        private const val WHAT_PRINT_STATUS: Int = 110
        private const val TAG = "MeasureBoardRepository"
    }
}
