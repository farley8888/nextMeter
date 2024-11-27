package com.vismo.nextgenmeter.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vismo.nextgenmeter.ui.theme.Typography
import com.vismo.nextgenmeter.util.GlobalUtils

@Composable
fun UpdateScreen(
    viewModel: UpdateViewModel
) {
    val updateState by viewModel.updateState.collectAsState()
    val updateDetails by viewModel.updateDetails.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            // details of the update
            Text("Version: ${updateDetails?.version}", style = Typography.headlineMedium)
            Text("Description: ${updateDetails?.description}", style = Typography.headlineMedium)
            Text("Must Update Before:" + GlobalUtils.formatTimestamp(updateDetails?.mustUpdateBefore, showTime = false, showDate = true), style = Typography.headlineMedium)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (updateState) {
                is UpdateState.Idle -> {
                    Text("Checking for Updates", style = Typography.headlineMedium)
                }
                is UpdateState.Downloading -> {
                    // Downloading UI
                    val progress = (updateState as UpdateState.Downloading).progress
                    Text("Downloading: $progress%", style = Typography.headlineMedium)

                }
                is UpdateState.Installing -> {
                    Text(text = "Installing", style = Typography.headlineMedium)
                }
                is UpdateState.Success -> {
                    Text(text = "Update Successful. Restating the app. Please wait...", style = Typography.headlineMedium, color = androidx.compose.ui.graphics.Color.Green)
                }
                is UpdateState.Error -> {
                    val error = (updateState as UpdateState.Error).message
                    Text("Error: $error", style = Typography.headlineMedium, color = androidx.compose.ui.graphics.Color.Red)
                }

                is UpdateState.NoUpdateFound -> {
                    Text("No Update Found", style = Typography.headlineMedium)
                    // remove this screen from the backstack
                }
            }
        }
    }
}