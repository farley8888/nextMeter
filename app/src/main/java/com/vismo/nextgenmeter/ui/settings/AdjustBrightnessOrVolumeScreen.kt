package com.vismo.nextgenmeter.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material3.Icon
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
        SettingSliderRow(
            value = volume,
            onValueChange = { viewModel.updateVolume(it) },
            iconStart = { Icon(imageVector = Icons.AutoMirrored.Filled.VolumeDown, contentDescription = "Volume Down Icon") },
            label = "Volume",
            iconEnd = { Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Volume Up Icon") }
        )
        Spacer(modifier = Modifier.height(28.dp))
        SettingSliderRow(
            value = brightness,
            onValueChange = { viewModel.updateBrightness(it) },
            iconStart = { Icon(imageVector = Icons.Default.BrightnessLow, contentDescription = "Brightness Low Icon") },
            label = "Brightness",
            iconEnd = { Icon(imageVector = Icons.Default.BrightnessHigh, contentDescription = "Brightness High Icon") }
        )
    }
}

@Composable
fun SettingSliderRow(
    value: Float,
    onValueChange: (Float) -> Unit,
    iconStart: @Composable () -> Unit,
    iconEnd: @Composable () -> Unit,
    label: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            iconStart()
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..1f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )
            iconEnd()
        }
        Text(
            text = label,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
    }
}