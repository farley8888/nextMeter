package com.vismo.cablemeter.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.unit.dp
import com.vismo.cablemeter.MainViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(viewModel: MainViewModel) {
    val uiState = viewModel.topAppBarUiState.collectAsState().value

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.height(48.dp),
        title = {},
        navigationIcon = {
            IconButton({}) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                )
            }
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Lock",
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = "GPS",
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "User",
                )
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
            text = "SUNTEC",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}