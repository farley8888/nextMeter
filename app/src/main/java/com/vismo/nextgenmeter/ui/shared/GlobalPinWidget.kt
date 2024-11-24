package com.vismo.nextgenmeter.ui.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.vismo.nextgenmeter.util.GlobalUtils.performVirtualTapFeedback

@Composable
fun GlobalPinWidget(onOtpEntered: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .width(270.dp),  // 2024 11 24 VAN - not enough time to figure out the real way to make the container width responsive
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        GlobalPinTextField(
            pin = pin,
        )

        Spacer(modifier = Modifier.height(12.dp))

        val view = LocalView.current
        GlobalNumberKeypad(
            onNumberClick = { number ->
                if (pin.length < 6) {
                    pin += number.toString()
                    if (pin.length == 6) {
                        onOtpEntered(pin)
                    }
                }
                performVirtualTapFeedback(view)
            },
            onDeleteClick = {
                if (pin.isNotEmpty()) {
                    pin = pin.dropLast(1)
                }
                performVirtualTapFeedback(view)
            }
        )
    }
}