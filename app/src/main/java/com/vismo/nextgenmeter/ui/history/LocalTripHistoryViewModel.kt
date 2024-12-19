package com.vismo.nextgenmeter.ui.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.vismo.nextgenmeter.datastore.TripDataStore
import com.vismo.nextgenmeter.model.TripData
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.repository.LocalTripsRepository
import com.vismo.nextgenmeter.repository.PeripheralControlRepository
import com.vismo.nextgenmeter.repository.TripFileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LocalTripHistoryViewModel @Inject constructor(
    private val tripFileManager: TripFileManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val peripheralControlRepository: PeripheralControlRepository
) : ViewModel() {

    private val _trips: MutableStateFlow<List<TripData>> = MutableStateFlow(emptyList())
    val trips: StateFlow<List<TripData>> = _trips

    init {
        viewModelScope.launch {
            tripFileManager.descendingSortedTrip.collectLatest {
                _trips.value = it
            }
        }
    }

    fun printReceipt(tripData: TripData) {
        viewModelScope.launch(ioDispatcher) {
            peripheralControlRepository.printTripReceiptCommand(tripData)
        }
    }

    companion object {
        private const val TAG = "LocalTripHistoryViewModel"
    }
}