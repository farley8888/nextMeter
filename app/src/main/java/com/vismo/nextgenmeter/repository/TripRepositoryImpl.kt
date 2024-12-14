package com.vismo.nextgenmeter.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
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
import io.sentry.IScope
import io.sentry.Sentry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

class TripRepositoryImpl @Inject constructor(
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
    private var isLostTripBeingSearched = false

    private suspend fun handleFirestoreTripUpdate(trip: TripData) {
        val tripInFirestore: MeterTripInFirestore = dashManager.convertToType(trip)
        if (trip.isNewTrip) {
            handleTripStart(trip, tripInFirestore)
        } else {
            dashManager.updateTripOnFirestore(tripInFirestore)
        }
    }

    private suspend fun handleTripStart(trip: TripData, tripInFirestore: MeterTripInFirestore) {
        dashManager.createTripOnFirestore(tripInFirestore)
        if (meterPreferenceRepository.getOngoingTripId().first() != trip.tripId) {
            meterPreferenceRepository.saveOngoingTripId(trip.tripId)
        }
        dashManager.setFirestoreTripDocumentListener(trip.tripId)
    }


    override fun initObservers(scope: CoroutineScope) {
        Log.d(TAG, "initObservers")
        externalScope = scope
        externalScope?.launch(ioDispatcher) {
            launch {
                TripDataStore.ongoingTripData.collect { trip ->
                    trip?.let {
                        // Update trip in Firestore if required
                        if (trip.requiresUpdateOnDatabase && trip.tripId.isNotBlank()) {
                            handleFirestoreTripUpdate(trip)
                            if (trip.tripStatus == TripStatus.ENDED) {
                                val tripData = trip.copy(isDash = _currentTripPaidStatus.value == TripPaidStatus.COMPLETELY_PAID)
                                localTripsRepository.upsertTrip(tripData)
                                meterPreferenceRepository.saveOngoingTripId("")    // Reset ongoing trip id
                                _currentTripPaidStatus.value = TripPaidStatus.NOT_PAID // Reset trip paid status
                                dashManager.endTripDocumentListener()
                            }
                        } else if (trip.tripId.isBlank() && !isLostTripBeingSearched) {
                            // Handle trip id not found in saved preferences
                            Log.e(TAG, "Trip ID not found in saved preferences")
                            isLostTripBeingSearched = true
                            val timeoutJob = launch {
                                delay(10_000) // 10 seconds
                                isLostTripBeingSearched = false
                                Log.d(TAG, "Timeout reached, resetting isLostTripBeingSearched to false.")
                            }
                            dashManager.getLastUnEndedTrip() { latestTripInFirestore ->
                                timeoutJob.cancel()
                                handleOngoingLostTrip(latestTripInFirestore, newTrip = it)
                                isLostTripBeingSearched = false
                            }
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
                        Log.i(TAG, "Trip Paid Status: ${_currentTripPaidStatus.value}")
                    }
                }
            }
        }
    }

    private fun handleOngoingLostTrip(tripInFirestore: MeterTripInFirestore?, newTrip: TripData) {
        externalScope?.launch {
            if (tripInFirestore == null) {
                // cloud not find trip in firestore - make sure to create a new trip
                val newTripId = MeasureBoardUtils.generateTripId()
                handleFirestoreTripUpdate(newTrip.copy(
                    tripId = newTripId,
                    isNewTrip = true,
                    requiresUpdateOnDatabase = true
                ))
                meterPreferenceRepository.saveOngoingTripId(newTripId)
                Log.i(TAG, "Ongoing lost trip not found in Firestore - creating a new trip: ${newTrip.tripId}")
            } else {
                val tripData: TripData = dashManager.convertToType(tripInFirestore)
                meterPreferenceRepository.saveOngoingTripId(tripData.tripId)
                dashManager.setFirestoreTripDocumentListener(tripData.tripId)
                Log.i(TAG, "Ongoing lost trip found in Firestore - using latest un-ended trip: ${tripData.tripId}")
            }
        }
    }

    override suspend fun startTrip() {
        val tripId = MeasureBoardUtils.generateTripId()
        measureBoardRepository.writeStartTripCommand(MeasureBoardUtils.getIdWithoutHyphens(tripId))
        meterPreferenceRepository.saveOngoingTripId(tripId)
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
        measureBoardRepository.writeStartAndPauseTripCommand(MeasureBoardUtils.getIdWithoutHyphens(tripId))
        meterPreferenceRepository.saveOngoingTripId(tripId)
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