package com.vismo.nextgenmeter.ui.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.vismo.nextgenmeter.util.GlobalUtils.performVirtualTapFeedback

@Composable
fun GlobalPinWidget(
    onOtpEntered: (String) -> Unit,
    pinState: MutableState<String>,
) {

    Column(
        modifier = Modifier
            .width(270.dp),  // 2024 11 24 VAN - not enough time to figure out the real way to make the container width responsive
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {

        Spacer(modifier = Modifier.height(8.dp))

        GlobalPinTextField(
            pin = pinState.value,
        )

        Spacer(modifier = Modifier.height(20.dp))

        val view = LocalView.current
        GlobalNumberKeypad(
            onNumberClick = { number ->
                if (pinState.value.length < 6) {
                    pinState.value += number.toString()
                    if (pinState.value.length == 6) {
                        onOtpEntered(pinState.value)
                    }
                }
                performVirtualTapFeedback(view)
            },
            onDeleteClick = {
                if (pinState.value.isNotEmpty()) {
                    pinState.value = pinState.value.dropLast(1)
                }
                performVirtualTapFeedback(view)
            }
        )
    }
}