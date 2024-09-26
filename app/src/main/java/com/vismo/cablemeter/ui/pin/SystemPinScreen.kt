package com.vismo.cablemeter.ui.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vismo.cablemeter.ui.shared.GlobalPinWidget
import com.vismo.cablemeter.ui.theme.nobel900

@Composable
fun SystemPinScreen(navigate: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(nobel900)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            // QR code or some other UI

        }

        Column(
            modifier = Modifier
                .weight(1.2f)
        ) {
            GlobalPinWidget {
                val otp = it
                if (otp == "191005") {
                    navigate()
                }
            }
        }
    }
}