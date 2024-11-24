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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vismo.nextgenmeter.ui.theme.Black
import com.vismo.nextgenmeter.ui.theme.nobel50
import com.vismo.nextgenmeter.ui.theme.nobel600
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

    Row (
        modifier = Modifier.fillMaxSize()
            .background(Black)
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
        .padding(start = 16.dp, end = 16.dp),
    ) {
        CustomButton(
            text = "清除資料",
            containerColor = valencia200,
            textColor = Black,
            LocalView.current) {
            viewModel.clearAllLocalTrips()
        }
        CustomButton(
            text = "打印記錄",
            containerColor = nobel600,
            textColor = nobel50,
            LocalView.current) {}
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
    viewModel: TripSummaryDashboardViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Black),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        ActionButtons(viewModel = viewModel)
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DataColumn(title = "", data = listOf("旗數", "里數", "候時", "金額", "附加費"), horizontalAlignment = Alignment.CenterHorizontally)
            DataColumn(title = "總數", data = listOf(allTripsSummary.totalTrips, allTripsSummary.totalDistanceInKM, allTripsSummary.totalWaitTime, allTripsSummary.totalFare, allTripsSummary.totalExtras))
            DataColumn(title = "現金", data = listOf(allTripsSummary.totalTrips, allTripsSummary.totalDistanceInKM, allTripsSummary.totalWaitTime, allTripsSummary.totalFare, allTripsSummary.totalExtras))
            DataColumn(title = "電子", data = listOf("0", "0", "0", "0", "$0.00"))
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
        Text(text = title, style = MaterialTheme.typography.headlineSmall.copy(
            textDecoration = TextDecoration.Underline
        ))

        Spacer(modifier = Modifier.height(4.dp))

        data.forEach { item ->
            Text(text = item, style = MaterialTheme.typography.titleLarge)
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