package com.vismo.cablemeter.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.cablemeter.datastore.MCUParamsDataStore
import com.vismo.cablemeter.repository.MeasureBoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class EditAdminPropertiesViewModel @Inject constructor(
    private val measureBoardRepository: MeasureBoardRepository,
): ViewModel() {

    val mcuPriceParams = MCUParamsDataStore.mcuPriceParams.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = null
    )

    val deviceIdData = MCUParamsDataStore.deviceIdData.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = null
    )

    fun updateKValue(kValue: Int) {
        measureBoardRepository.updateKValue(kValue)
    }

    fun updateLicensePlate(licensePlate: String) {
        measureBoardRepository.updateLicensePlate(licensePlate)
    }

    fun updatePriceParams(
        startPrice: Int, stepPrice: Int, stepPrice2nd:Int, threshold:Int
    ) {
        measureBoardRepository.updatePriceParams(
            startPrice, stepPrice, stepPrice2nd, threshold
        )
    }

    fun reEnquireParameters() {
        measureBoardRepository.enquireParameters()
    }
}