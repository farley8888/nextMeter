package com.vismo.cablemeter.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.vismo.cablemeter.datastore.MCUParamsDataStore
import com.vismo.cablemeter.datastore.TripDataStore
import com.vismo.cablemeter.model.OngoingMeasureBoardStatus
import com.vismo.cablemeter.model.TripData
import com.vismo.cablemeter.model.TripStatus
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.util.MeasureBoardUtils
import com.vismo.nxgnfirebasemodule.DashManager
import com.vismo.nxgnfirebasemodule.model.MeterTripInFirestore
import com.vismo.nxgnfirebasemodule.model.TripPaidStatus
import com.vismo.nxgnfirebasemodule.model.getPricingResult
import com.vismo.nxgnfirebasemodule.model.isDashPayment
import com.vismo.nxgnfirebasemodule.model.paidStatus
import com.vismo.nxgnfirebasemodule.util.LogConstant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

class TripRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val measureBoardRepository: MeasureBoardRepository,
    private val dashManager: DashManager,
    private val localTripsRepository: LocalTripsRepository
) : TripRepository {
    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val _currentTripPaidStatus: MutableStateFlow<TripPaidStatus> = MutableStateFlow(TripPaidStatus.NOT_PAID)
    override val currentTripPaidStatus = _currentTripPaidStatus

    override val remoteUnlockMeter = dashManager.remoteUnlockMeter
    private val _currentAbnormalPulseCounter = MutableStateFlow<Int?>(null)
    private val _currentOverSpeedCounter = MutableStateFlow<Int?>(null)

    init {
        initObservers()
    }

    private fun initObservers() {
        repositoryScope.launch {
            launch {
                TripDataStore.tripData.collect { trip ->
                    trip?.let {
                        // Update trip in Firestore if required
                        if (trip.requiresUpdateOnDatabase) {
                            val tripInFirestore: MeterTripInFirestore = dashManager.convertToType(it)
                            dashManager.updateTripOnFirestore(tripInFirestore)
                            localTripsRepository.updateTrip(it)
                        }
                        // Handle abnormal pulse and overspeed counters
                        val abnormalPulseChanged = it.abnormalPulseCounter != _currentAbnormalPulseCounter.value && it.abnormalPulseCounter != null && it.abnormalPulseCounter > 0
                        val overSpeedChanged = it.overSpeedCounter != _currentOverSpeedCounter.value && it.overSpeedCounter != null && it.overSpeedCounter > 0
                        if (abnormalPulseChanged || overSpeedChanged) {
                            val logMap = mapOf(
                                LogConstant.CREATED_BY to LogConstant.CABLE_METER,
                                LogConstant.ACTION to LogConstant.ACTION_PULSE_COUNTER,
                                LogConstant.TRIP_ID to trip.tripId,
                                LogConstant.ABNORMAL_PULSE_COUNTER to trip.abnormalPulseCounter,
                                LogConstant.OVER_SPEED_COUNTER to trip.overSpeedCounter,
                                LogConstant.ONGOING_MEASURE_BOARD_STATUS to OngoingMeasureBoardStatus.fromInt(trip.mcuStatus ?: -1).toString(),
                                LogConstant.OVER_SPEED_LOCKUP_DURATION to trip.overSpeedDurationInSeconds,
                                LogConstant.SERVER_TIME to FieldValue.serverTimestamp(),
                                LogConstant.DEVICE_TIME to Timestamp.now(),
                            )
                            dashManager.writeToLoggingCollection(logMap)

                            // Update current counters
                            _currentOverSpeedCounter.value = trip.overSpeedCounter
                            _currentAbnormalPulseCounter.value = trip.abnormalPulseCounter
                        } else if(trip.abnormalPulseCounter == null && trip.overSpeedCounter == null) {
                            // Clear counters if both are null
                            _currentOverSpeedCounter.value = null
                            _currentAbnormalPulseCounter.value = null
                        }
                    }
                }
            }

            launch {
                dashManager.tripInFirestore.collectLatest { tripInFirestore ->
                    tripInFirestore?.let {
                        val pricingResult = tripInFirestore.getPricingResult(tripInFirestore.isDashPayment())
                        if (pricingResult.applicableTotal != tripInFirestore.total
                            || pricingResult.applicableFee != tripInFirestore.dashFee) {
                            dashManager.updateFirestoreTripTotalAndFee(
                                tripId = tripInFirestore.tripId,
                                total = pricingResult.applicableTotal,
                                fee = pricingResult.applicableFee
                            )
                        }
                        _currentTripPaidStatus.value = tripInFirestore.paidStatus()
                        if (tripInFirestore.tripStatus == com.vismo.nxgnfirebasemodule.model.TripStatus.ENDED) {
                            dashManager.endTripDocumentListener()
                            localTripsRepository.setDashPaymentStatus(tripInFirestore.tripId, tripInFirestore.isDashPayment())
                            _currentTripPaidStatus.value = TripPaidStatus.NOT_PAID
                        }
                    }
                }
            }
        }
    }

    override suspend fun startTrip() {
        val tripId = MeasureBoardUtils.generateTripId()
        val licensePlate = MCUParamsDataStore.deviceIdData.first()?.licensePlate ?: ""
        val deviceId = MCUParamsDataStore.deviceIdData.first()?.deviceId ?: ""
        val tripData = TripData(tripId = tripId, startTime = Timestamp.now(), tripStatus = TripStatus.HIRED, licensePlate = licensePlate, deviceId = deviceId)
        TripDataStore.setTripData(tripData)
        localTripsRepository.addTrip(tripData)
        measureBoardRepository.writeStartTripCommand(MeasureBoardUtils.getIdWithoutHyphens(tripId))
        dashManager.createTripAndSetDocumentListenerOnFirestore(tripId)
    }

    override fun resumeTrip() {
        TripDataStore.tripData.value?.let {
            measureBoardRepository.writeResumeTripCommand()
        }
    }

    override suspend fun startAndPauseTrip() {
        val tripId = MeasureBoardUtils.generateTripId()
        val tripData = TripData(tripId = tripId, startTime = Timestamp.now(), tripStatus = TripStatus.STOP)
        TripDataStore.setTripData(tripData)
        localTripsRepository.addTrip(tripData)
        measureBoardRepository.writeStartAndPauseTripCommand(MeasureBoardUtils.getIdWithoutHyphens(tripId))
        dashManager.createTripAndSetDocumentListenerOnFirestore(tripId)
    }

    override fun endTrip() {
        measureBoardRepository.writeEndTripCommand()
    }

    override fun pauseTrip() {
        TripDataStore.tripData.value?.let {
            measureBoardRepository.writePauseTripCommand()
        }
    }

    override fun addExtras(extrasAmount: Int) {
        TripDataStore.tripData.value?.let {
            val extrasTotal = (it.extra + extrasAmount).toInt()
            if (extrasTotal < 1000) {
                measureBoardRepository.writeAddExtrasCommand(extrasTotal)
            }
        }
    }

    override fun close() {
        repositoryScope.cancel()
    }

    override fun lockMeter(beepDuration: Int, beepInterval: Int, beepRepeatCount: Int) {
        measureBoardRepository.emitBeepSound(beepDuration, beepInterval, beepRepeatCount)
        dashManager.writeLockMeter()
    }

    override fun unlockMeter() {
        measureBoardRepository.unlockMeter()
    }

    override fun resetUnlockMeterStatusInRemote() {
        dashManager.resetUnlockMeterStatusInRemote()
    }

}