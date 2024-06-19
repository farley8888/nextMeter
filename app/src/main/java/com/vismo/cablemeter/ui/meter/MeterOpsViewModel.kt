package com.vismo.cablemeter.ui.meter

import androidx.lifecycle.ViewModel
import com.vismo.cablemeter.repository.MeasureBoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MeterOpsViewModel
    @Inject
    constructor(
        private val measureBoardRepository: MeasureBoardRepository,
    ) : ViewModel()
