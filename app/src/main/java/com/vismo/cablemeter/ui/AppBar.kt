package com.vismo.cablemeter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.SignalCellularAlt1Bar
import androidx.compose.material.icons.outlined.SignalCellularAlt2Bar
import androidx.compose.material.icons.outlined.SignalCellularNodata
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vismo.cablemeter.MainViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    viewModel: MainViewModel,
    onBackButtonClick: () -> Unit
) {
    val uiState = viewModel.topAppBarUiState.collectAsState().value
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.height(48.dp),
        title = {},
        navigationIcon = {
            Row (
                modifier = Modifier.fillMaxHeight()
                    .wrapContentWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (uiState.isBackButtonVisible) {
                    IconButton(onClick = onBackButtonClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                }
                Text(
                    text = uiState.dateTime,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 4.dp, end = 16.dp).wrapContentSize(align = Alignment.Center)
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .border(
                            BorderStroke(1.dp, Color.White),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .wrapContentSize(align = Alignment.Center)
                ) {
                    Text(
                        text = uiState.envVariable,
                        color = Color.White,
                        fontSize = 16.sp,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.wrapContentSize(align = Alignment.Center)
                    )
                }
            }
        },
        actions = {
            Row (
                modifier = Modifier.fillMaxHeight()
                    .wrapContentWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (uiState.driverPhoneNumber.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "User Icon",
                        modifier = Modifier.wrapContentSize(align = Alignment.Center)
                            .padding(end = 2.dp)
                    )
                    Text(
                        text = uiState.driverPhoneNumber,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.wrapContentSize(align = Alignment.Center)
                            .padding(end = 16.dp)
                    )
                }
                if (uiState.isLocationIconVisible) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "Location",
                        modifier = Modifier.wrapContentSize(align = Alignment.Center)
                            .padding(end = 4.dp)
                    )
                }
                if (uiState.isWifiIconVisible) {
                    Icon(
                        imageVector = Icons.Outlined.Wifi,
                        contentDescription = "Wifi",
                        modifier = Modifier.wrapContentSize(align = Alignment.Center)
                            .padding(end = 4.dp)
                    )
                }
                when (uiState.signalStrength) {
                    1 -> {
                        Icon(
                            imageVector = Icons.Outlined.SignalCellularAlt1Bar,
                            contentDescription = "Low Signal",
                            modifier = Modifier.wrapContentSize(align = Alignment.Center)
                                .padding(end = 4.dp)
                        )
                    }

                    2 -> {
                        Icon(
                            imageVector = Icons.Outlined.SignalCellularAlt2Bar,
                            contentDescription = "Low Signal",
                            modifier = Modifier.wrapContentSize(align = Alignment.Center)
                                .padding(end = 4.dp)
                        )
                    }

                    3, 4 -> {
                        Icon(
                            imageVector = Icons.Outlined.SignalCellularAlt,
                            contentDescription = "High Signal",
                            modifier = Modifier.wrapContentSize(align = Alignment.Center)
                                .padding(end = 4.dp)
                        )
                    }

                    else -> {
                        Icon(
                            imageVector = Icons.Outlined.SignalCellularNodata,
                            contentDescription = "No Signal",
                            modifier = Modifier.wrapContentSize(align = Alignment.Center)
                                .padding(end = 4.dp)
                        )
                    }
                }
            }
        },
    )
    // Overlay a centered title
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp), // Match the TopAppBar height
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = uiState.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}