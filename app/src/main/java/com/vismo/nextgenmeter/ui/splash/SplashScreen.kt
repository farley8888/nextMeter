package com.vismo.nextgenmeter.ui.splash

import android.util.Log
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vismo.nextgenmeter.MainActivity
import com.vismo.nextgenmeter.ui.shared.CircleLoader
import com.vismo.nextgenmeter.ui.theme.Black
import com.vismo.nextgenmeter.ui.theme.gold700
import com.vismo.nextgenmeter.ui.theme.secondary700

@Composable
fun SplashScreen(
    viewModel: SplashScreenViewModel,
    navigateToPair: () -> Unit,
    navigateToMeterOps: () -> Unit,
    alwaysNavigateToPair: Boolean?
) {
    val isLoading = viewModel.isLoading.collectAsState().value
    val showLoginToggle = viewModel.showLoginToggle.collectAsState().value
    val showConnectionIconsToggle = viewModel.showConnectionIconsToggle.collectAsState().value
    Log.d("SplashScreen", "alwaysNavigateToPair: $alwaysNavigateToPair")

    Column (
        modifier = Modifier
            .fillMaxSize()
            .background(color = Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        // 2024 11 24 - VAN - removed the splash screen loading circle loader from here
        if ((!isLoading && showLoginToggle && showConnectionIconsToggle) || alwaysNavigateToPair == true) {
            navigateToPair()
        } else if(!isLoading) {
            navigateToMeterOps()
        }
    }
}