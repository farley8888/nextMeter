package com.vismo.cablemeter.ui.pair

import android.os.CountDownTimer
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
import com.vismo.cablemeter.ui.theme.nobel900
import com.vismo.cablemeter.ui.theme.primary600

@Composable
fun DriverPairScreen(viewModel: DriverPairViewModel) {
    val uiState = viewModel.driverPairScreenUiData.collectAsState().value
    Row (
        modifier =
        Modifier
            .background(color = nobel900)
            .fillMaxSize()
    ){
        Column(
            modifier = Modifier.weight(1f),
        ) {
            QRCode(qrcodeString = uiState.qrString, viewModel)
        }

        Column(
            modifier = Modifier.weight(1f),
        ) {
            StartSession(driverPhoneNumber = uiState.driverPhoneNumber)
        }
    }
}

@Composable
fun StartSession(driverPhoneNumber: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { /*TODO*/ },
            colors = ButtonDefaults.buttonColors(
                containerColor = primary600,
                contentColor = nobel50
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(100.dp)
        ) {
            Text(
                text = "$driverPhoneNumber 已登入",
                fontSize = 18.sp,
            )
        }
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = { /*TODO*/ },
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
fun QRCode(qrcodeString: String, viewModel: DriverPairViewModel) {
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
                .clickable { showQRCode = true }
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