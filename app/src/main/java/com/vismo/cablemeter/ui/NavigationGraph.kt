package com.vismo.cablemeter.ui


import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.vismo.cablemeter.MainActivity.Companion.NavigationDestination
import com.vismo.cablemeter.ui.settings.admin.EditAdminPropertiesViewModel
import com.vismo.cablemeter.ui.settings.admin.advance.EditFareCalculationPropertiesScreen
import com.vismo.cablemeter.ui.settings.admin.basic.EditKValueAndLicensePlateScreen
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
import com.vismo.cablemeter.ui.settings.AdjustBrightnessOrVolumeScreen
import com.vismo.cablemeter.ui.settings.AdjustBrightnessOrVolumeViewModel
import com.vismo.cablemeter.ui.shared.GlobalSnackbarDelegate
import com.vismo.cablemeter.ui.splash.SplashScreen
import com.vismo.cablemeter.ui.splash.SplashScreenViewModel

@Composable
fun NavigationGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    snackbarDelegate: GlobalSnackbarDelegate,
) {
    // Shared ViewModels
    val editAdminPropertiesViewModel = hiltViewModel<EditAdminPropertiesViewModel>()

    NavHost(
        navController = navController,
        startDestination = NavigationDestination.Splash.route,
        modifier = Modifier.padding(innerPadding),
        enterTransition = {
            fadeIn(tween(700))
        },
        exitTransition = {
            fadeOut(tween(700))
        }
    ) {
        composable(NavigationDestination.Splash.route) {
            val viewModel = hiltViewModel<SplashScreenViewModel>()
            SplashScreen(viewModel, navigateToPair = {
                navController.navigate(NavigationDestination.Pair.route) {
                    popUpTo(NavigationDestination.Splash.route) { inclusive = true }
                    restoreState = true
                    launchSingleTop = true
                }
            }, navigateToMeterOps = {
                navController.navigate(NavigationDestination.MeterOps.route) {
                    popUpTo(NavigationDestination.Splash.route) { inclusive = true }
                    restoreState = true
                    launchSingleTop = true
                }
            },)
        }
        composable(NavigationDestination.MeterOps.route) {
            val viewModel = hiltViewModel<MeterOpsViewModel>()
            MeterOpsScreen(viewModel,
                navigateToDashBoard = {
                navController.navigate(NavigationDestination.TripSummaryDashboard.route)
            })
        }
        composable(NavigationDestination.Pair.route) {
            val viewModel = hiltViewModel<DriverPairViewModel>()
            DriverPairScreen(viewModel, snackbarDelegate,
                navigateToMeterOps = {
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
            }, navigateToAdjustBrightnessOrVolume = {
               navController.navigate(NavigationDestination.AdjustBrightnessOrVolume.route)
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
            SystemPinScreen(navigate = {
                navController.navigate(NavigationDestination.AdminBasicEdit.route)
            })
        }
        composable(NavigationDestination.AdminBasicEdit.route) {
            EditKValueAndLicensePlateScreen(
                viewModel = editAdminPropertiesViewModel,
                snackbarDelegate = snackbarDelegate,
                navigateToAdminAdvancedEdit = {
                navController.navigate(NavigationDestination.AdminAdvancedEdit.route)
            })
        }
        composable(NavigationDestination.AdminAdvancedEdit.route) {
            EditFareCalculationPropertiesScreen(
                viewModel = editAdminPropertiesViewModel,
                snackbarDelegate = snackbarDelegate
            )
        }
        composable(NavigationDestination.AdjustBrightnessOrVolume.route) {
            AdjustBrightnessOrVolumeScreen(viewModel = hiltViewModel<AdjustBrightnessOrVolumeViewModel>())
        }
    }
}