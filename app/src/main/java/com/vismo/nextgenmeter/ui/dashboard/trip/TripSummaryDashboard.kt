package com.vismo.nextgenmeter.ui.dashboard.trip

import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.vismo.nextgenmeter.ui.shared.GenericDialogContent
import com.vismo.nextgenmeter.ui.shared.GlobalDialog
import com.vismo.nextgenmeter.ui.shared.GlobalToast
import com.vismo.nextgenmeter.ui.theme.Black
import com.vismo.nextgenmeter.ui.theme.mineShaft600
import com.vismo.nextgenmeter.ui.theme.Typography
import com.vismo.nextgenmeter.ui.theme.nobel100
import com.vismo.nextgenmeter.ui.theme.nobel50
import com.vismo.nextgenmeter.ui.theme.nobel600
import com.vismo.nextgenmeter.ui.theme.nobel800
import com.vismo.nextgenmeter.ui.theme.pastelGreen400
import com.vismo.nextgenmeter.ui.theme.secondary500
import com.vismo.nextgenmeter.ui.theme.valencia200
import com.vismo.nextgenmeter.util.GlobalUtils.performVirtualTapFeedback

@Composable
fun TripSummaryDashboard(
    viewModel: TripSummaryDashboardViewModel,
    navigateToTripHistory: () -> Unit,
    navigateToAdjustBrightnessOrVolume: () -> Unit,
    navigateToMCUSummary: () -> Unit
) {
    val allTripsSummary = viewModel.allTripSummary.collectAsState().value

    val showDialogClearAllLocalTrips = remember { mutableStateOf(false) }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        Row() {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Options(navigateToTripHistory, navigateToAdjustBrightnessOrVolume, navigateToMCUSummary)
            }
            Column(
                modifier = Modifier
                    .weight(1.8f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TripSummary(
                    allTripsSummary = allTripsSummary,
                    viewModel = viewModel,
                    onClickBtnClearAllLocalTrips = {
                        showDialogClearAllLocalTrips.value = true
                    },
                    onClickBtnPrintRecord = {},
                )
            }
        }

        Dialog(showDialog = showDialogClearAllLocalTrips) {
            GenericDialogContent(
                title = "清除行程資料",
                message = "確定清除所有行程資料？",
                confirmButtonText = "確認",
                onConfirm = {
                    viewModel.clearAllLocalTrips()
                    GlobalToast.show("所有行程資料已清除")
                },
                cancelButtonText = "取消",
                onCancel = {
                    showDialogClearAllLocalTrips.value = false
                },
                onDismiss = {
                    showDialogClearAllLocalTrips.value = false
                }
            )
        }
    }
}


@Composable
fun Dialog(showDialog: MutableState<Boolean>, dialogContent: @Composable () -> Unit) {
    GlobalDialog(
        onDismiss = {},
        showDialog = showDialog,
        isBlinking = false,
        content = dialogContent,
    )
}


@Composable
fun ActionButtons(onClickBtnClearAllLocalTrips: () -> Unit, onClickBtnPrintRecord: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 16.dp),
    ) {
        CustomButton(
            text = "清除資料",
            containerColor = valencia200,
            textColor = Black,
            LocalView.current) {
            onClickBtnClearAllLocalTrips()
        }
        CustomButton(
            text = "打印記錄",
            containerColor = nobel600,
            textColor = nobel50,
            LocalView.current) {
            onClickBtnPrintRecord()
        }
    }
}

@Composable 
fun RowScope.CustomButton(text: String, containerColor: Color, textColor: Color, view: View, onClick: () -> Unit) {
    Button(
        onClick = {
            onClick()
            performVirtualTapFeedback(view)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = nobel50
        ),
        modifier = Modifier
            .height(100.dp)
            .weight(1f)
            .padding(16.dp)
    ) {
        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ){
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.headlineSmall
            )
        }

    }
}

@Composable
fun ColumnScope.Options(
    navigateToTripHistory: () -> Unit,
    navigateToAdjustBrightnessOrVolume: () -> Unit,
    navigateToMCUSummary: () -> Unit
) {
    CustomListItem(text = "本更行程數據", LocalView.current, navigateToTripHistory, secondary500)
    CustomListItem(text = "系統設定", LocalView.current, navigateToAdjustBrightnessOrVolume, pastelGreen400)
    CustomListItem(text = "系統資料", LocalView.current, navigateToMCUSummary, pastelGreen400)
}

@Composable
fun TripSummary(
    allTripsSummary: TripSummaryDashboardUiData,
    viewModel: TripSummaryDashboardViewModel,
    onClickBtnClearAllLocalTrips: () -> Unit,
    onClickBtnPrintRecord: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Black),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        ActionButtons(
            onClickBtnClearAllLocalTrips = onClickBtnClearAllLocalTrips,
            onClickBtnPrintRecord = onClickBtnPrintRecord,
        )
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DataColumn(title = "", data = listOf("旗數", "里數", "候時", "金額", "附加費"), horizontalAlignment = Alignment.CenterHorizontally)
            DataColumn(title = "總數", data = listOf(allTripsSummary.totalTrips, allTripsSummary.totalDistanceInKM, allTripsSummary.totalWaitTime, allTripsSummary.totalFare, allTripsSummary.totalExtras))
            DataColumn(title = "現金", data = listOf(allTripsSummary.totalTrips, allTripsSummary.totalDistanceInKM, allTripsSummary.totalWaitTime, allTripsSummary.totalFare, allTripsSummary.totalExtras), textColor = mineShaft600)
            DataColumn(title = "電子", data = listOf("0", "0.0", "00:00:00", "$0.0", "$0.0"), textColor = mineShaft600)
        }
    }
}

@Composable
fun RowScope.DataColumn(
    title: String, data: List<String>,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    textColor: Color = Color.White
) {
    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier.weight(1f)
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall.copy(
            textDecoration = TextDecoration.Underline
        ), color = textColor)

        Spacer(modifier = Modifier.height(4.dp))

        data.forEach { item ->
            Text(text = item, style = MaterialTheme.typography.titleLarge, color = textColor)
        }
    }
}

@Composable
fun ColumnScope.CustomListItem(text: String, view: View, onClick: () -> Unit, color10percent: Color) {
    // Define a vertical gradient with color stops at specific positions
    val gradient = Brush.verticalGradient(
        0f to color10percent,   // Start at 0%, color is Blue
        0.15f to color10percent, // At 15%, color is still Blue
        0.15f to nobel600, // From 15%, color changes to Gray
        1f to nobel600    // At 100%, color is Gray
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)  // Makes each item take up an equal share of the column's height
            .clickable {
                onClick()
                performVirtualTapFeedback(view = view)
            }
            .padding(16.dp)
            // Apply the gradient background with rounded corners
            .background(brush = gradient, shape = RoundedCornerShape(8.dp))
            .border(
                BorderStroke(2.dp, nobel600),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
    }
}