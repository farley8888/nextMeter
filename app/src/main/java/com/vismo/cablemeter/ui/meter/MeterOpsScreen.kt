package com.vismo.cablemeter.ui.meter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vismo.cablemeter.model.MeterOpsUiData
import com.vismo.cablemeter.ui.theme.Black


@Composable
fun MeterOpsScreen(viewModel: MeterOpsViewModel) {
    val focusRequester = remember { FocusRequester() }
    val uiState = viewModel.uiState.collectAsState().value

    Column(
        modifier =
        Modifier
            .background(color = Black)
            .focusRequester(focusRequester)
            .focusable()
            .fillMaxSize()
            .onKeyEvent {
                if (it.type == KeyDown) {
                    val code = it.nativeKeyEvent.scanCode
                    val repeatCount = it.nativeKeyEvent.repeatCount
                    val isLongPress = it.nativeKeyEvent.isLongPress
                    viewModel.handleKeyEvent(code, repeatCount, isLongPress)
                }
                true
            }
    ) {
        TaxiMeterUI(uiState)
    }

    // Request focus when the composable is first composed
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun ColumnScope.TaxiMeterUI(uiState: MeterOpsUiData) {
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.weight(3f)
    ) {
        DetailsBox(uiState)
        TotalBox(uiState)
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .weight(1f),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        DistanceTimeAndStatusBox(uiState)
    }
}

@Composable
fun RowScope.DetailsBox(uiState: MeterOpsUiData) {
    Column(
        modifier = Modifier
            .size(height = 200.dp, width = 150.dp) // Set a constant height and width
            .border(1.dp, Color.White) // Adding a border
            .weight(1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top

        ) {
            Text(text = "DETAILS", color = Color.White)
            Text(text = "H.K.$", color = Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.End
        ){
            val fare = uiState.fare
            val fareDouble = fare.toDoubleOrNull()
            if (fareDouble != null && fareDouble > 0) {
                Text(
                    text = fare.substring(0, uiState.fare.length - 1),
                    color = Color.Green,
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                )
            }
            Text(
                text = "FARE",
                color = Color.Yellow,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .rotate(270f) // Orienting the text vertically
                    .offset(x = 6.dp) // Move the text down
                    .offset(y = 5.dp) // Move the text right
                    .align(Alignment.CenterVertically)
            )

        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.End
        ) {
            val fare = uiState.fare
            val fareDouble = fare.toDoubleOrNull()
            val extras = uiState.extras
            val extrasDouble = extras.toDoubleOrNull()
            if (extrasDouble != null && fareDouble != null && fareDouble > 0) {
                Text(
                    text = extras,
                    color = Color.Green,
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                )
            }
            Text(
                text = "EXTRA",
                color = Color.Yellow,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .rotate(270f) // Orienting the text vertically
                    .offset(x = 6.dp) // Move the text down
                    .offset(y = 5.dp) // Move the text right
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun RowScope.TotalBox(uiState: MeterOpsUiData) {
    Column(
        modifier = Modifier
            .weight(2f)
            .padding(8.dp)
            .height(height = 200.dp) // Set a constant height
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top

        ) {
            Text(text = "TOTAL", color = Color.White)
            Text(text = "H.K.$", color = Color.White)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.End
        ){
            val totalFare = uiState.totalFare
            val size = totalFare.length
            val fontSize = when {
                size < 6 -> 150.sp
                size < 7 -> 120.sp
                else -> 100.sp
            }
            val totalFareDouble = totalFare.toDoubleOrNull()
            if (totalFareDouble != null && totalFareDouble > 0) {
                Text(
                    text = totalFare,
                    color = Color.Green,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
fun RowScope.DistanceTimeAndStatusBox(uiState: MeterOpsUiData) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "DIST. (KM)", color = Color.Gray, textAlign = TextAlign.Start)
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = uiState.distanceInKM, color = Color.Gray, fontSize = 36.sp, textAlign = TextAlign.End)
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "TIME", color = Color.Gray, textAlign = TextAlign.Start)
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = uiState.duration, color = Color.Gray, fontSize = 36.sp, textAlign = TextAlign.End)
            }
        }
        Button(
            onClick = { /*TODO*/ },
            modifier = Modifier
                .weight(1.3f)
                .padding(2.dp),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(
                text = "${uiState.status.toStringCN()} ${uiState.status.toStringEN()}",
                color = Color.White,
                fontSize = 24.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
