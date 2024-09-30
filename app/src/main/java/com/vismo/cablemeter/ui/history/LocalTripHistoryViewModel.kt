package com.vismo.cablemeter.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.cablemeter.model.TripData
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.repository.LocalTripsRepository
import com.vismo.cablemeter.repository.PeripheralControlRepository
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
                _trips.value = localTripsRepository.getAllTrips()
            }
        }
    }

    fun printReceipt(tripData: TripData) {
        viewModelScope.launch(ioDispatcher) {
            peripheralControlRepository.writePrintReceiptCommand(tripData)
        }
    }
}