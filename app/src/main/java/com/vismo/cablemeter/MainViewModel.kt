package com.vismo.cablemeter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.cablemeter.repository.MeasureBoardRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import com.vismo.cablemeter.module.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val measureBoardRepository: MeasureBoardRepositoryImpl,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel(){

    fun sendPrintCmd() {
        viewModelScope.launch(ioDispatcher) {
//            measureBoardRepository.sendPrintCmd(
//                fare = "1500",
//                extras = "500",
//                duration = "3600",
//                distance = "1500",
//                totalFare = "2000",
//            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        measureBoardRepository.stopCommunication()
    }
}
