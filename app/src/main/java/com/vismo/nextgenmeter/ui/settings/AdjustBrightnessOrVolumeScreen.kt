package com.vismo.nextgenmeter.ui.settings

import android.widget.Space
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.outlined.Brightness5
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.BrightnessLow
import androidx.compose.material.icons.sharp.BrightnessHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vismo.nextgenmeter.ui.theme.nobel400
import com.vismo.nextgenmeter.ui.theme.nobel800

@Composable
fun AdjustBrightnessOrVolumeScreen(
    viewModel: AdjustBrightnessOrVolumeViewModel
) {
    val brightness by viewModel.brightnessLevel.collectAsState()
    val volume by viewModel.volumeLevel.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
    ) {

        Spacer(modifier = Modifier
            .weight(0.12f))

        Column(
            modifier = Modifier
                .weight(0.76f)
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SettingSliderRow(
                value = volume,
                onValueChange = { viewModel.updateVolume(it) },
                iconStart = { Icon(imageVector = Icons.AutoMirrored.Outlined.VolumeDown, contentDescription = "Volume Down Icon", modifier = Modifier.size(36.dp)) },
                label = "音量",
                iconEnd = { Icon(imageVector = Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = "Volume Up Icon", modifier = Modifier.size(36.dp)) }
            )
            Spacer(modifier = Modifier.height(20.dp))
            SettingSliderRow(
                value = brightness,
                onValueChange = { viewModel.updateBrightness(it) },
                iconStart = { Icon(imageVector = Icons.Outlined.Brightness5, contentDescription = "Brightness Low Icon", modifier = Modifier.size(36.dp)) },
                label = "亮度",
                iconEnd = { Icon(imageVector = Icons.Filled.BrightnessHigh, contentDescription = "Brightness High Icon", modifier = Modifier.size(36.dp)) }
            )
        }

        Spacer(modifier = Modifier
            .weight(0.12f))
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
        Text(
            text = label,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            style = TextStyle(fontSize = 26.sp),
        )


        Box(modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(nobel800)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .padding(vertical = 5.dp, horizontal = 10.dp),
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
        }
    }
}