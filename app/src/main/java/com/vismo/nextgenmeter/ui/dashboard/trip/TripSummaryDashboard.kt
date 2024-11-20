package com.vismo.nextgenmeter.ui.dashboard.trip

import android.view.View
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vismo.nextgenmeter.ui.theme.nobel400
import com.vismo.nextgenmeter.ui.theme.nobel50
import com.vismo.nextgenmeter.ui.theme.nobel500
import com.vismo.nextgenmeter.ui.theme.nobel800
import com.vismo.nextgenmeter.ui.theme.valencia300
import com.vismo.nextgenmeter.util.GlobalUtils.performVirtualTapFeedback

@Composable
fun TripSummaryDashboard(
    viewModel: TripSummaryDashboardViewModel,
    navigateToTripHistory: () -> Unit,
    navigateToAdjustBrightnessOrVolume: () -> Unit,
    navigateToMCUSummary: () -> Unit
) {
    val allTripsSummary = viewModel.allTripSummary.collectAsState().value

    Row (
        modifier = Modifier.fillMaxSize()
    ) {
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
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun ActionButtons(viewModel: TripSummaryDashboardViewModel) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
        CustomButton(
            text = "清除\n" + "資料",
            containerColor = valencia300,
            LocalView.current) {
            viewModel.clearAllLocalTrips()
        }
        CustomButton(
            text = "打印\n" + "記錄",
            containerColor = nobel400,
            LocalView.current) {}
    }
}

@Composable 
fun RowScope.CustomButton(text: String, containerColor: Color, view: View, onClick: () -> Unit) {
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
                fontSize = 18.sp,
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
    CustomListItem(text = "本更行程數據", LocalView.current, navigateToTripHistory)
    CustomListItem(text = "系統設定", LocalView.current, navigateToAdjustBrightnessOrVolume)
    CustomListItem(text = "系統資料", LocalView.current, navigateToMCUSummary)
}

@Composable
fun TripSummary(
    allTripsSummary: TripSummaryDashboardUiData,
    viewModel: TripSummaryDashboardViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = nobel800),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        ActionButtons(viewModel = viewModel)
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DataColumn(title = "", data = listOf("旗數", "里數", "候時", "金額"), horizontalAlignment = Alignment.CenterHorizontally)
            DataColumn(title = "總數", data = listOf(allTripsSummary.totalTrips, allTripsSummary.totalDistanceInKM, allTripsSummary.totalWaitTime, allTripsSummary.totalFare))
            DataColumn(title = "現金", data = listOf(allTripsSummary.totalTrips, allTripsSummary.totalDistanceInKM, allTripsSummary.totalWaitTime, allTripsSummary.totalFare))
            DataColumn(title = "電子", data = listOf("0", "0", "0", "$0.00"))
        }
    }
}

@Composable
fun RowScope.DataColumn(
    title: String, data: List<String>,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier.weight(1f)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(8.dp))

        data.forEach { item ->
            Text(text = item, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun ColumnScope.CustomListItem(text: String, view: View, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)  // Makes each item take up an equal share of the column's height
            .clickable {
                onClick()
                performVirtualTapFeedback(view = view)
            }
            .padding(16.dp)
            .border(
                BorderStroke(2.dp, nobel500),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
    }
}