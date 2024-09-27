package com.vismo.cablemeter.repository

import com.google.firebase.Timestamp
import com.vismo.cablemeter.datastore.TripDataStore
import com.vismo.cablemeter.model.TripData
import com.vismo.cablemeter.model.TripStatus
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.util.MeasureBoardUtils
import com.vismo.nxgnfirebasemodule.DashManager
import com.vismo.nxgnfirebasemodule.model.MeterTripInFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class TripRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val measureBoardRepository: MeasureBoardRepository,
    private val dashManager: DashManager,
    private val localTripsRepository: LocalTripsRepository
) : TripRepository {

    private val tripData = TripDataStore.tripData

    init {
        CoroutineScope(ioDispatcher).launch {
            tripData.collect { trip ->
                trip?.let {
                    if (trip.requiresUpdateOnDatabase) {
                        val tripInFirestore: MeterTripInFirestore = dashManager.convertToType(it)
                        dashManager.updateTripOnFirestore(tripInFirestore)
                        localTripsRepository.updateTrip(it)
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
    }

    override fun resumeTrip() {
        tripData.value?.let {
            measureBoardRepository.writeResumeTripCommand()
        }
    }

    override suspend fun startAndPauseTrip() {
        val tripId = MeasureBoardUtils.generateTripId()
        val tripData = TripData(tripId = tripId, startTime = Timestamp.now(), tripStatus = TripStatus.HIRED)
        TripDataStore.setTripData(tripData)
        measureBoardRepository.writeStartAndPauseTripCommand(MeasureBoardUtils.getIdWithoutHyphens(tripId))
    }

    override fun endTrip() {
        measureBoardRepository.writeEndTripCommand()
    }

    override fun pauseTrip() {
        tripData.value?.let {
            measureBoardRepository.writePauseTripCommand()
        }
    }

    override fun addExtras(extrasAmount: Int) {
        tripData.value?.let {
            val extrasTotal = (it.extra + extrasAmount).toInt()
            if (extrasTotal < 1000) {
                measureBoardRepository.writeAddExtrasCommand(extrasTotal)
            }
        }
    }

    override suspend fun printReceipt() {
        tripData.value?.let {
            if (it.tripStatus == TripStatus.PAUSED) {
                measureBoardRepository.writePrintReceiptCommand(it)
            }
        }
    }
}