package com.vismo.cablemeter.ui.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vismo.cablemeter.ui.shared.GlobalPinWidget
import com.vismo.cablemeter.ui.theme.nobel50
import com.vismo.cablemeter.ui.theme.nobel800
import com.vismo.cablemeter.ui.theme.nobel900
import com.vismo.cablemeter.ui.theme.pastelGreen600
import com.vismo.cablemeter.ui.theme.valencia600

@Composable
fun SystemPinScreen(
    viewModel: SystemPinViewModel,
    systemPinType: SystemPinType,
    navigateToMeterOps: () -> Unit
) {
    val isPinVerified = viewModel.isPinVerified.collectAsState().value
    val isPinSaved = viewModel.isPinSaved.collectAsState().value
    val isPaired = viewModel.isPairSuccessful.collectAsState().value

    if (isPinSaved == true || isPaired == true) {
        navigateToMeterOps()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(nobel900)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(modifier = Modifier.weight(1f)) {
                if (systemPinType is SystemPinType.DriverPin) {
                    if (systemPinType.isPinExistInStorage) {
                        Text(
                            text = "Enter your pin to pair",
                            fontSize = 20.sp,
                            color = nobel50,
                            style = TextStyle(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .padding(8.dp)
                                .wrapContentSize()
                        )
                    } else {
                        Text(
                            text = "Set a pin for faster pairing",
                            fontSize = 20.sp,
                            color = nobel50,
                            style = TextStyle(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .padding(8.dp)
                                .wrapContentSize()
                        )
                    }
                }
            }

            if (systemPinType is SystemPinType.DriverPin && !systemPinType.isPinExistInStorage) {
                Row(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = {
                            viewModel.saveSkipDriverPin(systemPinType.driverPhoneNumber)
                            navigateToMeterOps()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = nobel800,
                            contentColor = nobel50
                        ),
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(horizontal = 32.dp)
                            .height(55.dp)
                    ) {
                        Text(
                            text = "Or skip for now",
                            fontSize = 18.sp,
                        )
                    }
                }
            }

            Row(modifier = Modifier.weight(1f)) {
                when (isPinVerified) {
                    true -> {
                        Text(text = "Pin Verified Successfully. Attempting to pair..", color = pastelGreen600)
                    }
                    false -> {
                        Text(text = "Invalid pin. Remaining attempts: 3", color = valencia600)
                    }
                    else -> {

                    }
                }
            }

        }

        Column(
            modifier = Modifier
                .weight(1.2f)
        ) {
            GlobalPinWidget {
                val otp = it
                if (systemPinType is SystemPinType.DriverPin) {
                    if (systemPinType.isPinExistInStorage) {
                        viewModel.verifyDriverPin(otp, systemPinType.driverPhoneNumber)
                    } else {
                        viewModel.updateDriverPin(otp)
                    }
                }
            }
        }
    }
}

sealed class SystemPinType {
    data class DriverPin(val isPinExistInStorage: Boolean, val driverPhoneNumber: String) : SystemPinType()
    data object SystemPin : SystemPinType()
}