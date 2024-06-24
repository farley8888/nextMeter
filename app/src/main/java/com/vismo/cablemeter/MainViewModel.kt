package com.vismo.cablemeter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.cablemeter.repository.MeasureBoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.vismo.cablemeter.module.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val measureBoardRepository: MeasureBoardRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel(){

    val measureBoardData = combine(measureBoardRepository.deviceIdData, measureBoardRepository.tripStatus) {
            deviceIdData, tripStatus ->
        deviceIdData?.let {
            tripStatus?.let {
                Pair(deviceIdData, tripStatus)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun sendPrintCmd() {
        viewModelScope.launch(ioDispatcher) {
            measureBoardRepository.sendPrintCmd(
                fare = "1500",
                extras = "500",
                duration = "3600",
                distance = "1500",
                totalFare = "2000",
            )
        }
    }
}
