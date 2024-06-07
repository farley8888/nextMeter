package com.vismo.cablemeter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.cablemeter.repository.MeasureBoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val measureBoardRepository: MeasureBoardRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel(){
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