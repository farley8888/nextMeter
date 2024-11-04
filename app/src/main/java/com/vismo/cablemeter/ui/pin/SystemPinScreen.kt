package com.vismo.cablemeter.ui.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vismo.cablemeter.ui.shared.GlobalPinWidget
import com.vismo.cablemeter.ui.theme.nobel900
import com.vismo.cablemeter.ui.theme.pastelGreen700
import com.vismo.cablemeter.ui.theme.valencia700

@Composable
fun SystemPinScreen(
    viewModel: SystemPinViewModel,
    navigate: () -> Unit
) {
    val totpStatus by viewModel.totpStatus.collectAsState()
    val navigationToNextScreen by viewModel.navigationToNextScreen.collectAsState()
    if (navigationToNextScreen) {
        navigate()
        viewModel.resetNavigation()
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
        ) {
            Text(
                text = totpStatus,
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = if (totpStatus.contains("Error")) valencia700 else pastelGreen700
            )

            IconButton(
                onClick = { viewModel.refreshTOTPData() },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Autorenew,
                    contentDescription = "Refresh TOTP",
                )
            }

        }

        Column(
            modifier = Modifier
                .weight(1.2f)
        ) {
            GlobalPinWidget {
                val otp = it
                if (otp.length == 6) {
                    viewModel.verify(otp)
                }
            }
        }
    }
}