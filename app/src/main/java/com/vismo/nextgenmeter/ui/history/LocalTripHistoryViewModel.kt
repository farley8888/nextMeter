package com.vismo.nextgenmeter.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.nextgenmeter.model.TripData
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.repository.LocalTripsRepository
import com.vismo.nextgenmeter.repository.PeripheralControlRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LocalTripHistoryViewModel @Inject constructor(
    private val localTripsRepository: LocalTripsRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val peripheralControlRepository: PeripheralControlRepository
) : ViewModel() {

    private val _trips: MutableStateFlow<List<TripData>> = MutableStateFlow(emptyList())
    val trips: StateFlow<List<TripData>> = _trips

    init {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                val trips = localTripsRepository.getDescendingSortedTrips()
                _trips.value = trips
            }
        }
    }

    fun printReceipt(tripData: TripData) {
        viewModelScope.launch(ioDispatcher) {
            peripheralControlRepository.printTripReceiptCommand(tripData)
        }
    }
}