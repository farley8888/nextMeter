package com.vismo.cablemeter.ui.dashboard.mcu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vismo.cablemeter.ui.theme.mineShaft100
import com.vismo.cablemeter.ui.theme.mineShaft600
import com.vismo.cablemeter.ui.theme.nobel200
import com.vismo.cablemeter.ui.theme.nobel500
import com.vismo.cablemeter.ui.theme.nobel700


@Composable
fun MCUSummaryDashboard(
    viewModel: MCUSummaryDashboardViewModel,
    navigate: () -> Unit
) {
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
            // Command Button
            Button(
                onClick = { /*TODO*/ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .height(40.dp)
            ) {
                Text(
                    text = "指令输入",
                    fontSize = 20.sp,
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
                        .background(Color.Red)
                        .padding(horizontal = 16.dp)
                        .border(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.Red)
                        )
                ) {
                    Text(
                        text = "市區",
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
                        text = "DASH02T",
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
            ) {
                Text(
                    text = "TOYOTA CROWN",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            DetailRow("安桌固性", "1.5.1")
            DetailRow("安桌ID", "NGSM2406A0600")
            DetailRow("計量ID", "2407100017")
            DetailRow("計量固性", "24071691")
            DetailRow("APP版本", "v5.0.0.946 (1.6.0)")
            DetailRow("車費版本", "240714B1")
            DetailRow("K值", "0650")
        }

        // Second Column
        Column(
            modifier = Modifier
                .weight(1.5f)
                .padding(start = 16.dp)
        ) {
            // Pricing Section
            PricingDetails()
        }
    }
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
fun PricingDetails() {
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
        PricingRow("首2公里\n" + "或其它部份", "$24.00")
        PricingRow("每200米\n" + "或每分鐘", "+$1.90")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Gray)
                .padding(8.dp)
        ) {
            Text(
                text = "▼車費達$195.00後▼",
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        PricingRow("每200米\n" + "或每分鐘", "+$1.60")
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
