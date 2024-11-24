package com.vismo.nextgenmeter.ui.meter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.vismo.nextgenmeter.ui.meter.MeterOpsViewModel.Companion.TOTAL_LOCK_BEEP_COUNTER
import com.vismo.nextgenmeter.ui.shared.BlinkingVisibility
import com.vismo.nextgenmeter.ui.shared.GlobalSnackbarDelegate
import com.vismo.nextgenmeter.ui.shared.NxGnDialogContent
import com.vismo.nextgenmeter.ui.shared.NxGnMeterDialog
import com.vismo.nextgenmeter.ui.theme.Black
import com.vismo.nextgenmeter.ui.theme.Purple40
import com.vismo.nextgenmeter.ui.theme.Typography
import com.vismo.nextgenmeter.ui.theme.nobel500
import com.vismo.nextgenmeter.ui.theme.oswaldFontFamily
import com.vismo.nextgenmeter.ui.theme.red
import com.vismo.nextgenmeter.util.GlobalUtils.performVirtualTapFeedback
import kotlinx.coroutines.delay


@Composable
fun MeterOpsScreen(
    viewModel: MeterOpsViewModel,
    snackbarDelegate: GlobalSnackbarDelegate,
    navigateToDashBoard: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val localFocusManager = LocalFocusManager.current
    val uiState = viewModel.uiState.collectAsState().value
    val view = LocalView.current
    val meterLockState = viewModel.meterLockState.collectAsState().value
    val lockTitle = if (meterLockState is MeterLockAction.Lock && meterLockState.isAbnormalPulse) "異常脈衝" else if (meterLockState is MeterLockAction.Lock) "被鎖定" else ""
    val lockDialogShowState = remember { mutableStateOf(false) }
    val showSnackBar = viewModel.showSnackBarMessage.collectAsState().value

    if (showSnackBar != null) {
        snackbarDelegate.showSnackbar(showSnackBar.second,showSnackBar.first)
        viewModel.clearSnackBarMessage()
    }

    Column(
        modifier =
        Modifier
            .background(color = Black)
            .focusRequester(focusRequester)
            .focusable()
            .fillMaxSize()
            .onKeyEvent {
                if (it.type == KeyDown && meterLockState !is MeterLockAction.Lock && !viewModel.isTTSPlaying()) {
                    val code = it.nativeKeyEvent.scanCode
                    val repeatCount = it.nativeKeyEvent.repeatCount
                    val isLongPress = it.nativeKeyEvent.isLongPress
                    viewModel.handleKeyEvent(code, repeatCount, isLongPress)
                }
                true
            }
            .clickable {
                if (uiState.status == ForHire) {
                    localFocusManager.clearFocus(force = true) // Clear focus before navigating so that no key events are triggered
                    navigateToDashBoard()
                    performVirtualTapFeedback(view)
                }
            }
    ) {
        lockDialogShowState.value = lockTitle.isNotEmpty() && uiState.overSpeedDurationInSeconds < 10 // we only want to show the dialog once for the first 10 seconds
        NxGnMeterDialog(showDialog = lockDialogShowState, onDismiss = {}, content = { NxGnDialogContent(
            message = lockTitle,
        ) })
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
    val fareOrTotal = if (uiState.status == Paused) "TOTAL" else "FARE"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = extrasOrDetails,
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .weight(0.32f)
                .padding(start = 8.dp)
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

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .weight(.73f),
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
            .then(
                if (uiState.status == Paused) {
                    Modifier.border(1.dp, Color.White)
                } else {
                    Modifier
                }
            )
            .weight(1f),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Spacer(modifier = Modifier
            .height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Spacer(modifier = Modifier.
                weight(0.4f))
            Text(text = "H.K.$",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.weight(6.8f).padding(start = 8.dp)
            )
            Text(text = "¢",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.weight(3.2f)
            )
        }
        Spacer(modifier = Modifier
            .height(2.dp))
        if (uiState.status == Paused) {
            FareOrExtraRow(label = " FARE ", value = uiState.fare.substring(0, uiState.fare.length - 1), showZero = false, color = uiState.totalColor, modifier = Modifier.weight(1f))
            FareOrExtraRow(label = "EXTRA", value = uiState.extras, showZero = true, color = uiState.totalColor, modifier = Modifier.weight(1f).offset(y = (-8).dp))
        } else if (uiState.status == Hired || uiState.status == PastTrip) {
            FareOrExtraRow(label = "", value = uiState.extras, showZero = true, color = uiState.totalColor, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun FareOrExtraRow(label: String, value: String, showZero: Boolean, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        val valueDouble = value.toDoubleOrNull()
        if (valueDouble != null && valueDouble > 0 || showZero) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(0.dp)
                    .height(120.dp)
            ) {
                Text(
                    text = value,
                    color = color,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = oswaldFontFamily,
                    letterSpacing = TextUnit(-5.2F, TextUnitType.Sp), //squeeze the text together
                    textAlign = TextAlign.End,
                    lineHeight = (80.sp) * 1.2,
                    style = TextStyle(
                        lineHeight = (80.sp) * 1.2,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                    overflow = TextOverflow.Visible,
                    modifier = Modifier
                        .wrapContentWidth()
                        .offset(x = 14.dp, y = (-20).dp)
                        .scale(scaleX = 1.0f, scaleY = 1.06f)
                )
            }
        }
        Box(modifier = Modifier
            .align(Alignment.CenterVertically)
            .wrapContentSize()
            .wrapContentHeight()
            .padding(0.dp)
        ) {
            Text(
                text = if (label.isBlank()) "-----" else label,
                fontWeight = FontWeight.Bold,
                color = if (label.isBlank()) Color.Transparent else Color.Yellow,
                maxLines = 1,
                modifier = Modifier
                    .rotate(270f) // Orienting the text vertically
                    .align(Alignment.BottomStart)
                    .padding(0.dp)
                    .offset(y = 8.dp)
            )
        }
    }
}


@Composable
fun RowScope.TotalBox(uiState: MeterOpsUiData) {
    Column(
        modifier = Modifier
            .weight(2.2f)
            .padding(8.dp)
            .height(height = 250.dp) // Set a constant height
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(.2f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(text = "H.K.$",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.weight(7.5f)
            )
            Text(text = "¢",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.weight(3.2f)
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
            val numberToShow = if(uiState.status == Hired || uiState.status == PastTrip) uiState.fare else if (uiState.status == Paused) uiState.totalFare else ""
            val startingPrice = if(uiState.mcuStartingPrice.isNotEmpty()) "c${uiState.mcuStartingPrice}" else "27.00"
            val totalFare = if(uiState.remainingOverSpeedTimeInSeconds != null && uiState.overSpeedDurationInSeconds > TOTAL_LOCK_BEEP_COUNTER) "c${uiState.mcuStartingPrice}" else numberToShow
            val totalFareDouble = uiState.totalFare.toDoubleOrNull()
            var isVisible by remember { mutableStateOf(true) }
            val isBlinking = uiState.overSpeedDurationInSeconds in 1..TOTAL_LOCK_BEEP_COUNTER
            LaunchedEffect(isBlinking) {
                if (isBlinking) {
                    while (true) {
                        isVisible = true
                        delay(500)
                        isVisible = false
                        delay(500)
                    }
                } else {
                    isVisible = true
                }
            }
            if (totalFareDouble != null && totalFareDouble > 0) { // So that we don't show 0 as the total fare
                BlinkingVisibility(isVisible = isVisible || !isBlinking) { // blink for the first 30 seconds of lock state
                    Text(
                        text = totalFare,
                        color = uiState.totalColor,
                        style = Typography.displayLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 148.sp,
                            letterSpacing = TextUnit(-6.5F, TextUnitType.Sp), //squeeze the text together
                            lineHeight = 148.sp
                        ),
                        overflow = TextOverflow.Visible,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .offset(y = (-16).dp)
                    )
                }
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
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White))
                Text(
                    text = if(uiState.remainingOverSpeedTimeInSeconds != null) "0.0" else uiState.distanceInKM,
                    color = Color.Gray,
                    style = Typography.displayLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 48.sp,
                        letterSpacing = TextUnit(-2F, TextUnitType.Sp), //squeeze the text together
                        lineHeight = 48.sp
                    ),
                    textAlign = TextAlign.End,
                    modifier = Modifier.align(Alignment.End),
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
                    style = Typography.displayLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 48.sp,
                        letterSpacing = TextUnit(-2F, TextUnitType.Sp), //squeeze the text together
                        lineHeight = 48.sp
                    ),
                    textAlign = TextAlign.End,
                    modifier = Modifier.align(Alignment.End)
                )
        }
        val buttonContainerColor = if(uiState.status is Paused) red else if(uiState.status is Hired) Purple40 else nobel500
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
            colors = ButtonDefaults.buttonColors(containerColor = buttonContainerColor),
            contentPadding = PaddingValues(top = 2.dp, bottom = 2.dp, start = 8.dp, end = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.languagePref.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 34.sp,
                    lineHeight = 34.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.wrapContentWidth(Alignment.Start).padding(start = 16.dp)
                )

                Spacer(modifier = Modifier.weight(1f))  // Space to push the second Text to the right

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.wrapContentWidth(Alignment.End).padding(end = 16.dp)
                ) {
                    Text(
                        text = uiState.status.toStringCN(),
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 27.sp,
                        lineHeight = 27.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = uiState.status.toStringEN(),
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 27.sp,
                        lineHeight = 27.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
