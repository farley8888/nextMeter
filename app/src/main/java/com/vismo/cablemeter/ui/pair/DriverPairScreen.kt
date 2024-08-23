package com.vismo.cablemeter.ui.pair

import android.os.CountDownTimer
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.vismo.cablemeter.ui.theme.mineShaft50
import com.vismo.cablemeter.ui.theme.mineShaft900
import com.vismo.cablemeter.ui.theme.nobel100
import com.vismo.cablemeter.ui.theme.nobel400
import com.vismo.cablemeter.ui.theme.nobel50
import com.vismo.cablemeter.ui.theme.nobel800
import com.vismo.cablemeter.ui.theme.nobel900
import com.vismo.cablemeter.ui.theme.primary600
import com.vismo.cablemeter.util.GlobalUtils.performVirtualTapFeedback

@Composable
fun DriverPairScreen(
    viewModel: DriverPairViewModel,
    navigateToMeterOps : () -> Unit,
    navigateToPinScreen : (String, Boolean) -> Unit
) {
    val uiState = viewModel.driverPairScreenUiData.collectAsState().value
    val view = LocalView.current

    if(uiState.isDriverPinSet == false) {
        navigateToPinScreen(uiState.driverPhoneNumber, false)
        viewModel.clearIsDriverPinSet()
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
                navigateToMeterOps = navigateToMeterOps,
                navigateToPinScreen = navigateToPinScreen,
                view = view
            )
        }
    }
}

@Composable
fun StartSession(
    driverPhoneNumber: String,
    viewModel: DriverPairViewModel,
    navigateToMeterOps: () -> Unit,
    navigateToPinScreen: (String, Boolean) -> Unit,
    view: View
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (driverPhoneNumber.isNotEmpty()) {
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
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
        } else {
            // show saved driver options if no driver is logged in
            val savedDriverIds = viewModel.savedDriverIds.collectAsState().value
            if (savedDriverIds.isNotEmpty()) {
                Text(
                    text = "掃描二維碼或使用 PIN 碼進行配對",
                    fontSize = 20.sp,
                    color = nobel50,
                    style = TextStyle(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(8.dp).wrapContentSize()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.Top
                ) {
                    // table of saved drivers arranged 2x2
                    for (i in savedDriverIds.indices step 2) {
                        Row {
                            for (j in i until i + 2) {
                                if (j < savedDriverIds.size) {
                                    Button(
                                        onClick = {
                                            navigateToPinScreen(savedDriverIds[j], true)
                                            performVirtualTapFeedback(view)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = nobel400,
                                            contentColor = nobel50
                                        ),
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .weight(1f)
                                            .wrapContentSize()
                                    ) {
                                        Text(
                                            text = savedDriverIds[j],
                                            fontSize = 18.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
//        Spacer(modifier = Modifier.height(40.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(.8f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Button(
                onClick = {
                    viewModel.clearDriverSession()
                    navigateToMeterOps()
                    performVirtualTapFeedback(view)
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
                    text = "作為客人使用",
                    fontSize = 18.sp,
                )
            }
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
                val barcodeEncoder = BarcodeEncoder()
                val bitmap = barcodeEncoder.encodeBitmap(qrcodeString, BarcodeFormat.QR_CODE, 300, 300)
                AndroidView(
                    { android.widget.ImageView(it) },
                    modifier = Modifier.fillMaxSize(),
                    update = { it.setImageBitmap(bitmap) }
                )
            } else {
                QRCodePlaceholder()
            }
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
                .background(Color(0xFF1E88E5))
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
                .background(Color(0xFF1E88E5))
                .padding(8.dp),
            textAlign = TextAlign.Center
        )
    }
}