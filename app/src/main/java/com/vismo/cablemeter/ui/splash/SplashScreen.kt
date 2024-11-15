package com.vismo.cablemeter.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vismo.cablemeter.ui.shared.CircleLoader
import com.vismo.cablemeter.ui.theme.Black
import com.vismo.cablemeter.ui.theme.gold700
import com.vismo.cablemeter.ui.theme.secondary700

@Composable
fun SplashScreen(
    viewModel: SplashScreenViewModel,
    navigateToPair: () -> Unit,
    navigateToMeterOps: () -> Unit,
) {
    val isLoading = viewModel.isLoading.collectAsState().value
    val showLoginToggle = viewModel.showLoginToggle.collectAsState().value

    Column (
        modifier = Modifier
            .fillMaxSize()
            .background(color = Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        CircleLoader(
            color = secondary700,
            secondColor = gold700,
            modifier = Modifier.size(100.dp),
            isVisible = isLoading
        )
        if (!isLoading && showLoginToggle) {
            navigateToPair()
        } else if(!isLoading) {
            navigateToMeterOps()
        }
    }
}