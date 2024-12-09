package com.vismo.nextgenmeter.repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.vismo.nextgenmeter.datastore.DeviceDataStore
import com.vismo.nextgenmeter.datastore.TripDataStore
import com.vismo.nextgenmeter.model.OngoingMeasureBoardStatus
import com.vismo.nextgenmeter.model.TripData
import com.vismo.nextgenmeter.model.TripStatus
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.util.MeasureBoardUtils
import com.vismo.nxgnfirebasemodule.DashManager
import com.vismo.nxgnfirebasemodule.model.MeterTripInFirestore
import com.vismo.nxgnfirebasemodule.model.TripPaidStatus
import com.vismo.nxgnfirebasemodule.model.getPricingResult
import com.vismo.nxgnfirebasemodule.model.isDashPayment
import com.vismo.nxgnfirebasemodule.model.paidStatus
import com.vismo.nxgnfirebasemodule.util.LogConstant
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.IScope
import io.sentry.Sentry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TripRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val mainDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val measureBoardRepository: MeasureBoardRepository,
    private val dashManager: DashManager,
    private val localTripsRepository: LocalTripsRepository,
    private val meterPreferenceRepository: MeterPreferenceRepository
) : TripRepository {
    private val _currentTripPaidStatus: MutableStateFlow<TripPaidStatus> = MutableStateFlow(TripPaidStatus.NOT_PAID)
    override val currentTripPaidStatus = _currentTripPaidStatus

    override val remoteUnlockMeter = dashManager.remoteUnlockMeter
    private val _currentAbnormalPulseCounter = MutableStateFlow<Int?>(null)
    private val _currentOverSpeedCounter = MutableStateFlow<Int?>(null)
    private var externalScope: CoroutineScope? = null

    private fun shouldUpdateTrip(trip: TripData): Boolean {
        return trip.requiresUpdateOnDatabase || trip.wasTripJustStarted || dashManager.tripDocumentListener == null
    }

    private fun handleLocalTripUpdateIfNeeded(trip: TripData) {
        if (trip.requiresUpdateOnDatabase) {
            localTripsRepository.updateTrip(trip)
        }
    }

    private suspend fun handleFirestoreTripUpdate(trip: TripData) {
        val tripInFirestore: MeterTripInFirestore = dashManager.convertToType(trip)
        if (dashManager.tripDocumentListener == null) {
            handleTripStart(trip, tripInFirestore)
        } else {
            dashManager.updateTripOnFirestore(tripInFirestore)
        }
    }

    private suspend fun handleTripStart(trip: TripData, tripInFirestore: MeterTripInFirestore) {
        if (trip.wasTripJustStarted) {
            dashManager.createTripOnFirestore(tripInFirestore)
            if (meterPreferenceRepository.getOngoingTripId().first() != trip.tripId) {
                meterPreferenceRepository.saveOngoingTripId(trip.tripId)
            }
        }
        dashManager.setFirestoreTripDocumentListener(trip.tripId)
    }


    override fun initObservers(scope: CoroutineScope) {
        externalScope = scope
        externalScope?.launch(ioDispatcher) {
            launch {
                TripDataStore.ongoingTripData.collect { trip ->
                    trip?.let {
                        // Update trip in Firestore if required
                        if (shouldUpdateTrip(trip)) {
                            handleLocalTripUpdateIfNeeded(trip)
                            handleFirestoreTripUpdate(trip)
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
                TripDataStore.fallbackTripDataToStartNewTrip.collectLatest {
                    val tripId = it?.tripId
                    if (!tripId.isNullOrBlank()) {
                        dashManager.getTripFromFirestore(tripId) { tripInFirestore ->
                            handleOngoingLostTrip(tripInFirestore, newTrip = it)
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
                        Log.i(TAG, "Trip Paid Status: ${_currentTripPaidStatus.value}")
                    }
                }
            }
        }
    }

    private fun handleOngoingLostTrip(tripInFirestore: MeterTripInFirestore?, newTrip: TripData) {
        externalScope?.launch {
            if (tripInFirestore == null) {
                // cloud not find trip in firestore
                withContext(mainDispatcher) {
                    Toast.makeText(context, "Ongoing local trip not found. Creating a new trip.", Toast.LENGTH_SHORT).show()
                    TripDataStore.setTripData(newTrip)
                }
            } else {
                val tripData: TripData = dashManager.convertToType(tripInFirestore)
                TripDataStore.setTripData(
                    tripData.copy(
                        wasTripJustStarted = true, // so that the trip listener is set
                        requiresUpdateOnDatabase = false
                    )
                )
            }
            TripDataStore.clearFallbackTripDataToStartNewTrip()
        }
    }

    override suspend fun startTrip() {
        val tripId = MeasureBoardUtils.generateTripId()
        val licensePlate = DeviceDataStore.deviceIdData.first()?.licensePlate ?: ""
        val deviceId = DeviceDataStore.deviceIdData.first()?.deviceId ?: ""
        val tripData = TripData(tripId = tripId, startTime = Timestamp.now(), tripStatus = TripStatus.HIRED, licensePlate = licensePlate, deviceId = deviceId, wasTripJustStarted = true)
        TripDataStore.setTripData(tripData)
        localTripsRepository.addTrip(tripData)
        measureBoardRepository.writeStartTripCommand(MeasureBoardUtils.getIdWithoutHyphens(tripId))
        Sentry.configureScope { scope: IScope ->
            scope.setTag("trip_id", tripId)
        }
    }

    override fun resumeTrip() {
        TripDataStore.ongoingTripData.value?.let {
            measureBoardRepository.writeResumeTripCommand()
        }
    }

    override suspend fun startAndPauseTrip() {
        val tripId = MeasureBoardUtils.generateTripId()
        val licensePlate = DeviceDataStore.deviceIdData.first()?.licensePlate ?: ""
        val deviceId = DeviceDataStore.deviceIdData.first()?.deviceId ?: ""
        val tripData = TripData(tripId = tripId, startTime = Timestamp.now(), tripStatus = TripStatus.STOP, licensePlate = licensePlate, deviceId = deviceId, wasTripJustStarted = true)
        TripDataStore.setTripData(tripData)
        localTripsRepository.addTrip(tripData)
        measureBoardRepository.writeStartAndPauseTripCommand(MeasureBoardUtils.getIdWithoutHyphens(tripId))
        Sentry.configureScope { scope: IScope ->
            scope.setTag("trip_id", tripId)
        }
    }

    override fun endTrip() {
        measureBoardRepository.writeEndTripCommand()
        Sentry.configureScope { scope: IScope ->
            scope.removeTag("trip_id")
        }
    }

    override fun pauseTrip() {
        TripDataStore.ongoingTripData.value?.let {
            measureBoardRepository.writePauseTripCommand()
        }
    }

    override fun addExtras(extrasAmount: Int) {
        TripDataStore.ongoingTripData.value?.let {
            val extrasTotal = (it.extra + extrasAmount).toInt()
            if (extrasTotal < 1000) {
                measureBoardRepository.writeAddExtrasCommand(extrasTotal)
            }
        }
    }

    override fun subtractExtras(extrasAmount: Int) {
        TripDataStore.ongoingTripData.value?.let {
            val extrasTotal = (it.extra - extrasAmount).toInt()
            val finalTotal = if (extrasTotal < 0) 0 else extrasTotal
            // we want the beep sound to be played when the extras total is 0
            measureBoardRepository.writeAddExtrasCommand(finalTotal)
        }
    }

    override fun lockMeter(beepDuration: Int, beepInterval: Int, beepRepeatCount: Int) {
        externalScope?.launch(ioDispatcher) {
            measureBoardRepository.emitBeepSound(beepDuration, beepInterval, beepRepeatCount)
            delay(1000)
            dashManager.writeLockMeter()
        }
    }

    override fun unlockMeter() {
        measureBoardRepository.unlockMeter()
    }

    override fun resetUnlockMeterStatusInRemote() {
        dashManager.resetUnlockMeterStatusInRemote()
    }

    override suspend fun getMostRecentTrip() {
        measureBoardRepository.emitBeepSound(5, 0, 1)
        val mostRecentTrip = localTripsRepository.getMostRecentCompletedTrip()
        TripDataStore.setMostRecentTripData(mostRecentTrip)
    }

    override fun emitBeepSound() {
        measureBoardRepository.emitBeepSound(5, 0, 1)
        Log.d(TAG, "Beep sound emitted")
    }


    companion object {
        private const val TAG = "TripRepositoryImpl"
    }
}