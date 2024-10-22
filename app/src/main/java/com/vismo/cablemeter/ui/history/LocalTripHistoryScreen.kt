package com.vismo.cablemeter.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vismo.cablemeter.model.TripData
import com.vismo.cablemeter.ui.theme.gold500
import com.vismo.cablemeter.ui.theme.pastelGreen200
import com.vismo.cablemeter.ui.theme.pastelGreen600
import com.vismo.cablemeter.util.GlobalUtils


@Composable
fun LocalTripHistoryScreen(viewModel: LocalTripHistoryViewModel) {
    val trips = viewModel.trips.collectAsState().value
    var selectedTripId by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent {
                if (it.type == KeyDown) {
                    selectedTripId?.let { id ->
                        viewModel.printReceipt(trips.find { trip -> trip.tripId == id }!!)
                        true
                    } ?: false
                } else {
                    false
                }
            },
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        // Add header as a list item
        item {
            TripHeaderRow()
        }
        // Add trip items
        items(trips) { trip ->
            TripItemRow(
                trip = trip,
                isSelected = trip.tripId == selectedTripId,
                onClick = { selectedTripId = trip.tripId }
            )
        }
    }

    // Request focus when the composable is first composed
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun TripHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(8.dp)
    ) {
        val columnWeight = 1f
        listOf("Start Time", "End Time", "Fare", "Extra", "Wait Time", "Trip Total").forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(columnWeight),
                fontSize = 18.sp,
                color = gold500,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TripItemRow(trip: TripData, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) {
        if (trip.isDash) pastelGreen200.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else Color.Transparent
    val textColor = if (trip.isDash) pastelGreen600 else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        val columnWeight = 1f
        Text(
            text = GlobalUtils.formatTimestampToTime(trip.startTime, showDate = true),
            modifier = Modifier.weight(columnWeight),
            fontSize = 18.sp,
            color = textColor,
            textAlign = TextAlign.Center
        )
        val isEndDateDifferentFromStart = trip.endTime != null && !GlobalUtils.isSameDay(trip.startTime, trip.endTime)
        Text(
            text = GlobalUtils.formatTimestampToTime(trip.pauseTime, showDate = isEndDateDifferentFromStart),
            modifier = Modifier.weight(columnWeight),
            fontSize = 18.sp,
            color = textColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = trip.fare.toString(),
            modifier = Modifier.weight(columnWeight),
            fontSize = 18.sp,
            color = textColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = trip.extra.toString(),
            modifier = Modifier.weight(columnWeight),
            fontSize = 18.sp,
            color = textColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = trip.waitDurationInSeconds.toString(),
            modifier = Modifier.weight(columnWeight),
            fontSize = 18.sp,
            color = textColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = trip.totalFare.toString(),
            modifier = Modifier.weight(columnWeight),
            fontSize = 18.sp,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}


