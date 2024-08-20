package com.vismo.cablemeter.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.vismo.cablemeter.MainActivity.Companion.NavigationDestination
import com.vismo.cablemeter.ui.dashboard.TripSummaryDashBoard
import com.vismo.cablemeter.ui.dashboard.TripSummaryDashBoardViewModel
import com.vismo.cablemeter.ui.history.LocalTripHistoryScreen
import com.vismo.cablemeter.ui.history.LocalTripHistoryViewModel
import com.vismo.cablemeter.ui.meter.MeterOpsScreen
import com.vismo.cablemeter.ui.meter.MeterOpsViewModel
import com.vismo.cablemeter.ui.pair.DriverPairScreen
import com.vismo.cablemeter.ui.pair.DriverPairViewModel

@Composable
fun NavigationGraph(navController: NavHostController, innerPadding: PaddingValues) {
    NavHost(
        navController = navController,
        startDestination = NavigationDestination.Pair.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable(NavigationDestination.MeterOps.route) {
            val viewModel = hiltViewModel<MeterOpsViewModel>()
            MeterOpsScreen(viewModel, navigateToDashBoard = {
                navController.navigate(NavigationDestination.Dashboard.route)
            })
        }
        composable(NavigationDestination.Pair.route) {
            val viewModel = hiltViewModel<DriverPairViewModel>()
            DriverPairScreen(viewModel, navigateToMeterOps = {
                navController.navigate(NavigationDestination.MeterOps.route)
            })
        }
        composable(NavigationDestination.TripHistory.route) {
            val viewModel = hiltViewModel<LocalTripHistoryViewModel>()
            LocalTripHistoryScreen(viewModel)
        }
        composable(NavigationDestination.Dashboard.route) {
            val viewModel = hiltViewModel<TripSummaryDashBoardViewModel>()
            TripSummaryDashBoard(viewModel, navigateToTripHistory = {
                navController.navigate(NavigationDestination.TripHistory.route)
            })
        }
    }
}