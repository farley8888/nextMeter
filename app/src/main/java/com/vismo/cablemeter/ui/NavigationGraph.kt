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
import com.vismo.cablemeter.ui.dashboard.mcu.MCUSummaryDashboard
import com.vismo.cablemeter.ui.dashboard.mcu.MCUSummaryDashboardViewModel
import com.vismo.cablemeter.ui.dashboard.trip.TripSummaryDashboard
import com.vismo.cablemeter.ui.dashboard.trip.TripSummaryDashboardViewModel
import com.vismo.cablemeter.ui.history.LocalTripHistoryScreen
import com.vismo.cablemeter.ui.history.LocalTripHistoryViewModel
import com.vismo.cablemeter.ui.meter.MeterOpsScreen
import com.vismo.cablemeter.ui.meter.MeterOpsViewModel
import com.vismo.cablemeter.ui.pair.DriverPairScreen
import com.vismo.cablemeter.ui.pair.DriverPairViewModel
import com.vismo.cablemeter.ui.pin.SystemPinScreen

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
                navController.navigate(NavigationDestination.TripSummaryDashboard.route)
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
        composable(NavigationDestination.TripSummaryDashboard.route) {
            val viewModel = hiltViewModel<TripSummaryDashboardViewModel>()
            TripSummaryDashboard(viewModel, navigateToTripHistory = {
                navController.navigate(NavigationDestination.TripHistory.route)
            }, navigateToMCUSummary = {
                navController.navigate(NavigationDestination.MCUSummaryDashboard.route)
            })
        }
        composable(NavigationDestination.MCUSummaryDashboard.route) {
            val viewModel = hiltViewModel<MCUSummaryDashboardViewModel>()
            MCUSummaryDashboard(viewModel, navigate = {
                navController.navigate(NavigationDestination.SystemPin.route)
            })
        }
        composable(NavigationDestination.SystemPin.route) {
            SystemPinScreen()
        }
    }
}