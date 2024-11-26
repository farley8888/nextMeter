package com.vismo.nextgenmeter.ui.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lightspark.composeqr.QrCodeView
import com.vismo.nextgenmeter.ui.shared.FlippableCard
import com.vismo.nextgenmeter.ui.shared.GlobalPinWidget
import com.vismo.nextgenmeter.ui.theme.mineShaft900
import com.vismo.nextgenmeter.ui.theme.nobel500
import com.vismo.nextgenmeter.ui.theme.nobel900
import com.vismo.nextgenmeter.ui.theme.pastelGreen700
import com.vismo.nextgenmeter.ui.theme.valencia50
import com.vismo.nextgenmeter.ui.theme.valencia700
import com.vismo.nextgenmeter.util.GlobalUtils.performVirtualTapFeedback

@Composable
fun SystemPinScreen(
    viewModel: SystemPinViewModel,
    navigate: () -> Unit
) {
    val totpStatus = viewModel.totpStatus.collectAsState()
    val navigationToNextScreen by viewModel.navigationToNextScreen.collectAsState()
    if (navigationToNextScreen) {
        navigate.invoke()
        viewModel.resetNavigation()
    }

    SystemPinScreenForm(
        onOtpInputComplete = { otp ->
            viewModel.verify(otp)
        },
        onRefresh = {
            viewModel.refreshTOTPData()
        },
        totpStatus = totpStatus.value
    )
}


@Composable
fun SystemPinScreenForm(
    dummyQRCodeString: String = "https://d-ash.com",
    onOtpInputComplete: ((otp: String) -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    totpStatus: String = "Loading..."
) {
    var isFlipped by remember { mutableStateOf(false) }
    val localView = LocalView.current

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(nobel900),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            FlippableCard(
                isFlipped = isFlipped,
                frontContent = {
                    QrCode(dummyQRCodeString)
                },
                backContent = {
                    TOTPDetails(totpStatus = totpStatus, onRefresh = onRefresh)
                }
            )
        }

        Column(
            modifier = Modifier
                .wrapContentWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GlobalPinWidget {
                val otp = it
                if (otp.length == 6) {
                    if (otp == "000005") {
                        isFlipped = !isFlipped
                    } else {
                        onOtpInputComplete?.invoke(otp)
                    }
                    performVirtualTapFeedback(localView)
                }
            }
        }
    }
}

@Composable
fun TOTPDetails(totpStatus: String, onRefresh: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .background(valencia50)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "TOTP Mode", style = MaterialTheme.typography.headlineSmall, color = mineShaft900)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = totpStatus,
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = if (totpStatus.contains("Error")) valencia700 else pastelGreen700,
            )
            Spacer(modifier = Modifier.height(16.dp))
            IconButton(
                onClick = { onRefresh?.invoke() },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(32.dp)
                    .background(nobel500)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Autorenew,
                    contentDescription = "Refresh TOTP",
                )
            }
        }
    }
}

@Composable
fun QrCode(dummyQRCodeString: String) {
    Box(
        modifier = Modifier
            .background(Color.White)
            .wrapContentSize()
    ) {
        QrCodeView(
            data = dummyQRCodeString,
            modifier = Modifier
                .size(240.dp)
                .padding(24.dp)
        )
    }
}


@Preview
@Composable
fun SystemPinScreenFormPreview() {
    SystemPinScreenForm()
}