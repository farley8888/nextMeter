package com.vismo.nextgenmeter.ui.meter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TaxiAlert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vismo.nextgenmeter.ui.shared.GenericActionDialogContent
import com.vismo.nextgenmeter.ui.shared.GlobalDialog
import com.vismo.nextgenmeter.ui.theme.Black
import com.vismo.nextgenmeter.ui.theme.Purple40
import com.vismo.nextgenmeter.ui.theme.Typography
import com.vismo.nextgenmeter.ui.theme.red
import com.vismo.nextgenmeter.ui.theme.valencia100
import com.vismo.nextgenmeter.util.GlobalUtils.performVirtualTapFeedback
import com.vismo.nextgenmeter.util.GlobalUtils.toDoubleOrZero


@Composable
fun MeterOpsScreen(
    viewModel: MeterOpsViewModel,
    navigateToDashBoard: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val uiState = viewModel.uiState.collectAsState().value
    val view = LocalView.current
    val meterLockState = viewModel.meterLockState.collectAsState().value
    val lockTitle = if (meterLockState is MeterLockAction.Lock && meterLockState.isAbnormalPulse) "Abnormal Pulse" else if (meterLockState is MeterLockAction.Lock) "Over-Speed" else ""
    val lockMessage = if (meterLockState is MeterLockAction.Lock && meterLockState.isAbnormalPulse) "Please check the pulse sensor" else if (meterLockState is MeterLockAction.Lock) "Please drive safely" else ""
    val lockDialogShowState = remember { mutableStateOf(false) }

    Column(
        modifier =
        Modifier
            .background(color = Black)
            .focusRequester(focusRequester)
            .focusable()
            .fillMaxSize()
            .onKeyEvent {
                if (it.type == KeyDown && meterLockState !is MeterLockAction.Lock) {
                    val code = it.nativeKeyEvent.scanCode
                    val repeatCount = it.nativeKeyEvent.repeatCount
                    val isLongPress = it.nativeKeyEvent.isLongPress
                    viewModel.handleKeyEvent(code, repeatCount, isLongPress)
                }
                true
            }
            .clickable {
                if (uiState.status == ForHire) {
                    navigateToDashBoard()
                    performVirtualTapFeedback(view)
                }
            }
    ) {
        lockDialogShowState.value = lockTitle.isNotEmpty() && uiState.remainingOverSpeedTimeInSeconds == null // cause this becomes non null 30 seconds after. and we only want to show the dialog once
        GlobalDialog(
            onDismiss = {},
            showDialog = lockDialogShowState,
            isBlinking = false,
            content = {
                GenericActionDialogContent(
                    title = lockTitle,
                    message = lockMessage,
                    actions = emptyList(),
                    iconResId = Icons.Rounded.TaxiAlert,
                    backgroundColor = valencia100,
                    dismissDialog = { lockDialogShowState.value = false }
                )
            }
        )
        TaxiMeterUI(uiState, meterLockState, viewModel)
    }

    // Request focus when the composable is first composed
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

// Show Extras & Fare header
@Composable
fun ExtrasAndFareHeader(uiState: MeterOpsUiData) {
    val extrasOrDetails = if (uiState.status == Paused) "DETAILS" else "EXTRAS"
    val fareOrTotal = if (uiState.status == Paused) "FARE" else "TOTAL"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = extrasOrDetails,
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .weight(0.3f)
        )
        Text(
            text = fareOrTotal,
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.weight(0.7f)
        )
    }
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White)
    )
}

@Composable
fun ColumnScope.TaxiMeterUI(uiState: MeterOpsUiData, meterLockState: MeterLockAction, viewModel: MeterOpsViewModel) {
    // Extras And Fare Header
    ExtrasAndFareHeader(uiState)

    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.weight(2f)
    ) {
        DetailsBox(uiState)
        TotalBox(uiState)
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .weight(1f),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        DistanceTimeAndStatusBox(uiState, meterLockState = meterLockState, viewModel = viewModel)
    }
}

@Composable
fun RowScope.DetailsBox(uiState: MeterOpsUiData) {
    Column(
        modifier = Modifier
            // .size(height = 220.dp, width = 180.dp) // Set a constant height and width
            .then(
                if (uiState.status == Paused) {
                    Modifier.border(1.dp, Color.White)
                } else {
                    Modifier
                }
            )
            .weight(1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top

        ) {
            Text(text = "H.K.$",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Text(text = "¢",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
        }
        if (uiState.status == Paused) {
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .weight(2.5f),
//                verticalAlignment = Alignment.Bottom,
//                horizontalArrangement = Arrangement.End
//            ) {
//                val fare = uiState.fare
//                val fareDouble = fare.toDoubleOrNull()
//                if (fareDouble != null && fareDouble > 0) {
//                    Text(
//                        text = fare.substring(0, fare.length - 1), // remove 1 zero
//                        color = uiState.totalColor,
//                        style = Typography.displayMedium.copy(
//                            fontSize = 62.sp
//                        ),
//                        textAlign = TextAlign.End,
//                        modifier = Modifier.align(Alignment.CenterVertically)
//                    )
//                }
//                Text(
//                    text = "FARE",
//                    color = Color.Yellow,
//                    fontWeight = FontWeight.Bold,
//                    modifier = Modifier
//                        .rotate(270f) // Orienting the text vertically
//                        .offset(x = 6.dp) // Move the text down
//                        .offset(y = 5.dp) // Move the text right
//                        .align(Alignment.CenterVertically)
//                )
//            }
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .weight(2.5f),
//                verticalAlignment = Alignment.Bottom,
//                horizontalArrangement = Arrangement.End
//            ) {
//                val fare = uiState.fare
//                val fareDouble = fare.toDoubleOrNull()
//                val extras = uiState.extras
//                val extrasDouble = extras.toDoubleOrNull()
//                if (extrasDouble != null && fareDouble != null && fareDouble > 0) {
//                    Text(
//                        text = extras,
//                        color = uiState.totalColor,
//                        style = Typography.displayMedium.copy(
//                            fontSize = 62.sp
//                        ),
//                        textAlign = TextAlign.End,
//                        modifier = Modifier.align(Alignment.CenterVertically)
//                    )
//                }
//                Text(
//                    text = "EXTRA",
//                    color = Color.Yellow,
//                    fontWeight = FontWeight.Bold,
//                    modifier = Modifier
//                        .rotate(270f) // Orienting the text vertically
//                        .offset(x = 6.dp) // Move the text down
//                        .offset(y = 5.dp) // Move the text right
//                        .align(Alignment.CenterVertically)
//                )
//            }
            FareOrExtraRow(label = "FARE", value = uiState.fare.substring(0, uiState.fare.length - 1), color = uiState.totalColor)
            FareOrExtraRow(label = "EXTRA", value = uiState.extras, color = uiState.totalColor)
        } else if (uiState.status == Hired) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2.5f),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.End
            ) {
                val extras = uiState.extras
                val extrasDouble = extras.toDoubleOrNull()
                if (extrasDouble != null && extrasDouble > 0) {
                    Text(
                        text = extrasDouble.toString(),
                        color = uiState.totalColor,
                        style = Typography.displayMedium.copy(
                            fontSize = 62.sp
                        ),
                        textAlign = TextAlign.End,
                        modifier = Modifier.align(Alignment.Top)
                    )
                }
            }
        }
    }
}

@Composable
fun FareOrExtraRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.End
    ) {

        val valueDouble = value.toDoubleOrNull()
        if (valueDouble != null && valueDouble > 0) {
            Text(
                text = value,
                color = color,
                style = Typography.displayMedium.copy(
                    fontSize = 65.sp
                ),
                textAlign = TextAlign.End,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
        Box(modifier = Modifier
            .weight(1f)
            .align(Alignment.CenterVertically)
        ) {
            Text(
                text = label,
                color = Color.Yellow,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .rotate(270f) // Orienting the text vertically
                    .offset(x = 1.dp) // Move the text down
                    .offset(y = 1.dp) // Move the text right
                    .align(Alignment.Center)
            )
        }
    }
}


@Composable
fun RowScope.TotalBox(uiState: MeterOpsUiData) {
    Column(
        modifier = Modifier
            .weight(2.4f)
            .padding(8.dp)
            .height(height = 250.dp) // Set a constant height
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(text = "H.K.$",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Text(text = "¢",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
        }
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.End
        ){
            // if status = hired, show fare
            // if status = paused, show total
            val numberToShow = if(uiState.status == Hired) uiState.fare else if (uiState.status == Paused) uiState.totalFare else ""
            val totalFare = if(uiState.remainingOverSpeedTimeInSeconds != null) "c${numberToShow}" else numberToShow
            val totalFareDouble = uiState.totalFare.toDoubleOrNull()
            if (totalFareDouble != null && totalFareDouble > 0) { // So that we don't show 0 as the total fare
                Text(
                    text = totalFare,
                    color = uiState.totalColor,
                    style = Typography.displayLarge.copy(
                        fontSize = 140.sp
                    ),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
fun RowScope.DistanceTimeAndStatusBox(uiState: MeterOpsUiData, meterLockState: MeterLockAction, viewModel: MeterOpsViewModel) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                Text(
                    text = "DIST. (KM)",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White))
                Text(
                    text = if(uiState.remainingOverSpeedTimeInSeconds != null) "c0.0" else uiState.distanceInKM,
                    color = Color.Gray,
                    style = Typography.displaySmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.align(Alignment.End)
                )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                Text(
                    text = "TIME",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White)
                )
                Text(
                    text = uiState.remainingOverSpeedTimeInSeconds ?: uiState.duration,
                    color = Color.Gray,
                    style = Typography.displaySmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.align(Alignment.End)
                )
        }
        val buttonContainerColor = if(uiState.status is Paused) red else Purple40
        Button(
            onClick = {
                if(meterLockState !is MeterLockAction.Lock) {
                    viewModel.toggleLanguagePref()
                }
            },
            modifier = Modifier
                .weight(1f)
                .padding(2.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonContainerColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.languagePref.toString(),
                    color = Color.White,
                    style = Typography.bodyLarge,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.wrapContentWidth(Alignment.Start)
                )

                Spacer(modifier = Modifier.weight(1f))  // Space to push the second Text to the right

                Text(
                    text = "${uiState.status.toStringCN()} ${uiState.status.toStringEN()}",
                    color = Color.White,
                    style = Typography.headlineSmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.wrapContentWidth(Alignment.End)
                )
            }
        }
    }
}
