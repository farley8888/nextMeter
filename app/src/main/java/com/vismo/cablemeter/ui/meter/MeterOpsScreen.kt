package com.vismo.cablemeter.ui.meter

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vismo.cablemeter.R
import com.vismo.cablemeter.model.TripStateInMeterOpsUI
import com.vismo.cablemeter.ui.theme.Black
import com.vismo.cablemeter.ui.theme.LightGray
import com.vismo.cablemeter.ui.theme.Red
import com.vismo.cablemeter.ui.theme.SuperLightGray
import com.vismo.cablemeter.ui.theme.White


@Composable
fun MeterOpsScreen(viewModel: MeterOpsViewModel) {
    val focusRequester = remember { FocusRequester() }
    val uiState = viewModel.uiState.collectAsState()

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .background(color = Black)
            .focusRequester(focusRequester)
            .focusable()
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
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
        ) {
            ExtrasGroup(
                modifier =
                    Modifier
                        .weight(1f), uiState.value.extras

            )
            FareGroup(
                modifier =
                    Modifier
                        .weight(2f), uiState.value.fare
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        PriceSeparatedLine()
        Spacer(modifier = Modifier.height(8.dp))
        MetricGroup(uiState.value.status, uiState.value.distanceInKM, uiState.value.duration)
    }

    // Request focus when the composable is first composed
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun FareGroup(modifier: Modifier, fareValue: String) {
    Column(
        modifier =
            modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .padding(8.dp),
    ) {
        Text(
            text = "FARE",
            color = White,
            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
        )
        ViewSeparator(Modifier.fillMaxWidth())
        FareDetails(fareValue)
    }
}

@Composable
fun ExtrasGroup(modifier: Modifier, extrasValue: String) {
    Column(
        modifier =
            modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .padding(8.dp),
    ) {
        Text(
            text = "EXTRAS",
            color = White,
            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
        )
        ViewSeparator(Modifier.fillMaxWidth())
        ExtrasDetails(extrasValue)
    }
}

@Composable
fun ViewSeparator(modifier: Modifier) {
    Box(
        modifier =
            modifier
                .height(4.dp)
                .background(color = SuperLightGray),
    )
}

@Composable
fun ExtrasDetails(extrasValue: String) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(start = 6.dp, bottom = 8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(start = 8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.hkd_label),
                color = White,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "¢",
                color = White,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(160.dp),
        ) {
            Text(
                text = extrasValue,
                color = Red,
                fontSize = 78.sp,
                modifier =
                    Modifier
                        .weight(1f)
//                    .padding(top = (-30).dp)
                        .align(Alignment.CenterVertically),
            )
            // Image composable for the fare label
        }
    }
}

@Composable
fun FareDetails(fareValue: String) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 6.dp, bottom = 8.dp),
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(start = 8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.hkd_label),
                color = White,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "¢",
                color = White,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(160.dp),
        ) {
            val fareValueInt = fareValue.toDoubleOrNull()
            if (fareValueInt != null && fareValueInt > 0) {
                Text(
                    text = fareValue,
                    color = Red,
                    fontSize = 78.sp,
                    modifier =
                    Modifier
                        .weight(1f)
//                    .padding(top = (-30).dp)
                        .align(Alignment.CenterVertically),
                )
            }
        }
    }
}

@Composable
fun PriceSeparatedLine() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(color = LightGray),
    )
}

@Composable
fun MetricGroup(tripState: TripStateInMeterOpsUI, distance: String, time: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 8.dp),
    ) {
        MetricItem(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            label = "DIST. ( K M )",
            value = distance,
            valueColor = LightGray,
        )
        MetricItem(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            label = "TIME",
            value = time,
            valueColor = LightGray,
        )
        MeterGoButton(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(top = 12.dp, end = 8.dp, bottom = 8.dp)
                    .background(color = Color.Gray),
            tripState = tripState
        )
    }
}

@Composable
fun MetricItem(
    modifier: Modifier,
    label: String,
    value: String,
    valueColor: Color,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = label,
            color = LightGray,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        ViewSeparator(Modifier.fillMaxWidth())
        Text(
            text = value,
            color = valueColor,
            fontSize = 50.sp,
            modifier =
                Modifier
                    .fillMaxWidth()
//                .padding(top = (-35).dp)
                    .align(Alignment.End),
        )
    }
}

@Composable
fun MeterGoButton(modifier: Modifier, tripState: TripStateInMeterOpsUI) {
    Column(
        modifier = modifier,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
        ) {
            Text(
                text = "粵",
                color = White,
                fontSize = 32.sp,
                modifier =
                    Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically),
            )
            Column(
                modifier = Modifier.weight(2f),
            ) {
                Text(
                    text = tripState.toStringCN(),
                    color = White,
                    fontSize = 26.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Text(
                    text = tripState.toStringEN(),
                    color = White,
                    fontSize = 26.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}
