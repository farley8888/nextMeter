package com.vismo.cablemeter.repository

import com.google.firebase.Timestamp
import com.vismo.cablemeter.datastore.TripDataStore
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    init {
        initObservers()
    }

    private fun initObservers() {
        repositoryScope.launch {
            launch {
                TripDataStore.tripData.collect { trip ->
                    trip?.let {
                        if (trip.requiresUpdateOnDatabase) {
                            val tripInFirestore: MeterTripInFirestore = dashManager.convertToType(it)
                            dashManager.updateTripOnFirestore(tripInFirestore)
                            localTripsRepository.updateTrip(it)
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
                            _currentTripPaidStatus.value = TripPaidStatus.NOT_PAID
                        }
                    }
                }
            }
        }
    }

    override suspend fun startTrip() {
        val tripId = MeasureBoardUtils.generateTripId()
        val tripData = TripData(tripId = tripId, startTime = Timestamp.now(), tripStatus = TripStatus.HIRED)
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
        val tripData = TripData(tripId = tripId, startTime = Timestamp.now(), tripStatus = TripStatus.HIRED)
        TripDataStore.setTripData(tripData)
        localTripsRepository.addTrip(tripData)
        measureBoardRepository.writeStartAndPauseTripCommand(MeasureBoardUtils.getIdWithoutHyphens(tripId))
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

}