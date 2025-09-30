package com.vismo.nextgenmeter.ui.pair

import android.os.CountDownTimer
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lightspark.composeqr.QrCodeView
import com.vismo.nextgenmeter.ui.pair.DriverPairViewModel.Companion.AUTO_NAVIGATE_DURATION
import com.vismo.nextgenmeter.ui.pair.DriverPairViewModel.Companion.DEFAULT_AUTO_NAVIGATE_COUNTDOWN_VALUE
import com.vismo.nextgenmeter.ui.shared.GenericDialogContent
import com.vismo.nextgenmeter.ui.shared.GlobalDialog
import com.vismo.nextgenmeter.ui.shared.GlobalSnackbarDelegate
import com.vismo.nextgenmeter.ui.shared.SnackbarState
import com.vismo.nextgenmeter.ui.theme.Typography
import com.vismo.nextgenmeter.ui.theme.blueLink
import com.vismo.nextgenmeter.ui.theme.mineShaft50
import com.vismo.nextgenmeter.ui.theme.mineShaft900
import com.vismo.nextgenmeter.ui.theme.nobel100
import com.vismo.nextgenmeter.ui.theme.nobel400
import com.vismo.nextgenmeter.ui.theme.nobel50
import com.vismo.nextgenmeter.ui.theme.nobel900
import com.vismo.nextgenmeter.ui.theme.primary600
import com.vismo.nextgenmeter.util.GlobalUtils.performVirtualTapFeedback

@Composable
fun DriverPairScreen(
    viewModel: DriverPairViewModel,
    snackbarDelegate: GlobalSnackbarDelegate,
    navigateToMeterOps : () -> Unit,
) {
    val uiState = viewModel.driverPairScreenUiData.collectAsState().value
    val view = LocalView.current
    val showHealthCheckDialog = remember(uiState.licensePlate, uiState.isReceivedFirstHeartBeat) { mutableStateOf(uiState.licensePlate.isBlank() && uiState.isReceivedFirstHeartBeat) }
    val showTurnOnAccDialog = remember(uiState.isReceivedFirstHeartBeat) { mutableStateOf(!uiState.isReceivedFirstHeartBeat) }
    val isDeviceInfoSetAfterHealthCheck = viewModel.isLicensePlateAndKVUpdated.collectAsState().value

    val healthCheckTitle = "請用車房APP掃描二維碼"
    val healthCheckMessage = "咪錶編號: %s"

    GlobalDialog(
        onDismiss = {},
        showDialog = showHealthCheckDialog,
        isBlinking = false,
        content = {
            HealthCheckDialogContent(
                title = healthCheckTitle,
                message = healthCheckMessage.format(uiState.deviceSerialNumber),
                qrLink = "suntec.app/${uiState.deviceSerialNumber}",
                dismiss = { showHealthCheckDialog.value = false }
            )
        },
        usePlatformDefaultWidth = false,
        width = 350,
        height = 350
    )

    GlobalDialog(
        onDismiss = {},
        showDialog = showTurnOnAccDialog,
        isBlinking = false,
        content = {
            GenericDialogContent(
                title = "未檢測到匙火ACC",
                message = "如需啟動咪表，請起動車輛（點起匙火ACC)。如不用，請按下面按鈕開始關機",
                confirmButtonText = "開始關機",
                onConfirm = {
                    viewModel.startACCStatusInquiries()
                },
            )
        },
    )

    if (isDeviceInfoSetAfterHealthCheck) {
        snackbarDelegate.showSnackbar(SnackbarState.SUCCESS,"License Plate and K-Value set successfully")
        viewModel.clearLicensePlateAndKVUpdated()
    }

    Row (
        modifier =
        Modifier
            .background(color = nobel900)
            .fillMaxSize()
    ){
        Column(
            modifier = Modifier.weight(1f),
        ) {
            QRCode(qrcodeString = uiState.qrString, viewModel, view, modifier = Modifier.weight(1f).fillMaxWidth())
            Text(text = uiState.deviceSerialNumber, color = Color.White, style = Typography.labelSmall, modifier = Modifier.padding(horizontal = 18.dp, vertical = 0.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
        ) {
            StartSession(
                driverPhoneNumber = uiState.driverPhoneNumber,
                viewModel = viewModel,
                navigateToMeterOps, view, modifier = Modifier.weight(1f).fillMaxWidth())
            Text(text = uiState.licensePlate, color = Color.White, style = Typography.labelSmall, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 0.dp), textAlign = TextAlign.End)
        }
    }
}

@Composable
fun HealthCheckDialogContent(title: String, message: String, qrLink: String, dismiss: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = nobel100,
            style = Typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = "$message\n請用車房APP掃描二維碼",
            color = nobel100,
            style = Typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(nobel100)
                .zIndex(1f),
            contentAlignment = Alignment.Center
        ) {
            DashQrCodeView(data = qrLink)
        }
        Button(
            onClick = dismiss,
            colors = ButtonDefaults.buttonColors(
                containerColor = primary600,
                contentColor = nobel50
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(16.dp)
        ) {
        }
    }
}

@Composable
fun StartSession(
    driverPhoneNumber: String,
    viewModel: DriverPairViewModel,
    navigateToMeterOps: () -> Unit,
    view: View,
    modifier: Modifier
) {
    val autoNavigateCountDown = viewModel.autoNavigateCountdown.collectAsState(initial = DEFAULT_AUTO_NAVIGATE_COUNTDOWN_VALUE).value

    if (autoNavigateCountDown == 0) {
        navigateToMeterOps()
    }

    Column(
        modifier = modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                if (driverPhoneNumber.isNotEmpty()) {
                    navigateToMeterOps()
                    performVirtualTapFeedback(view)
                }
                      },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (driverPhoneNumber.isNotEmpty()) primary600 else nobel400,
                contentColor = nobel50
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(100.dp)
        ) {
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ){
                Text(
                    text = driverPhoneNumber,
                    style = Typography.titleLarge
                )
                Text(
                    text = "已登入",
                    style = Typography.bodyLarge
                )
                if (autoNavigateCountDown in 0..AUTO_NAVIGATE_DURATION) {
                    Text(
                        text = "$autoNavigateCountDown 秒後跳至下一頁",
                        style = Typography.labelSmall
                    )
                }
            }

        }
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = {
                viewModel.clearDriverSession()
                navigateToMeterOps()
                performVirtualTapFeedback(view)
                      },
            colors = ButtonDefaults.buttonColors(
                containerColor = nobel400,
                contentColor = nobel50
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(100.dp)
        ) {
            Text(
                text = "訪客模式",
                style = Typography.bodyLarge
            )
        }
    }
}

@Composable
fun QRCode(qrcodeString: String, viewModel: DriverPairViewModel, view: View, modifier: Modifier) {
    var showQRCode by remember { mutableStateOf(false) }

    // Start a countdown when QR code is shown
    if (showQRCode) {
        viewModel.refreshQr()
        LaunchedEffect(Unit) {
            object : CountDownTimer(60000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                   // not needed
                }

                override fun onFinish() {
                    showQRCode = false

                }
            }.start()
        }
    }
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(nobel100)
                .clickable {
                    showQRCode = true
                    performVirtualTapFeedback(view)
                }
                .zIndex(1f),
            contentAlignment = Alignment.Center
        ) {
            if (showQRCode && qrcodeString.isNotEmpty()) {
                DashQrCodeView(data = qrcodeString)
            } else {
                QRCodePlaceholder()
            }
        }
    }
}

@Composable
fun DashQrCodeView(data: String, size: Int = 250) {
    QrCodeView(
        data = data,
        modifier = Modifier
            .size(size.dp)
            .padding(16.dp),
    )
}

@Composable
fun QRCodePlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "按此產生登入二維碼",
            color = mineShaft50,
            modifier = Modifier
                .fillMaxWidth()
                .background(blueLink)
                .padding(8.dp),
            textAlign = TextAlign.Center,
            style = Typography.bodyLarge
        )

        Text(
            color = mineShaft900,
            text = "登入",
            style = Typography.displayLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )

        Text(
            text = "60秒內可掃描登入",
            color = mineShaft50,
            style = Typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .background(blueLink)
                .padding(8.dp),
            textAlign = TextAlign.Center
        )
    }
}