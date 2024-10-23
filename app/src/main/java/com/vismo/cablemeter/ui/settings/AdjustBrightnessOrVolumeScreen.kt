package com.vismo.cablemeter.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdjustBrightnessOrVolumeScreen(
    viewModel: AdjustBrightnessOrVolumeViewModel
) {
    val brightness by viewModel.brightnessLevel.collectAsState()
    val volume by viewModel.volumeLevel.collectAsState()

    Column(
    modifier = Modifier
    .fillMaxSize()
    .padding(16.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Brightness")
        Slider(
            value = brightness,
            onValueChange = { viewModel.updateBrightness(it) },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Volume")
        Slider(
            value = volume,
            onValueChange = { viewModel.updateVolume(it) },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}