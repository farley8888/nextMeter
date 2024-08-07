package com.vismo.cablemeter.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.vismo.cablemeter.datastore.TripDataStore
import com.vismo.cablemeter.model.TripData
import com.vismo.cablemeter.model.TripStatus
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.util.MeasureBoardUtils
import com.vismo.nxgnfirebasemodule.DashManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

class TripRepositoryImpl @Inject constructor(
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val measureBoardRepository: MeasureBoardRepository,
    private val dashManager: DashManager
) : TripRepository {

    private val tripData = TripDataStore.tripData

    init {
        firebaseAuthRepository.initToken()
        CoroutineScope(ioDispatcher).launch {
            tripData.collect { trip ->
                trip?.let {
                    if (trip.requiresUpdateOnFirestore) {
                        dashManager.updateTripOnFirestore(
                            dashManager.convertToMeterTripInFirestore(
                                it
                            )
                        )
                        dashManager.sendHeartbeat()
                    }
                }
            }
        }
    }

    override fun startTrip() {
        val tripId = MeasureBoardUtils.generateTripId()
        val tripData = TripData(tripId = tripId, startTime = Timestamp.now(), tripStatus = TripStatus.HIRED)
        TripDataStore.setTripData(tripData)
        measureBoardRepository.writeStartTripCommand(MeasureBoardUtils.getIdWithoutHyphens(tripId))
    }

    override fun resumeTrip() {
        tripData.value?.let {
            measureBoardRepository.writeResumeTripCommand()
        }
    }

    override fun startAndPauseTrip() {
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

    override fun printReceipt() {
        TODO("Not yet implemented")
    }
}