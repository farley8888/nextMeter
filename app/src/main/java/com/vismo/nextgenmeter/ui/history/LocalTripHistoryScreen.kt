package com.vismo.nextgenmeter.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vismo.nextgenmeter.model.TripData
import com.vismo.nextgenmeter.ui.theme.gold500
import com.vismo.nextgenmeter.ui.theme.pastelGreen200
import com.vismo.nextgenmeter.ui.theme.pastelGreen600
import com.vismo.nextgenmeter.util.GlobalUtils


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
                if (it.type == KeyDown && it.nativeKeyEvent.scanCode == 255 && !it.nativeKeyEvent.isLongPress) {
                    selectedTripId?.let { id ->
                        viewModel.printReceipt(trips.find { trip -> trip.tripId == id }!!)
                        true
                    } ?: false
                } else {
                    false
                }
            },
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        // Add header as a list item
        item {
            TripHeaderRow()
        }
        // Add trip items
        // if trips is empty, show a message
        if (trips.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暫無行程",
                        fontSize = 24.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(trips) { trip ->
                TripItemRow(
                    count = trips.indexOf(trip) + 1,
                    trip = trip,
                    isSelected = trip.tripId == selectedTripId,
                    onClick = { selectedTripId = trip.tripId }
                )
            }
        }
        item {
            TripFooterRow()
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
        val fontSize = 28.sp
        // except for the first column, all other columns have the same weight
        Text(
            text = "#",
            modifier = Modifier.weight(0.5f),
            fontSize = fontSize,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        val columnWeight = 1f
        listOf("日", "始", "達", "里數", "侯時", "車費").forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(columnWeight),
                fontSize = fontSize,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
        val columnWeightWide = 1.25f
        listOf("附加","總費").forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(columnWeightWide),
                fontSize = fontSize,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

// This suppose to be floating footer row, but now simply a placeholder
@Composable
fun TripFooterRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically // Ensure vertical alignment
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { /* TODO */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = gold500 // Set the button's color to gold
                ),
                modifier = Modifier
                    .height(40.dp)
                    .width(100.dp),
                contentPadding = PaddingValues(vertical = 0.dp, horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center // Center the content
                ) {
                    Text(
                        text = "打印",
                        color = Color.White,
                        fontSize = 24.sp // Increased font size
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp), // Match the button's height
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "現金",
                    color = Color.Red,
                    fontSize = 24.sp, // Adjusted font size if needed
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp), // Match the button's height
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "電子支付",
                    color = pastelGreen600,
                    fontSize = 24.sp, // Adjusted font size if needed
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        // Add a spacer to push the button to the right
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun TripItemRow(count: Int, trip: TripData, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) {
        if (trip.isDash) pastelGreen200.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else Color.Transparent
    val defaultColor = Color.White
    val textColor = if (trip.isDash) pastelGreen600 else Color.Red

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp)
            .clickable { onClick() }
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(0.dp)
    ) {
        val fontSize = 28.sp
        Text(
            text = count.toString(),
            modifier = Modifier.weight(0.5f),
            fontSize = fontSize,
            color = defaultColor,
            textAlign = TextAlign.Center
        )
        val columnWeight = 1f
        val columnWeightWide = 1.25f


        // date in dd/mm format
        Text(
            text = GlobalUtils.formatTimestamp(trip.startTime, showTime = false, showDate = true),
            modifier = Modifier.weight(columnWeight),
            fontSize = fontSize,
            color = defaultColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = GlobalUtils.formatTimestamp(trip.startTime),
            modifier = Modifier.weight(columnWeight),
            fontSize = fontSize,
            color = textColor,
            textAlign = TextAlign.Center
        )
        val isEndDateDifferentFromStart = trip.endTime != null && !GlobalUtils.isSameDay(trip.startTime, trip.endTime)
        Text(
            text = GlobalUtils.formatTimestamp(trip.pauseTime, showDate = isEndDateDifferentFromStart),
            modifier = Modifier.weight(columnWeight),
            fontSize = fontSize,
            color = textColor,
            textAlign = TextAlign.Center
        )
        // distance in km
        Text(
            text = (trip.distanceInMeter/1000).toString(),
            modifier = Modifier.weight(columnWeight),
            fontSize = fontSize,
            color = defaultColor,
            textAlign = TextAlign.Center
        )
        // waiting time in minutes
        // need to convert seconds to minutes in format of MM:SS
        Text(
            text = GlobalUtils.formatSecondsToHHMM(trip.waitDurationInSeconds),
            modifier = Modifier.weight(columnWeight),
            fontSize = fontSize,
            color = defaultColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = trip.fare.toString(),
            modifier = Modifier.weight(columnWeight),
            fontSize = fontSize,
            color = defaultColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = "+$"+trip.extra.toString(),
            modifier = Modifier.weight(columnWeightWide),
            fontSize = fontSize,
            color = defaultColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = "$"+trip.totalFare.toString(),
            modifier = Modifier.weight(columnWeightWide),
            fontSize = fontSize,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}


