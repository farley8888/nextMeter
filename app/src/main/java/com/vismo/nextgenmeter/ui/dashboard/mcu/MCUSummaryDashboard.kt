package com.vismo.nextgenmeter.ui.dashboard.mcu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vismo.nextgenmeter.ui.shared.FlippableCard
import com.vismo.nextgenmeter.ui.theme.Typography
import com.vismo.nextgenmeter.ui.theme.gold350
import com.vismo.nextgenmeter.ui.theme.mineShaft100
import com.vismo.nextgenmeter.ui.theme.mineShaft600
import com.vismo.nextgenmeter.ui.theme.nobel200
import com.vismo.nextgenmeter.ui.theme.nobel500
import com.vismo.nextgenmeter.ui.theme.nobel700
import com.vismo.nextgenmeter.util.GlobalUtils.getFormattedChangedPriceAt
import com.vismo.nextgenmeter.util.GlobalUtils.getFormattedChangedStepPrice
import com.vismo.nextgenmeter.util.GlobalUtils.getFormattedStartPrice
import com.vismo.nextgenmeter.util.GlobalUtils.getFormattedStepPrice
import com.vismo.nextgenmeter.util.GlobalUtils.performVirtualTapFeedback
import kotlinx.coroutines.delay


@Composable
fun MCUSummaryDashboard(
    viewModel: MCUSummaryDashboardViewModel,
    navigate: () -> Unit
) {
    val uiState = viewModel.mcuSummaryUiState.collectAsState().value
    var isFlipped by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // First Column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            val view = LocalView.current
            // Command Button
            Button(
                onClick = {
                    performVirtualTapFeedback(view)
                    navigate()
                          },
                colors = ButtonDefaults.buttonColors(containerColor = gold350),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .height(40.dp)
            ) {
                Text(
                    text = "指令輸入",
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // Region and ID Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier
                        .background(uiState.operatingAreaColor)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = uiState.operatingArea ?:"市區",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color.White)
                        .padding(horizontal = 4.dp)
                        .width(150.dp)
                        .border(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.White)
                        )
                ) {
                    Text(
                        text = uiState.deviceIdData.licensePlate,
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Vehicle Information Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .border(
                        BorderStroke(1.dp, nobel500),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                val startTime = System.currentTimeMillis()
                                val success = tryAwaitRelease()
                                val duration = System.currentTimeMillis() - startTime
                                if (success && duration >= 4000) {
                                    isFlipped = !isFlipped
                                }
                            }
                        )
                    }
            ) {
                Text(
                    text = "${uiState.vehicle?.make} ${uiState.vehicle?.model}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            FlippableCard(
                isFlipped = isFlipped,
                frontContent = {
                    Column {
                        McuInfoForTD(uiState)
                    }
                },
                backContent = {
                    Column {
                        McuInfoForAdmin(uiState)
                    }
                }
            )
        }

        // Second Column
        Column(
            modifier = Modifier
                .weight(1.5f)
                .padding(start = 16.dp)
        ) {
            // Pricing Section
            PricingDetails(uiState)
        }

        // Reset isFlipped after 5 seconds when it becomes true
        LaunchedEffect(isFlipped) {
            if (isFlipped) {
                delay(5000) // Wait for 5 seconds
                isFlipped = false // Reset isFlipped back to false
            }
        }
    }
}

@Composable
fun McuInfoForAdmin(uiState: MCUSummaryUiData) {
    DetailRow("APP版本", uiState.appVersion)
    DetailRow("K值", uiState.fareParams.kValue)
    DetailRow("車費版本", uiState.fareParams.parametersVersion)
    DetailRow("計量固件", uiState.fareParams.firmwareVersion)
}

@Composable
fun McuInfoForTD(uiState: MCUSummaryUiData) {
    DetailRow("安桌固件", uiState.androidROMVersion)
    DetailRow("安桌ID", uiState.androidId)
    DetailRow("計量ID", uiState.deviceIdData.deviceId)
    DetailRow("APP版本", "5.0.0.990")
    DetailRow("K值", uiState.fareParams.kValue)
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "$label: ", color = mineShaft600, fontSize = 16.sp)
        Text(text = value, color = mineShaft100, fontSize = 16.sp)
    }
}

@Composable
fun PricingDetails(uiState: MCUSummaryUiData) {
    Column(
        modifier = Modifier.width(280.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(nobel700)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "收費方式",
                    color = mineShaft100,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "HKS",
                    color = mineShaft100,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        PricingRow("首2公里\n" + "或其它部份", getFormattedStartPrice(uiState.fareParams.startingPrice))
        PricingRow("每200米\n" + "或每分鐘", getFormattedStepPrice(uiState.fareParams.stepPrice))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Gray)
                .padding(8.dp)
        ) {
            Text(
                text = "▼車費達${getFormattedChangedPriceAt(uiState.fareParams.changedPriceAt)}後▼",
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        PricingRow("每200米\n" + "或每分鐘", getFormattedChangedStepPrice(uiState.fareParams.changedStepPrice))
    }
}

@Composable
fun PricingRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                val spacingBeforeLine = 2.dp.toPx()
                val y = size.height - strokeWidth / 2 + spacingBeforeLine
                drawLine(
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    color = nobel200,
                    strokeWidth = 1f
                )
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = Color.White, fontSize = 16.sp)
        Text(
            text = value,
            color = Color.White,
            fontSize = 16.sp,
        )
    }
}
