package com.vismo.nextgenmeter.repository

import android.content.Context
import android.util.Log
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
import com.vismo.nextgenmeter.util.GlobalUtils.divideBy100AndConvertToDouble
import com.vismo.nextgenmeter.util.GlobalUtils.extractSubstring
import com.vismo.nextgenmeter.util.GlobalUtils.multiplyBy10AndConvertToDouble
import com.vismo.nextgenmeter.util.MeasureBoardUtils
import com.vismo.nextgenmeter.util.MeasureBoardUtils.ABNORMAL_PULSE
import com.vismo.nextgenmeter.util.MeasureBoardUtils.IDLE_HEARTBEAT
import com.vismo.nextgenmeter.util.MeasureBoardUtils.ONGOING_HEARTBEAT
import com.vismo.nextgenmeter.util.MeasureBoardUtils.PARAMETERS_ENQUIRY
import com.vismo.nextgenmeter.util.MeasureBoardUtils.REQUEST_UPGRADE_FIRMWARE
import com.vismo.nextgenmeter.util.MeasureBoardUtils.TRIP_END_SUMMARY
import com.vismo.nextgenmeter.util.MeasureBoardUtils.UPGRADING_FIRMWARE
import com.vismo.nextgenmeter.util.MeasureBoardUtils.getResultType
import com.vismo.nextgenmeter.util.MeasureBoardUtils.getTimeInSeconds
import com.vismo.nextgenmeter.util.MeasureBoardUtils.toHexString
import com.vismo.nxgnfirebasemodule.DashManagerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.IScope
import io.sentry.Sentry
import io.sentry.SentryLevel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.logging.Logger
import javax.inject.Inject

@Suppress("detekt.TooManyFunctions")
class MeasureBoardRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val dashManagerConfig: DashManagerConfig,
    private val meterPreferenceRepository: MeterPreferenceRepository,
) : MeasureBoardRepository {
    private var mBusModel: BusModel? = null
    private var taskChannel = Channel<suspend () -> Unit>(capacity = 100, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var messageChannel = Channel<MCUMessage>(capacity = 100, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Scope exception", throwable)
        Sentry.addBreadcrumb("MeasureBoardRepositoryImpl Scope exception", "MeasureBoardRepositoryImpl Scope exception")
        Sentry.captureException(throwable) { scope ->
            scope.level = SentryLevel.ERROR
        }
    }
    private var externalScope: CoroutineScope? = null

    override val meterIdentifierInRemote: StateFlow<String> = dashManagerConfig.meterIdentifier

    private fun startMessageProcessor() {
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            for (msg in messageChannel) {
                when (msg.what) {
                    IAtCmd.W_MSG_DISPLAY -> {
                        Log.d(TAG, "startMessageProcessor: ${msg.obj}")
                        val receiveData = msg.obj?.toString() ?: continue
                        // Check checksum and see if the message is valid
                        if(!validateChecksum(receiveData)){
                            Log.d(TAG, TAG_CHECKSUM_VALIDATION_FAILED)
                            Sentry.captureMessage(TAG_CHECKSUM_VALIDATION_FAILED)
                            continue
                        }
                        checkStatues(receiveData)
                    }
                    WHAT_PRINT_STATUS -> {
                        ShellUtils.execShellCmd("cat /sys/class/gpio/gpio73/value")
                        addTask {
                            delay(1800)
                            sendMessage(MCUMessage(WHAT_PRINT_STATUS, null))
                            Log.d(TAG, "startMessageProcessor: WHAT_PRINT_STATUS")
                        }
                    }
                }
            }
        }
    }

    private fun sendMessage(msg: MCUMessage) {
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            if (messageChannel.isClosedForSend || messageChannel.isClosedForReceive) {
                messageChannel = Channel<MCUMessage>(capacity = 100, onBufferOverflow = BufferOverflow.DROP_OLDEST)
                startMessageProcessor() // Restart the message processor
                Log.d(TAG, "sendMessage: messageChannel is closed")
            }

            messageChannel.send(msg)
            Log.d(TAG, "sendMessage: $msg")
        }
    }

    private fun startTaskProcessor() {
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            for (task in taskChannel) {
                task()
                Log.d(TAG, "startTaskProcessor: $task")
            }
        }
    }

    private fun addTask(task: suspend () -> Unit) {
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            if (taskChannel.isClosedForSend || taskChannel.isClosedForReceive) {
                taskChannel = Channel<suspend () -> Unit>(capacity = 100, onBufferOverflow = BufferOverflow.DROP_OLDEST)
                startTaskProcessor() // Restart the task processor
                Log.d(TAG, "addTask: taskChannel is closed")
            }
            taskChannel.send(task)
            Log.d(TAG, "addTask: $task")
        }
    }

    override fun startCommunicate() {
        mBusModel?.startCommunicate()
        Log.d(TAG, "startCommunicate")
    }

    override fun init(scope: CoroutineScope) {
        externalScope = scope
        startTaskProcessor()
        startMessageProcessor()
        addTask {
            openCommonUart()
            delay(200)
            setReceiveEvalDataLs()
            startCommunicate()
            if (mBusModel == null) {
                Log.e(TAG, "init: mBusModel is null")
                Sentry.captureMessage("init: mBusModel is null")
            }
            delay(200)
        }
        Log.d(TAG, "MeasureBoardRepositoryImpl: init")
    }

    private suspend fun handleIdleHeartbeatResult(result: String) {
        val timeHex = result.substring(40, 40 + 12)
        fun isHBValid(): Boolean {
            if (!MeasureBoardUtils.isValidDate(timeHex, "yyyyMMddHHmm")) return false
            return true
        }
        if (result.length < 125 || !result.startsWith(HEARTBEAT_IDENTIFIER) || !isHBValid()) {
            Log.d(TAG, "handleIdleHeartbeatResult: Invalid result length: ${result.length}")
            Sentry.captureMessage("handleIdleHeartbeatResult: Invalid result length: ${result.length}")
            return
        }
        DeviceDataStore.setMCUHeartbeatActive(true)
        val measureBoardDeviceId = result.substring(52, 52 + 10)
        val licensePlateHex = result.substring(110, 110 + 16)
        val licensePlate = MeasureBoardUtils.convertToASCIICharacters(licensePlateHex) ?: ""

        DeviceDataStore.setDeviceIdData(DeviceIdData(measureBoardDeviceId, licensePlate))
        DeviceDataStore.setMCUTime(timeHex)

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
        if (result.length < 75 || !result.startsWith(HEARTBEAT_IDENTIFIER)) {
            Log.d(TAG, "parseHeartbeatResult: Invalid result length: ${result.length}")
            Sentry.captureMessage("parseHeartbeatResult: Invalid result length: ${result.length}")
            return
        }
        // Parse the heartbeat result into a data class for better organization
        val heartbeatData = parseHeartbeatResult(result)
        if (heartbeatData == null) {
            Log.d(TAG, "parseHeartbeatResult: Invalid content: ${result}")
            Sentry.captureMessage("parseHeartbeatResult: Invalid content: ${result}")
            return
        }

        DeviceDataStore.setMCUHeartbeatActive(true)
        // Update the MCU time
        DeviceDataStore.setMCUTime(heartbeatData.currentTime)

        // Retrieve saved device ID and license plate
        val savedDeviceId = meterPreferenceRepository.getDeviceId().firstOrNull() ?: ""
        val savedLicensePlate = meterPreferenceRepository.getLicensePlate().firstOrNull() ?: ""

        // Update device ID data
        dashManagerConfig.setDeviceIdData(deviceId = savedDeviceId, licensePlate = savedLicensePlate)
        DeviceDataStore.setDeviceIdData(DeviceIdData(savedLicensePlate, savedLicensePlate))

        val ongoingTrip = TripDataStore.ongoingTripData.firstOrNull()

        val requiresUpdate = ongoingTrip?.fare != heartbeatData.fare ||
                ongoingTrip.tripStatus != heartbeatData.tripStatus ||
                ongoingTrip.extra != heartbeatData.extras

        val ongoingTripOverSpeedDurationInSeconds = ongoingTrip?.overSpeedDurationInSeconds ?: 0
        val overSpeedDurationInSeconds = if (ongoingTripOverSpeedDurationInSeconds > heartbeatData.lockedDurationDecimal
        ) {
            ongoingTripOverSpeedDurationInSeconds  + heartbeatData.lockedDurationDecimal
        } else {
            heartbeatData.lockedDurationDecimal
        }
        val savedOngoingTripId = meterPreferenceRepository.getOngoingTripId().firstOrNull() ?: ""

        val newTrip = if (ongoingTrip == null) {
            val savedOngoingStartTime = meterPreferenceRepository.getOngoingTripStartTime().firstOrNull()?.run {
                if (this != 0L) {
                    Timestamp(this, 0)
                } else null
            }
            // start Trip
            TripData(
                tripId = savedOngoingTripId,
                startTime = savedOngoingStartTime ?: Timestamp.now(),
                tripStatus = heartbeatData.tripStatus,
                fare = heartbeatData.fare,
                extra = heartbeatData.extras,
                totalFare = heartbeatData.totalFare,
                paidDistanceInMeters = heartbeatData.paidDistance,
                unpaidDistanceInMeters = heartbeatData.unpaidDistance,
                waitDurationInSeconds = getTimeInSeconds(heartbeatData.duration),
                pauseTime = getPauseTime(tripStatus = heartbeatData.tripStatus, currentPauseTime = null),
                endTime = null,
                requiresUpdateOnDatabase = true,
                licensePlate = savedLicensePlate,
                deviceId = savedDeviceId,
                overSpeedDurationInSeconds = overSpeedDurationInSeconds,
                overSpeedCounter = heartbeatData.overspeedCounterDecimal,
                abnormalPulseCounter = heartbeatData.abnormalPulseCounterDecimal,
                mcuStatus = heartbeatData.mcuStatus,
            )
        } else {
            // update Trip
            ongoingTrip.copy(
                tripId = savedOngoingTripId,
                tripStatus = heartbeatData.tripStatus,
                pauseTime = getPauseTime(tripStatus = heartbeatData.tripStatus, currentPauseTime = ongoingTrip.pauseTime),
                fare = heartbeatData.fare,
                extra = heartbeatData.extras,
                totalFare = heartbeatData.totalFare,
                paidDistanceInMeters = heartbeatData.paidDistance,
                unpaidDistanceInMeters = heartbeatData.unpaidDistance,
                waitDurationInSeconds = getTimeInSeconds(heartbeatData.duration),
                overSpeedDurationInSeconds = overSpeedDurationInSeconds,
                requiresUpdateOnDatabase = requiresUpdate,
                overSpeedCounter = heartbeatData.overspeedCounterDecimal,
                abnormalPulseCounter = heartbeatData.abnormalPulseCounterDecimal,
                mcuStatus = heartbeatData.mcuStatus,
            )
        }

        if(ongoingTrip?.fare != heartbeatData.fare && ongoingTrip != null) {
            emitBeepSound(5, 0, 1)
            Log.d(TAG, "handleOngoingHeartbeatResult: fare changed - beep sound emitted")
        }

        TripDataStore.updateTripDataValue(newTrip)

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

    private fun parseHeartbeatResult(result: String): OngoingMCUHeartbeatData? {
        val status = result.extractSubstring(17, 1).toIntOrNull().takeIf { it in 0..7 } ?: return null
        if(!MeasureBoardUtils.isValidDate(result.extractSubstring(56, 12), "yyyyMMddHHmm")) return null
        val heartbeatData = OngoingMCUHeartbeatData(
            measureBoardStatus = status,
            lockedDurationHex = result.extractSubstring(18, 4),
            paidDistanceHex = result.extractSubstring(22, 6),
            duration = result.extractSubstring(28, 6),
            extrasHex = result.extractSubstring(38, 6),
            fareHex = result.extractSubstring(44, 6),
            totalFareHex = result.extractSubstring(50, 6),
            currentTime = result.extractSubstring(56, 12),
            abnormalPulseCounterHex = result.extractSubstring(68, 2),
            overspeedCounterHex = result.extractSubstring(70, 2),
            unpaidDistanceHex = result.extractSubstring(72, 6)
        )

        return heartbeatData.processHexValues()
    }

    private suspend fun handleTripEndSummaryResult(result: String) {
        val distanceHex = result.substring(118, 118 + 6)
        val durationHex = result.substring(124, 124 + 6)
        val fareHex = result.substring(130, 130 + 6)
        val extrasHex = result.substring(136, 136 + 6)
        val totalFareHex = result.substring(142, 142 + 6)
        fun isHBValid(): Boolean {
            distanceHex.toDoubleOrNull()?.takeIf { it >= 0.0 } ?: return false
            if (!MeasureBoardUtils.isDurationValid(durationHex)) return false
            fareHex.toDoubleOrNull()?.takeIf { it >= 0.0 } ?: return false
            extrasHex.toDoubleOrNull()?.takeIf { it >= 0.0 } ?: return false
            totalFareHex.toDoubleOrNull()?.takeIf { it >= 0.0 } ?: return false
            return true
        }
        if (result.length < 147 || !result.startsWith(HEARTBEAT_IDENTIFIER) || !isHBValid()) {
            Log.d(TAG, "handleTripEndSummaryResult: Invalid result length or content: $result")
            Sentry.captureMessage("handleTripEndSummaryResult: Invalid result length or content: $result")
            return
        }
        DeviceDataStore.setMCUHeartbeatActive(true)
        val distance = distanceHex.multiplyBy10AndConvertToDouble()
        val duration = durationHex
        val fare = fareHex.divideBy100AndConvertToDouble()
        val extras = extrasHex.divideBy100AndConvertToDouble()
        val totalFare = totalFareHex.divideBy100AndConvertToDouble()

        Log.d(TAG, "TRIP_END_SUMMARY: $distance, ${getTimeInSeconds(duration)}, $fare, $extras, $totalFare")

        val currentOngoingTrip = TripDataStore.ongoingTripData.firstOrNull()

        if(currentOngoingTrip != null) {
            val newTrip = TripData(
                tripId = currentOngoingTrip.tripId,
                startTime = currentOngoingTrip.startTime,
                tripStatus = TripStatus.ENDED,
                fare = fare,
                extra = extras,
                totalFare = totalFare,
                paidDistanceInMeters = distance,
                waitDurationInSeconds = getTimeInSeconds(duration),
                pauseTime = currentOngoingTrip.pauseTime,
                endTime = Timestamp.now(),
                requiresUpdateOnDatabase = true,
                licensePlate = currentOngoingTrip.licensePlate,
                deviceId = currentOngoingTrip.deviceId
            )
            TripDataStore.updateTripDataValue(newTrip)
        } else {
            Log.d(TAG, "handleTripEndSummaryResult: currentOngoingTripInDB is null")
            Sentry.captureMessage("handleTripEndSummaryResult: currentOngoingTripInDB is null")
        }
        meterPreferenceRepository.saveOngoingTripId("", 0L)
        addTask {
            // after a trip ends, MCU will only continue sending IDLE heartbeats after it receives this response
            mBusModel?.write(Command.CMD_END_RESPONSE)
        }
    }

    private suspend fun checkStatues(result: String) {
        Log.d(TAG, "checkStatues: $result")
        when (getResultType(result)) {
            IDLE_HEARTBEAT -> handleIdleHeartbeatResult(result = result)
            ONGOING_HEARTBEAT -> handleOngoingHeartbearResult(result = result)
            TRIP_END_SUMMARY -> handleTripEndSummaryResult(result = result)
            PARAMETERS_ENQUIRY -> handleParametersEnquiryResult(result = result)
            ABNORMAL_PULSE -> handleAbnormalPulse(result = result)
            REQUEST_UPGRADE_FIRMWARE -> handleUpgradeFirmwareRequestResult(result)
            UPGRADING_FIRMWARE -> handleFirmwareUpdate(result)
            else -> {
                Log.d(TAG, "$TAG_UNKNOWN_RESULT type: ${getResultType(result)}")
                Sentry.captureMessage("$TAG_UNKNOWN_RESULT: $result")
            }
        }
    }

    private suspend fun handleFirmwareUpdate(result: String) {
        val version = result.substring(16, 16 + 8)
        val offset = result.substring(24, 24 + 4)
        val fileName = meterPreferenceRepository.getFirmwareFilenameForOTA().firstOrNull()
        Log.d(TAG, "handleFirmwareUpdate - result $result - offset - ${offset.toIntOrNull(16)}")
        if (offset.toIntOrNull(16) == null || fileName == null) return
        val offsetInt = offset.toInt(16)
        val firmwareBytes =
            MeasureBoardUtils.getPatchMeterBoardFirmwareCmd(fileName, version, offsetInt)
        mBusModel?.sendCmd(firmwareBytes)
        val total = MeasureBoardUtils.getFirmwareTotalOffset(fileName)
        val progress =
            Math.round(((total.toDouble() - offsetInt.toDouble()) / total.toDouble()) * 100)
                .toInt()

        if (progress == 100) {
            DeviceDataStore.setFirmwareUpdateComplete(true)
        }
    }

    private fun handleUpgradeFirmwareRequestResult(result: String) {
        val requestResult = result.substring(18, 18 + 2)
        Log.d(TAG, "REQUEST_UPGRADE_FIRMWARE ${result} == $requestResult")
        if ("FF" == requestResult) {
            Log.i(
                TAG,
                "REQUEST_UPGRADE_FIRMWARE request firmware upgrade fail with result ${requestResult}"
            )
        }
        if ("90" == requestResult) {
            val offset = result.substring(26, 26 + 2)
            Log.i(
                TAG,
                "REQUEST_UPGRADE_FIRMWARE request firmware upgrade success with result ${requestResult} $offset"
            )
        }
    }

    private suspend fun handleAbnormalPulse(result: String) {
        DeviceDataStore.setMCUHeartbeatActive(true)
        TripDataStore.setAbnormalPulseTriggered(isAbnormalPulseTriggered = true)
        Log.d(TAG, "ABNORMAL_PULSE: $result")
    }

    private suspend fun handleParametersEnquiryResult(result: String) {
        if (result.length < 97 || !result.startsWith(HEARTBEAT_IDENTIFIER)) {
            Log.d(TAG, "handleParametersEnquiryResult: Invalid result length: ${result.length}")
            Sentry.captureMessage("handleParametersEnquiryResult: Invalid result length: ${result.length}")
            return
        }
        DeviceDataStore.setMCUHeartbeatActive(true)
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

    override suspend fun requestPatchFirmware(fileName: String) {
        meterPreferenceRepository.saveFirmwareFilenameForOTA(fileName)
        val version = (fileName.split("/").lastOrNull() ?: "").substringBefore(".")
        addTask {
            mBusModel?.write(
                MeasureBoardUtils.getRequestPatchMeterBoardFirmwareCmd(
                    fileName,
                    version
                )!!.toHexString().replace(" ", "")
            )
        }
    }

    override fun close() {
        taskChannel.close()
        messageChannel.close()
        Log.d(TAG, "close")
    }

    private fun setReceiveEvalDataLs() {
        mBusModel?.setListener { data: String ->
            sendMessage(MCUMessage(IAtCmd.W_MSG_DISPLAY, data))
            Log.d(TAG, "setReceiveEvalDataLs $data")
            DeviceDataStore.setBusModelListenerDataReceived(true)
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
        Log.d(TAG, "stopCommunication")
    }


    /**
     * xor Checksum algo
     */
    fun calculateXORChecksum(data: ByteArray): Byte {
        var checksum: Int = 0 // Use Int for intermediate computation
        for (byte in data) {
            checksum = checksum xor byte.toInt()
        }
        return checksum.toByte() // Convert result back to Byte
    }

    /**
     * hex string to byte
     */
    fun hexStringToByteArray(hex: String): ByteArray {
        val fixedHex = if (hex.length % 2 != 0) "0$hex" else hex // Prepend a '0' if the string length is odd
        val length = fixedHex.length
        val byteArray = ByteArray(length / 2)
        for (i in 0 until length step 2) {
            val byte = fixedHex.substring(i, i + 2).toInt(16).toByte()
            byteArray[i / 2] = byte
        }
        return byteArray
    }

    fun validateChecksum(hexString: String): Boolean {
        // Step 1: Locate and trim the 55AA markers
        val startIndex = hexString.indexOf("55AA")
        if (startIndex == -1) return false // No valid 55AA found

        val endIndex = hexString.indexOf("55AA", startIndex + 4) // Look for the next 55AA
        if (endIndex == -1) return false // No second 55AA found

        // Extract the body (everything between the two 55AA markers)
        val bodyHex = hexString.substring(startIndex + 4, endIndex)

        // Step 2: Convert the body into ByteArray and extract the checksum byte
        val bodyBytes = hexStringToByteArray(bodyHex)
        if (bodyBytes.size < 2) return false // Ensure there's at least one data byte and one checksum byte

        val checksumByte = bodyBytes.last() // Last byte is the checksum
        val dataWithoutChecksum = bodyBytes.dropLast(1).toByteArray() // Remove the last byte (checksum)

        // Step 3: Calculate the XOR checksum for the remaining data
        val calculatedChecksum = calculateXORChecksum(dataWithoutChecksum)

        // Step 4: Compare the first byte of the calculated checksum with the checksum byte
        return calculatedChecksum == checksumByte
    }


    companion object {
        private const val WHAT_PRINT_STATUS: Int = 110
        private const val TAG = "MeasureBoardRepository"
        private const val HEARTBEAT_IDENTIFIER = "55AA"
        const val TAG_CHECKSUM_VALIDATION_FAILED = "Checksum validation failed"
        const val TAG_UNKNOWN_RESULT = "Unknown result"
    }
}
