package com.vismo.cablemeter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.cablemeter.model.TopAppBarUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.repository.FirebaseAuthRepository
import com.vismo.cablemeter.repository.MeasureBoardRepository
import com.vismo.cablemeter.repository.RemoteMCUControlRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val measureBoardRepository: MeasureBoardRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val remoteMCUControlRepository: RemoteMCUControlRepository
    ) : ViewModel(){

    private val _topAppBarUiState = MutableStateFlow(TopAppBarUiState())
    val topAppBarUiState: StateFlow<TopAppBarUiState> = _topAppBarUiState

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

    private fun observeFlows() {
        viewModelScope.launch(ioDispatcher) {
            launch {
                measureBoardRepository.mcuTime.collectLatest {
                    it?.let { dateTime ->
                        _topAppBarUiState.value = _topAppBarUiState.value.copy(
                            dateTime = dateTime
                        )
                    }
                }
            }
        }
    }

    fun updateBackButtonVisibility(isVisible: Boolean) {
        _topAppBarUiState.value = _topAppBarUiState.value.copy(
            isBackButtonVisible = isVisible
        )
    }


    init {
        viewModelScope.launch {
            firebaseAuthRepository.initToken()
            remoteMCUControlRepository.observeFlows()
        }
        observeFlows()
    }

    override fun onCleared() {
        super.onCleared()
        measureBoardRepository.stopCommunication()
    }
}
