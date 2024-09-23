package com.vismo.cablemeter.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vismo.cablemeter.model.TripData
import com.vismo.cablemeter.ui.theme.gold500
import com.vismo.cablemeter.util.GlobalUtils

@Composable
fun TripHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "Trip ID", modifier = Modifier.weight(1f), fontSize = 14.sp, color = gold500)
        Text(text = "Start Time", modifier = Modifier.weight(1f), fontSize = 14.sp, color = gold500)
        Text(text = "End Time", modifier = Modifier.weight(1f), fontSize = 14.sp, color = gold500)
        Text(text = "Fare", modifier = Modifier.weight(1f), fontSize = 14.sp, color = gold500)
        Text(text = "Extra", modifier = Modifier.weight(1f), fontSize = 14.sp, color = gold500)
        Text(text = "Wait Time", modifier = Modifier.weight(1f), fontSize = 14.sp, color = gold500)
        Text(text = "Trip Total", modifier = Modifier.weight(1f), fontSize = 14.sp, color = gold500)
    }
}


@Composable
fun LocalTripHistoryScreen(viewModel: LocalTripHistoryViewModel) {
    val trips = viewModel.trips.collectAsState().value

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        TripHeader()
        TripList(trips = trips)
    }
}

@Composable
fun TripList(trips: List<TripData>) {
    var selectedTripId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(trips) { trip ->
            TripItem(
                trip = trip,
                isSelected = trip.tripId == selectedTripId,
                onClick = {
                    selectedTripId = trip.tripId
                }
            )
        }
    }
}

@Composable
fun TripItem(trip: TripData, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = trip.tripId.substring(0, 5), modifier = Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(text = GlobalUtils.formatTimestampToTime(trip.startTime), modifier = Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(text = GlobalUtils.formatTimestampToTime(trip.endTime), modifier = Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(text = trip.fare.toString(), modifier = Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(text = trip.extra.toString(), modifier = Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(text = trip.waitDurationInSeconds.toString(), modifier = Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(text = trip.totalFare.toString(), modifier = Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}