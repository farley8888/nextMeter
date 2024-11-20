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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.lightspark.composeqr.QrCodeView
import com.vismo.nextgenmeter.ui.shared.GlobalDialog
import com.vismo.nextgenmeter.ui.shared.GlobalSnackbarDelegate
import com.vismo.nextgenmeter.ui.shared.SnackbarState
import com.vismo.nextgenmeter.ui.theme.blueLink
import com.vismo.nextgenmeter.ui.theme.gold350
import com.vismo.nextgenmeter.ui.theme.mineShaft50
import com.vismo.nextgenmeter.ui.theme.mineShaft900
import com.vismo.nextgenmeter.ui.theme.nobel100
import com.vismo.nextgenmeter.ui.theme.nobel400
import com.vismo.nextgenmeter.ui.theme.nobel50
import com.vismo.nextgenmeter.ui.theme.nobel900
import com.vismo.nextgenmeter.ui.theme.primary600
import com.vismo.nextgenmeter.ui.theme.primary800
import com.vismo.nextgenmeter.util.GlobalUtils.performVirtualTapFeedback

@Composable
fun DriverPairScreen(
    viewModel: DriverPairViewModel,
    snackbarDelegate: GlobalSnackbarDelegate,
    navigateToMeterOps : () -> Unit,
) {
    val uiState = viewModel.driverPairScreenUiData.collectAsState().value
    val view = LocalView.current
    val showDialog = remember(uiState.licensePlate) { mutableStateOf(uiState.licensePlate.isBlank()) }
    val isDeviceInfoSetAfterHealthCheck = viewModel.isLicensePlateAndKVUpdated.collectAsState().value

    val healthCheckTitle = "請用車房APP掃描二維碼"
    val healthCheckMessage = "咪錶編號: %s"

    GlobalDialog(
        onDismiss = {},
        showDialog = showDialog,
        isBlinking = false,
        content = {
            HealthCheckDialogContent(
                title = healthCheckTitle,
                message = healthCheckMessage.format(uiState.deviceSerialNumber),
                qrLink = "suntec.app/${uiState.deviceSerialNumber}"
            )
        }
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
            QRCode(qrcodeString = uiState.qrString, viewModel, view)
        }

        Column(
            modifier = Modifier.weight(1f),
        ) {
            StartSession(
                driverPhoneNumber = uiState.driverPhoneNumber,
                viewModel = viewModel,
                navigateToMeterOps, view)
        }
    }
}

@Composable
fun HealthCheckDialogContent(title: String, message: String, qrLink: String) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = nobel100,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "$message\n請用車房APP掃描二維碼",
            color = nobel100,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        DashAndGoldQrCodeView(data = qrLink)
    }
}

@Composable
fun StartSession(
    driverPhoneNumber: String,
    viewModel: DriverPairViewModel,
    navigateToMeterOps: () -> Unit,
    view: View
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                navigateToMeterOps()
                performVirtualTapFeedback(view)
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
                    fontSize = 24.sp,
                )
                Text(
                    text = "已登入",
                    fontSize = 18.sp,
                )
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
                fontSize = 18.sp,
            )
        }
    }
}

@Composable
fun QRCode(qrcodeString: String, viewModel: DriverPairViewModel, view: View) {
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
        modifier = Modifier.fillMaxSize(),
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
                DashAndGoldQrCodeView(data = qrcodeString, size = 250)
            } else {
                QRCodePlaceholder()
            }
        }
    }
}

@Composable
fun DashAndGoldQrCodeView(primaryColor: Color = primary800, data: String, size: Int = 200) {
    val gold = gold350
    QrCodeView(
        data = data,
        modifier = Modifier.size(size.dp),
//        colors = QrCodeColors(
//            background = primaryColor,
//            foreground = gold
//        ),
//        dotShape = DotShape.Square
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(primaryColor)
        ) {
            BasicText(
                text = "D",
                style = TextStyle.Default.copy(
                    color = gold,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif
                )
            )
        }
    }
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
            fontSize = 18.sp,
            modifier = Modifier
                .fillMaxWidth()
                .background(blueLink)
                .padding(8.dp),
            textAlign = TextAlign.Center
        )

        Text(
            color = mineShaft900,
            text = "登入",
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )

        Text(
            text = "60秒內可掃描登入",
            color = mineShaft50,
            fontSize = 18.sp,
            modifier = Modifier
                .fillMaxWidth()
                .background(blueLink)
                .padding(8.dp),
            textAlign = TextAlign.Center
        )
    }
}